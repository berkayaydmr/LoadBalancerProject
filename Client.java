import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Load Balancer Client ===");
        System.out.println("Available request types:");
        System.out.println("1. DIRECTORY - List server files");
        System.out.println("2. FILE_TRANSFER <filename> - Transfer a file");
        System.out.println("3. COMPUTATION <duration_seconds> - CPU intensive task");
        System.out.println("4. VIDEO_STREAMING <duration_seconds> - Stream video");
        System.out.print("Enter request type and parameters: ");
        
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            input = "DIRECTORY"; // Default request
        }
        
        // Connect to load balancer
        Socket lbSocket = new Socket("localhost", 9001);
        BufferedWriter lbOut = new BufferedWriter(new OutputStreamWriter(lbSocket.getOutputStream()));
        BufferedReader lbIn = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));

        // Send request to load balancer
        lbOut.write("REQUEST " + input + "\n");
        lbOut.flush();

        String response = lbIn.readLine();
        lbSocket.close();
        
        if (response != null && response.startsWith("SERVER_PORT")) {
            int port = Integer.parseInt(response.split(" ")[1]);
            System.out.println("Load balancer assigned server on port: " + port);
            
            // Connect directly to assigned server
            Socket serverSocket = new Socket("localhost", port);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            BufferedWriter serverOut = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
            
            // Send request to server
            serverOut.write(input + "\n");
            serverOut.flush();
            
            // Read server response
            System.out.println("\n=== Server Response ===");
            String line;
            while ((line = serverIn.readLine()) != null) {
                System.out.println(line);
                
                // For streaming and computation, show progress
                if (line.contains("PROGRESS") || line.contains("FRAME")) {
                    System.out.flush();
                }
                
                // Check for completion
                if (line.contains("COMPLETE") || line.contains("END")) {
                    break;
                }
            }
            
            serverSocket.close();
            System.out.println("\n=== Request Completed ===");
            
        } else {
            System.out.println("No server available: " + response);
        }
        
        scanner.close();
    }
}