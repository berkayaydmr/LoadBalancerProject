import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoadBalancer {
    private static List<Socket> servers = Collections.synchronizedList(new ArrayList<>());
    private static Map<Socket, Integer> serverLoads = new ConcurrentHashMap<>();
    private static Map<Socket, Integer> serverPorts = new ConcurrentHashMap<>();
    private static Map<Socket, Long> serverLastHeartbeat = new ConcurrentHashMap<>();
    private static Map<Socket, String> serverBalancingMethod = new ConcurrentHashMap<>();
    
    // Request type constants
    private static final String DIRECTORY = "DIRECTORY";
    private static final String FILE_TRANSFER = "FILE_TRANSFER";
    private static final String COMPUTATION = "COMPUTATION";
    private static final String VIDEO_STREAMING = "VIDEO_STREAMING";
    
    // Estimated processing times (in seconds)
    private static final Map<String, Integer> REQUEST_ESTIMATES = new HashMap<>();
    static {
        REQUEST_ESTIMATES.put(DIRECTORY, 1);
        REQUEST_ESTIMATES.put(FILE_TRANSFER, 5);
        REQUEST_ESTIMATES.put(COMPUTATION, 10);
        REQUEST_ESTIMATES.put(VIDEO_STREAMING, 30);
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9001;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Load Balancer started on port " + port);

        // Start health monitoring thread
        new Thread(() -> monitorServerHealth()).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }));

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

            if (message != null && message.startsWith("join")) {
                // Handle server registration with balancing method
                String[] parts = message.split(" ");
                String balancingMethod = "dynamic"; // default
                int port = 7000; // default
                
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].equals("-v") && i + 1 < parts.length) {
                        balancingMethod = parts[i + 1];
                    } else if (parts[i].startsWith("port=")) {
                        port = Integer.parseInt(parts[i].substring(5));
                    }
                }

                synchronized (servers) {
                    servers.add(socket);
                    serverLoads.put(socket, 0);
                    serverLastHeartbeat.put(socket, System.currentTimeMillis());
                    serverPorts.put(socket, port);
                    serverBalancingMethod.put(socket, balancingMethod);
                    System.out.println("Server joined on port " + port + " with " + balancingMethod + " balancing");
                }

                out.write("join_accepted\n");
                out.flush();

                // Keep reading updates from this server
                while (true) {
                    String updateMessage = in.readLine();
                    if (updateMessage == null) break;

                    if (updateMessage.toLowerCase().startsWith("load=")) {
                        try {
                            int load = Integer.parseInt(updateMessage.substring(5).trim());
                            serverLoads.put(socket, load);
                            serverLastHeartbeat.put(socket, System.currentTimeMillis());
                            System.out.println("Updated load for port " + serverPorts.get(socket) + ": " + load);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid load value from: " + socket);
                        }
                    } else if (updateMessage.equals("goodbye")) {
                        System.out.println("Server on port " + serverPorts.get(socket) + " said goodbye");
                        break;
                    }
                }

            } else if (message != null && message.startsWith("REQUEST")) {
                // Handle client request: REQUEST <type> [parameters]
                String[] parts = message.split(" ");
                String requestType = parts.length > 1 ? parts[1] : DIRECTORY;
                String parameters = parts.length > 2 ? String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)) : "";
                
                int estimatedTime = estimateRequestTime(requestType, parameters);
                Socket selected = selectServer(requestType, estimatedTime);
                
                if (selected != null) {
                    int port = serverPorts.getOrDefault(selected, 7000);
                    out.write("SERVER_PORT " + port + "\n");
                    System.out.println("Assigned " + requestType + " request to server on port: " + port + 
                                     " (estimated time: " + estimatedTime + "s)");
                } else {
                    out.write("NO_SERVER_AVAILABLE\n");
                    System.out.println("No servers available for " + requestType + " request");
                }
                out.flush();
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("Connection closed: " + e.getMessage());
        } finally {
            cleanupServer(socket);
        }
    }

    // Estimate request processing time based on type and parameters
    private static int estimateRequestTime(String requestType, String parameters) {
        int baseTime = REQUEST_ESTIMATES.getOrDefault(requestType, 5);
        
        if (requestType.equals(COMPUTATION) && !parameters.isEmpty()) {
            try {
                // Extract duration from computation request
                String[] parts = parameters.split(" ");
                for (String part : parts) {
                    if (part.endsWith("s") || part.endsWith("sec")) {
                        return Integer.parseInt(part.replaceAll("[^0-9]", ""));
                    }
                }
            } catch (NumberFormatException e) {
                // Use default if parsing fails
            }
        } else if (requestType.equals(VIDEO_STREAMING) && !parameters.isEmpty()) {
            try {
                // Extract duration from streaming request
                String[] parts = parameters.split(" ");
                for (String part : parts) {
                    if (part.endsWith("s") || part.endsWith("sec")) {
                        return Integer.parseInt(part.replaceAll("[^0-9]", ""));
                    }
                }
            } catch (NumberFormatException e) {
                // Use default if parsing fails
            }
        }
        
        return baseTime;
    }
    
    // Select best server based on request type and load balancing method
    private static Socket selectServer(String requestType, int estimatedTime) {
        synchronized (servers) {
            if (servers.isEmpty()) return null;
            
            // Try both static and dynamic methods, prefer dynamic for better performance
            Socket dynamicChoice = selectServerByLeastLoad();
            Socket staticChoice = selectServerByRoundRobin();
            
            // For computation and streaming requests, prefer least loaded server
            if (requestType.equals(COMPUTATION) || requestType.equals(VIDEO_STREAMING)) {
                return dynamicChoice;
            }
            
            // For quick requests, round-robin is fine
            return staticChoice != null ? staticChoice : dynamicChoice;
        }
    }

    // Least Load Algorithm (Dynamic)
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

    // Round Robin Algorithm (Static)
    private static int roundRobinIndex = 0;
    private static Socket selectServerByRoundRobin() {
        synchronized (servers) {
            if (servers.isEmpty()) return null;
            
            Socket selected = servers.get(roundRobinIndex % servers.size());
            roundRobinIndex = (roundRobinIndex + 1) % servers.size();
            return selected;
        }
    }

    // Weighted Load Algorithm (considers both load and response time)
    private static Socket selectServerByWeightedLoad() {
        Socket best = null;
        double bestScore = Double.MAX_VALUE;

        synchronized (servers) {
            for (Socket s : servers) {
                int load = serverLoads.getOrDefault(s, 0);
                String method = serverBalancingMethod.getOrDefault(s, "dynamic");
                
                // Weight calculation: lower is better
                double score = load * 1.0;
                if (method.equals("static")) {
                    score *= 1.2; // Slightly penalize static servers for dynamic requests
                }
                
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
            serverBalancingMethod.remove(socket);
        }
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
}