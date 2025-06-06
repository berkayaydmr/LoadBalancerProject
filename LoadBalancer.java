import java.io.*;
import java.net.*;
import java.util.*;

public class LoadBalancer {
    private static List<Socket> servers = new ArrayList<>();
    private static Map<Socket, Integer> serverLoads = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("Load Balancer started on port 9000");

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

            if (message != null && message.equals("REGISTER")) {
                synchronized (servers) {
                    servers.add(socket);
                    serverLoads.put(socket, 0);
                    System.out.println("Registered server: " + socket);
                }

            } else if (message != null && message.equals("CLIENT")) {
                Socket selected = selectLeastLoadedServer();
                if (selected != null) {
                    out.write("SERVER_PORT 7000\n"); // sabit portlu sunucular için
                } else {
                    out.write("NO_SERVER_AVAILABLE\n");
                }
                out.flush();

            } else if (message != null && message.toLowerCase().startsWith("load=")) {
                try {
                    int load = Integer.parseInt(message.substring(5).trim());
                    serverLoads.put(socket, load);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid load value from: " + socket);
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling connection: " + e.getMessage());
        }
    }

    private static Socket selectLeastLoadedServer() {
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
}