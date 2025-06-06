import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7001;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server running on port " + port);

        // Connect to load balancer with retry logic
        Socket lb = null;
        BufferedWriter lbOut = null;
        
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                lb = new Socket("localhost", 9001);
                lbOut = new BufferedWriter(new OutputStreamWriter(lb.getOutputStream()));
                lbOut.write("REGISTER " + port + "\n");
                lbOut.flush();
                System.out.println("Successfully registered with load balancer");
                break;
            } catch (IOException e) {
                System.out.println("Failed to connect to load balancer, attempt " + (attempt + 1));
                if (attempt < 4) {
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                } else {
                    System.out.println("Could not connect to load balancer after 5 attempts");
                    System.exit(1);
                }
            }
        }

        final BufferedWriter finalLbOut = lbOut;
        final Socket finalLb = lb;

        // Load balancer'a yük bilgisi gönder
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000);
                    int fakeLoad = (int)(Math.random() * 100);
                    finalLbOut.write("load=" + fakeLoad + "\n");
                    finalLbOut.flush();
                }
            } catch (Exception e) {
                System.out.println("Lost connection to load balancer: " + e.getMessage());
                try { finalLb.close(); } catch (IOException ie) {}
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