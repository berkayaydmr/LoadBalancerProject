import java.io.*;
import java.net.*;
import java.util.*;

public class LoadBalancer {
    private static List<Socket> servers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("Load Balancer started on port 9000");

        while (true) {
            Socket socket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String message = in.readLine();

            if (message != null && message.equals("REGISTER")) {
                servers.add(socket);
                System.out.println("Registered server: " + socket);
            }
             else if (message != null && message.equals("CLIENT")) {
                if (!servers.isEmpty()) {
                    Socket target = servers.get(0); 
                    out.write("SERVER_PORT " + target.getPort() + "\n");
                    out.flush();
                    System.out.println("Forwarded client to server on port: " + target.getPort());
                } else {
                    out.write("NO_SERVER_AVAILABLE\n");
                    out.flush();
                }
            }
        }
    }
}