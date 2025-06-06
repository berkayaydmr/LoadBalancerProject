import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TestClient {
    private static final String[] REQUEST_TYPES = {
        "DIRECTORY",
        "FILE_TRANSFER document1.pdf",
        "COMPUTATION 5",
        "VIDEO_STREAMING 10"
    };
    
    public static void main(String[] args) throws IOException, InterruptedException {
        int numThreads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int numRequests = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        
        System.out.println("=== Multi-threaded Load Balancer Test ===");
        System.out.println("Threads: " + numThreads + ", Total requests: " + numRequests);
        System.out.println("Starting concurrent requests...\n");
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numRequests);
        
        long startTime = System.currentTimeMillis();
        
        // Submit multiple requests concurrently
        for (int i = 0; i < numRequests; i++) {
            final int requestId = i + 1;
            final String request = REQUEST_TYPES[i % REQUEST_TYPES.length];
            
            executor.submit(() -> {
                try {
                    performRequest(requestId, request);
                } catch (Exception e) {
                    System.err.println("Thread " + requestId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all requests to complete
        latch.await();
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();
        
        System.out.println("\n=== Test Results ===");
        System.out.println("All " + numRequests + " requests completed in " + 
                         (endTime - startTime) + "ms");
        System.out.println("Average time per request: " + 
                         (endTime - startTime) / numRequests + "ms");
    }
    
    private static void performRequest(int requestId, String fullRequest) throws IOException {
        long requestStart = System.currentTimeMillis();
        
        synchronized (System.out) {
            System.out.println("[Thread " + requestId + "] Starting: " + fullRequest);
        }
        
        // Connect to load balancer
        Socket lbSocket = new Socket("localhost", 9001);
        BufferedWriter lbOut = new BufferedWriter(new OutputStreamWriter(lbSocket.getOutputStream()));
        BufferedReader lbIn = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));

        // Send request to load balancer
        lbOut.write("REQUEST " + fullRequest + "\n");
        lbOut.flush();

        String response = lbIn.readLine();
        lbSocket.close();
        
        if (response != null && response.startsWith("SERVER_PORT")) {
            int port = Integer.parseInt(response.split(" ")[1]);
            
            synchronized (System.out) {
                System.out.println("[Thread " + requestId + "] Assigned to server port: " + port);
            }
            
            // Connect directly to assigned server
            Socket serverSocket = new Socket("localhost", port);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
            
            // Send request to server
            serverOut.write(fullRequest + "\n");
            serverOut.flush();
            
            // Read server response
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            int lineCount = 0;
            
            while ((line = serverIn.readLine()) != null && lineCount < 10) {
                responseBuilder.append(line).append("\n");
                lineCount++;
                
                if (line.contains("END") || line.contains("COMPLETE")) {
                    break;
                }
            }
            
            serverSocket.close();
            
            long requestEnd = System.currentTimeMillis();
            
            synchronized (System.out) {
                System.out.println("[Thread " + requestId + "] Completed in " + 
                                 (requestEnd - requestStart) + "ms");
                System.out.println("[Thread " + requestId + "] Response preview:");
                String[] lines = responseBuilder.toString().split("\n");
                for (int i = 0; i < Math.min(3, lines.length); i++) {
                    System.out.println("    " + lines[i]);
                }
                System.out.println();
            }
            
        } else {
            synchronized (System.out) {
                System.out.println("[Thread " + requestId + "] No server available: " + response);
            }
        }
    }
}
