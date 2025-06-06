import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static AtomicInteger currentLoad = new AtomicInteger(0);
    private static String[] fileList = {"document1.pdf", "image1.jpg", "video1.mp4", "data.csv", "presentation.pptx"};
    
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7001;
        String balancingMethod = "dynamic"; // default
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v") && i + 1 < args.length) {
                balancingMethod = args[i + 1];
            }
        }
        
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server running on port " + port + " with " + balancingMethod + " balancing");

        // Connect to load balancer with retry logic
        Socket lb = null;
        BufferedWriter lbOut = null;
        
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                lb = new Socket("localhost", 9001);
                lbOut = new BufferedWriter(new OutputStreamWriter(lb.getOutputStream()));
                lbOut.write("join -v " + balancingMethod + " port=" + port + "\n");
                lbOut.flush();
                
                BufferedReader lbIn = new BufferedReader(new InputStreamReader(lb.getInputStream()));
                String response = lbIn.readLine();
                if ("join_accepted".equals(response)) {
                    System.out.println("Successfully registered with load balancer");
                    break;
                } else {
                    System.out.println("Load balancer rejected registration: " + response);
                }
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

        // Add shutdown hook for graceful exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                finalLbOut.write("goodbye\n");
                finalLbOut.flush();
                finalLb.close();
                serverSocket.close();
                System.out.println("Server shutdown gracefully");
            } catch (IOException e) {
                System.err.println("Error during shutdown: " + e.getMessage());
            }
        }));

        // Load balancer'a yük bilgisi gönder
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(5000);
                    int load = currentLoad.get();
                    finalLbOut.write("load=" + load + "\n");
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
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }
    
    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            
            String request = in.readLine();
            if (request != null) {
                String[] parts = request.split(" ");
                String requestType = parts[0];
                
                currentLoad.incrementAndGet();
                System.out.println("Handling request: " + requestType + " (Load: " + currentLoad.get() + ")");
                
                switch (requestType) {
                    case "DIRECTORY":
                        handleDirectoryRequest(out);
                        break;
                    case "FILE_TRANSFER":
                        String filename = parts.length > 1 ? parts[1] : "default.txt";
                        handleFileTransfer(out, filename);
                        break;
                    case "COMPUTATION":
                        int duration = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                        handleComputationRequest(out, duration);
                        break;
                    case "VIDEO_STREAMING":
                        int streamDuration = parts.length > 1 ? Integer.parseInt(parts[1]) : 30;
                        handleVideoStreaming(out, streamDuration);
                        break;
                    default:
                        handleDirectoryRequest(out); // Default to directory listing
                }
                
                currentLoad.decrementAndGet();
                System.out.println("Completed request: " + requestType + " (Load: " + currentLoad.get() + ")");
            }
            
            clientSocket.close();
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error handling client: " + e.getMessage());
            currentLoad.decrementAndGet();
        }
    }
    
    private static void handleDirectoryRequest(BufferedWriter out) throws IOException {
        StringBuilder response = new StringBuilder("DIRECTORY_LISTING\n");
        for (String file : fileList) {
            response.append(file).append("\n");
        }
        response.append("END\n");
        out.write(response.toString());
        out.flush();
        
        // Simulate fast response
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
    
    private static void handleFileTransfer(BufferedWriter out, String filename) throws IOException {
        out.write("FILE_TRANSFER_START " + filename + "\n");
        out.flush();
        
        // Simulate file transfer (variable time based on file size)
        int transferTime = filename.toLowerCase().contains("video") ? 8000 : 
                          filename.toLowerCase().contains("image") ? 3000 : 1000;
        
        try {
            Thread.sleep(transferTime);
            out.write("FILE_CONTENT: [Simulated content of " + filename + "]\n");
            out.write("FILE_TRANSFER_COMPLETE\n");
            out.flush();
        } catch (InterruptedException e) {
            out.write("FILE_TRANSFER_INTERRUPTED\n");
            out.flush();
        }
    }
    
    private static void handleComputationRequest(BufferedWriter out, int duration) throws IOException {
        out.write("COMPUTATION_START duration=" + duration + "s\n");
        out.flush();
        
        // Simulate computation work
        try {
            for (int i = 0; i < duration; i++) {
                Thread.sleep(1000);
                out.write("COMPUTATION_PROGRESS " + ((i + 1) * 100 / duration) + "%\n");
                out.flush();
            }
            out.write("COMPUTATION_COMPLETE result=42\n");
            out.flush();
        } catch (InterruptedException e) {
            out.write("COMPUTATION_INTERRUPTED\n");
            out.flush();
        }
    }
    
    private static void handleVideoStreaming(BufferedWriter out, int duration) throws IOException {
        out.write("VIDEO_STREAMING_START duration=" + duration + "s\n");
        out.flush();
        
        // Simulate video streaming with constant bit rate
        try {
            for (int i = 0; i < duration; i++) {
                Thread.sleep(1000);
                out.write("VIDEO_FRAME " + (i + 1) + "/" + duration + " [Frame data]\n");
                out.flush();
            }
            out.write("VIDEO_STREAMING_COMPLETE\n");
            out.flush();
        } catch (InterruptedException e) {
            out.write("VIDEO_STREAMING_INTERRUPTED\n");
                        out.flush();
        }
    }
}