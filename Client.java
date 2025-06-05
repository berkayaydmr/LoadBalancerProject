import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket lbSocket = new Socket("localhost", 9000);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(lbSocket.getOutputStream()));
        out.write("CLIENT\n");
        out.flush();
    }
}
