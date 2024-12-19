import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

class Account {
    private final int accountNumber;
    private final String name;
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();

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

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}

public class BankServer {
    private static final Map<Integer, Account> accounts = new HashMap<>();

    public static void main(String[] args) throws IOException {

        loadAccounts("accounts.txt");

        // Start the server
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Bank Server started on port 5000");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket).start();
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
        }
    }

    private static class ClientHandler extends Thread {
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
                        out.println("Error: Account not found");
                        continue;
                    }

                    switch (action) {
                        case "d":
                            account.deposit(amount);
                            out.println("Deposit successful. New balance: " + account.getBalance());
                            break;
                        case "w":
                            if (account.withdraw(amount)) {
                                out.println("Withdrawal successful. New balance: " + account.getBalance());
                            } else {
                                out.println("Error: Insufficient funds");
                            }
                            break;
                        case "q":
                            out.println("Balance: " + account.getBalance());
                            break;
                        default:
                            out.println("Error: Invalid action");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
