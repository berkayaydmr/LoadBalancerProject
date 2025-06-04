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
            String message = in.readLine();

            if (message != null && message.equals("REGISTER")) {
                servers.add(socket);
                System.out.println("Registered server: " + socket);
            }
        }
    }
}