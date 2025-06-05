import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(7000);
        System.out.println("Server running on port 7000");

        Socket lb = new Socket("localhost", 9000);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(lb.getOutputStream()));
        out.write("REGISTER\n");
        out.flush();

        Socket clientSocket = ss.accept();
        BufferedWriter outClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        outClient.write("Directory: file1.txt, file2.txt\n");
        outClient.flush();
    }
}
