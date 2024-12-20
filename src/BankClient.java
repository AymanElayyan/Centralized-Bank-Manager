import java.io.*;
import java.net.*;
import java.util.*;

public class BankClient {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true)) {

            // Load the transaction file
            String transactionFile = "transactions.txt"; // Replace with the path to your file
            List<Transaction> transactions = loadTransactions(transactionFile);

            // Sort transactions by timestamp
            transactions.sort(Comparator.comparingInt(Transaction::getTimestamp));

            // Execute transactions at specified intervals
            long startTime = System.currentTimeMillis();
            for (Transaction transaction : transactions) {
                // Wait for the timestamp
                long delay = transaction.getTimestamp() * 1000L - (System.currentTimeMillis() - startTime);
                if (delay > 0) {
                    Thread.sleep(delay);
                }

                // Send the transaction to the server
                String request = transaction.toRequestString();
                serverOutput.println(request);
                System.out.println("Sent to server: " + request);

                // Receive and log the server response
                String response = serverInput.readLine();
                System.out.println("Server response: " + response);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Method to load transactions from a file
    private static List<Transaction> loadTransactions(String fileName) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                transactions.add(Transaction.parse(line));
            }
        }
        return transactions;
    }

    // Inner class to represent a transaction
    static class Transaction {
        private final int timestamp;
        private final int accountNumber;
        private final String type;
        private final double amount;

        public Transaction(int timestamp, int accountNumber, String type, double amount) {
            this.timestamp = timestamp;
            this.accountNumber = accountNumber;
            this.type = type;
            this.amount = amount;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public static Transaction parse(String line) {
            String[] parts = line.split(" ");
            int timestamp = Integer.parseInt(parts[0]);
            int accountNumber = Integer.parseInt(parts[1]);
            String type = parts[2];
            double amount = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.0;
            return new Transaction(timestamp, accountNumber, type, amount);
        }

        public String toRequestString() {
            if (type.equals("q")) {
                return accountNumber + " " + type;
            } else {
                return accountNumber + " " + type + " " + amount;
            }
        }
    }
}
