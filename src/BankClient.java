import java.io.*;
import java.net.*;
import java.util.*;

public class BankClient {
    private static final String LOG_FILE = "client_log.txt";

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true);
             BufferedWriter logWriter = new BufferedWriter(new FileWriter(LOG_FILE, true))) {

            String transactionFile = "transactions.txt";
            List<Transaction> transactions = loadTransactions(transactionFile);

            transactions.sort(Comparator.comparingInt(Transaction::getTimestamp));

            long startTime = System.currentTimeMillis();
            for (Transaction transaction : transactions) {
                // Wait for the timestamp
                long delay = transaction.getTimestamp() * 1000L - (System.currentTimeMillis() - startTime);
                if (delay > 0) {
                    Thread.sleep(delay);
                }

                String request = transaction.toRequestString();
                serverOutput.println(request);
                log(logWriter, "Sent to server: " + request);

                String response = serverInput.readLine();
                log(logWriter, "Server response: " + response);
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void log(BufferedWriter logWriter, String message) throws IOException {
        logWriter.write(message);
        logWriter.newLine();
        logWriter.flush();
    }

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
