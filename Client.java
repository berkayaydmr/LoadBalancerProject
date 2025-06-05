import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket lbSocket = new Socket("localhost", 9000);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(lbSocket.getOutputStream()));
        out.write("CLIENT\n");
        out.flush();

        BufferedReader in = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
        String response = in.readLine();
        if (response != null && response.startsWith("SERVER_PORT")) {
            int port = Integer.parseInt(response.split(" ")[1]);
            Socket serverSocket = new Socket("localhost", port);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            System.out.println("Server says: " + serverIn.readLine());
        }
    }
}
