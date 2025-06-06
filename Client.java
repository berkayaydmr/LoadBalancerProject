import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket lbSocket = new Socket("localhost", 9000);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(lbSocket.getOutputStream()));
        BufferedReader in = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));

        out.write("CLIENT\n");
        out.flush();

        String response = in.readLine();
        if (response != null && response.startsWith("SERVER_PORT")) {
            int port = Integer.parseInt(response.split(" ")[1]);
            Socket serverSocket = new Socket("localhost", port);

            BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            String serverResponse = serverIn.readLine();
            System.out.println("Server responded: " + serverResponse);
        } else {
            System.out.println("No server available: " + response);
        }
    }
}