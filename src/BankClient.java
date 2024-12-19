import java.io.*;
import java.net.*;

public class BankClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader fileReader = new BufferedReader(new FileReader("transactions.txt"))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                out.println(line);
                System.out.println("Server response: " + in.readLine());
                Thread.sleep(1000); // Simulate periodic requests
                System.out.println("Request 5000");

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
