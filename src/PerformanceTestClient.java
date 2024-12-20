import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class PerformanceTestClient {
    private static final int PORT = 5000;
    private static final String HOST = "localhost";
    private static final int REQUEST_INTERVAL_MS = 2000; // 2 seconds

    public static void main(String[] args) throws InterruptedException {
        int maxClients = 100;
        int transactionsPerClient = 10;

        System.out.println("Clients\tAverage Time (ms)");
        for (int clientCount = 1; clientCount <= maxClients; clientCount++) {
            ExecutorService executor = Executors.newFixedThreadPool(clientCount);
            AtomicLong totalTransactionTime = new AtomicLong(0);

            CountDownLatch latch = new CountDownLatch(clientCount);
            for (int i = 0; i < clientCount; i++) {
                executor.submit(() -> {
                    try (Socket socket = new Socket(HOST, PORT);
                         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                        for (int j = 0; j < transactionsPerClient; j++) {
                            long startTime = System.currentTimeMillis();

                            out.println("101 d 100");
                            in.readLine();

                            long endTime = System.currentTimeMillis();
                            totalTransactionTime.addAndGet(endTime - startTime);


                            Thread.sleep(REQUEST_INTERVAL_MS);
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            long averageTime = totalTransactionTime.get() / (clientCount * transactionsPerClient);
            System.out.println(clientCount + "\t" + averageTime);
        }
    }
}
