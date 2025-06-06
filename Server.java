import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(7000); // sabit port
        System.out.println("Server running on port 7000");

        Socket lb = new Socket("localhost", 9000);
        BufferedWriter lbOut = new BufferedWriter(new OutputStreamWriter(lb.getOutputStream()));
        lbOut.write("REGISTER\n");
        lbOut.flush();

        // Load balancer'a yük bilgisi gönder
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000);
                    int fakeLoad = (int)(Math.random() * 100);
                    lbOut.write("load=" + fakeLoad + "\n");
                    lbOut.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Çoklu istemci desteği
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> {
                try {
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    out.write("Directory: file1.txt, file2.txt\n");
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}