import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// Account class
class Account {
    private final int accountNumber;
    private final String name;
    private double balance;
    private final ReentrantLock lock = new ReentrantLock(); //

    public Account(int accountNumber, String name, double balance) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.balance = balance;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean withdraw(double amount) {
        lock.lock();
        try {
            if (amount <= balance) {
                balance -= amount;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
    }
}

public class BankServer {
    private static final Map<Integer, Account> accounts = new ConcurrentHashMap<>();
    private static final int PORT = 5000;
    private static final String LOG_FILE = "server_log.txt";
    private static BufferedWriter logWriter;
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws IOException {
        // Initialize log writer
        logWriter = new BufferedWriter(new FileWriter(LOG_FILE, true));

        // Add a separator for each server run
        log("Tasneem");
        log("=============================================");
        log("Bank Server started on port " + PORT);


        // Load accounts
        loadAccounts("accounts.txt");

        // Start the server
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ExecutorService threadPool = Executors.newFixedThreadPool(10); // Fixed pool for scalability

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        } finally {
            logWriter.close();
        }
    }

    private static void loadAccounts(String fileName) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                int accountNumber = Integer.parseInt(parts[0]);
                String name = parts[1];
                double balance = Double.parseDouble(parts[2]);
                accounts.put(accountNumber, new Account(accountNumber, name, balance));
            }
        } catch (FileNotFoundException e) {
            log("Accounts file not found: " + fileName);
            throw e;
        }
    }

    private static void log(String message) {
        String timestamp = dateFormatter.format(new Date());
        try {
            logWriter.write("[" + timestamp + "]: " + message);
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(" ");
                    int accountNumber = Integer.parseInt(parts[0]);
                    String action = parts[1];
                    double amount = parts.length > 2 ? Double.parseDouble(parts[2]) : 0;

                    Account account = accounts.get(accountNumber);
                    if (account == null) {
                        String errorMsg = "Error: Account not found for request: " + request;
                        out.println("[" + dateFormatter.format(new Date()) + "]: " + errorMsg);
                        log(errorMsg);
                        continue;
                    }

                    String logMessage;
                    switch (action) {
                        case "d":
                            account.deposit(amount);
                            logMessage = "Deposit successful for account " + accountNumber + ". New balance: " + account.getBalance();
                            out.println("[" + dateFormatter.format(new Date()) + "]: " + logMessage);
                            log(logMessage);
                            break;
                        case "w":
                            if (account.withdraw(amount)) {
                                logMessage = "Withdrawal successful for account " + accountNumber + ". New balance: " + account.getBalance();
                                out.println("[" + dateFormatter.format(new Date()) + "]: " + logMessage);
                                log(logMessage);
                            } else {
                                logMessage = "Error: Insufficient funds for account " + accountNumber;
                                out.println("[" + dateFormatter.format(new Date()) + "]: " + logMessage);
                                log(logMessage);
                            }
                            break;
                        case "q":
                            logMessage = "Balance query for account " + accountNumber + ". Balance: " + account.getBalance();
                            out.println("[" + dateFormatter.format(new Date()) + "]: " + logMessage);
                            log(logMessage);
                            break;
                        default:
                            logMessage = "Error: Invalid action for request: " + request;
                            out.println("[" + dateFormatter.format(new Date()) + "]: " + logMessage);
                            log(logMessage);
                    }
                }
            } catch (IOException e) {
                log("Error handling client: " + e.getMessage());
            }
        }
    }
}
