import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoadBalancer {
    private static List<Socket> servers = Collections.synchronizedList(new ArrayList<>());
    private static Map<Socket, Integer> serverLoads = new ConcurrentHashMap<>();
    private static Map<Socket, Integer> serverPorts = new ConcurrentHashMap<>();
    private static Map<Socket, Long> serverLastHeartbeat = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9001);
        System.out.println("Load Balancer started on port 9001");

        // Start health monitoring thread
        new Thread(() -> monitorServerHealth()).start();

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleConnection(socket)).start();
        }
    }

    private static void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String message = in.readLine();

            if (message != null && message.startsWith("REGISTER")) {
                synchronized (servers) {
                    servers.add(socket);
                    serverLoads.put(socket, 0);
                    serverLastHeartbeat.put(socket, System.currentTimeMillis());

                    String[] parts = message.split(" ");
                    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 7000;
                    serverPorts.put(socket, port);
                    System.out.println("Registered server on port: " + port);
                }

                // Keep reading load updates from this server
                while (true) {
                    String loadMessage = in.readLine();
                    if (loadMessage == null) break;

                    if (loadMessage.toLowerCase().startsWith("load=")) {
                        try {
                            int load = Integer.parseInt(loadMessage.substring(5).trim());
                            serverLoads.put(socket, load);
                            serverLastHeartbeat.put(socket, System.currentTimeMillis());
                            System.out.println("Updated load for port " + serverPorts.get(socket) + ": " + load);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid load value from: " + socket);
                        }
                    }
                }

            } else if (message != null && message.equals("CLIENT")) {
                Socket selected = selectServerByLeastLoad(); // Default to least load
                if (selected != null) {
                    int port = serverPorts.getOrDefault(selected, 7000);
                    out.write("SERVER_PORT " + port + "\n");
                    System.out.println("Assigned client to server on port: " + port);
                } else {
                    out.write("NO_SERVER_AVAILABLE\n");
                    System.out.println("No servers available for client request");
                }
                out.flush();
                socket.close(); // Close client connection after response
            }

        } catch (IOException e) {
            System.out.println("Connection closed: " + e.getMessage());
        } finally {
            // Clean up disconnected server
            cleanupServer(socket);
        }
    }

    // Least Load Algorithm
    private static Socket selectServerByLeastLoad() {
        Socket best = null;
        int minLoad = Integer.MAX_VALUE;

        synchronized (servers) {
            for (Socket s : servers) {
                int load = serverLoads.getOrDefault(s, 0);
                if (load < minLoad) {
                    minLoad = load;
                    best = s;
                }
            }
        }
        return best;
    }

    // Weighted Load Algorithm (considers both load and response time)
    private static Socket selectServerByWeightedLoad() {
        Socket best = null;
        double bestScore = Double.MAX_VALUE;

        synchronized (servers) {
            for (Socket s : servers) {
                int load = serverLoads.getOrDefault(s, 0);
                // Simple weighted score: higher load = worse score
                double score = load * 1.0;
                
                if (score < bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }
        return best;
    }

    private static void monitorServerHealth() {
        while (true) {
            try {
                Thread.sleep(10000); // Check every 10 seconds
                long currentTime = System.currentTimeMillis();
                List<Socket> deadServers = new ArrayList<>();

                for (Socket server : servers) {
                    long lastHeartbeat = serverLastHeartbeat.getOrDefault(server, 0L);
                    if (currentTime - lastHeartbeat > 15000) { // 15 seconds timeout
                        deadServers.add(server);
                    }
                }

                for (Socket deadServer : deadServers) {
                    System.out.println("Removing dead server on port: " + serverPorts.get(deadServer));
                    cleanupServer(deadServer);
                }

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void cleanupServer(Socket socket) {
        synchronized (servers) {
            servers.remove(socket);
            serverLoads.remove(socket);
            serverPorts.remove(socket);
            serverLastHeartbeat.remove(socket);
        }
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}