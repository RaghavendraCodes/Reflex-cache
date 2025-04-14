package org.example;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class BenchmarkParallel {

    private static final int TOTAL_OPERATIONS = 100;
    private static final int THREADS = 5;  // 5 threads for exclusive data storage
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    // Reentrant lock for synchronizing access to shared data
    private static final ReentrantLock lock = new ReentrantLock();

    // Shared data store (instead of thread-local)
    private static final Map<String, String> sharedDataStore = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        System.out.println("Starting API + SET benchmark...");
        long setStart = System.nanoTime();

        CountDownLatch setLatch = new CountDownLatch(TOTAL_OPERATIONS);
        for (int i = 0; i < TOTAL_OPERATIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Simulate external API call (replace with real endpoint if necessary)
                    String apiResponse = fetchDummyData(index);

                    // Acquire lock to safely update shared data
                    lock.lock();
                    try {
                        // Shared data store accessed and updated
                        sharedDataStore.put("api_key" + index, apiResponse);
                    } finally {
                        lock.unlock();
                    }

                    // Optionally, send the data to the server
                    try (Socket socket = new Socket(HOST, PORT);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                        writer.write("SET api_key" + index + " " + apiResponse + "\n");
                        writer.flush();
                        reader.readLine(); // consume "OK"
                    }
                } catch (IOException ignored) {}
                setLatch.countDown();
            });
        }
        setLatch.await();
        long setEnd = System.nanoTime();
        double setTime = (setEnd - setStart) / 1_000_000.0;
        double setThroughput = TOTAL_OPERATIONS / (setTime / 1000.0);  // ops per second

        System.out.printf("SET %d keys in %.2f ms (%.2f ms/op)\n", TOTAL_OPERATIONS, setTime, setTime / TOTAL_OPERATIONS);
        System.out.printf("SET Throughput: %.2f ops/sec\n", setThroughput);

        // Now we perform a GET operation and try to ensure the data is current
        System.out.println("Starting GET + UPDATE benchmark...");
        long getStart = System.nanoTime();

        CountDownLatch getLatch = new CountDownLatch(TOTAL_OPERATIONS);
        for (int i = 0; i < TOTAL_OPERATIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // First, simulate GET operation from the server or local data store
                    String key = "api_key" + index;

                    // Acquire lock to safely update shared data
                    lock.lock();
                    try {
                        String value = sharedDataStore.get(key);
                        if (value != null) {
                            // Perform some operation on the value, e.g., updating it
                            String updatedValue = value + "_updated";
                            sharedDataStore.put(key, updatedValue); // update the key in the shared store
                        }
                    } finally {
                        lock.unlock();
                    }

                    // Send GET to server
                    try (Socket socket = new Socket(HOST, PORT);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                        writer.write("GET " + key + "\n");
                        writer.flush();
                        reader.readLine(); // consume value (ignore it for now)
                    }
                } catch (IOException ignored) {}
                getLatch.countDown();
            });
        }
        getLatch.await();
        long getEnd = System.nanoTime();
        double getTime = (getEnd - getStart) / 1_000_000.0;
        double getThroughput = TOTAL_OPERATIONS / (getTime / 1000.0);  // ops per second

        System.out.printf("GET and Update %d keys in %.2f ms (%.2f ms/op)\n", TOTAL_OPERATIONS, getTime, getTime / TOTAL_OPERATIONS);
        System.out.printf("GET and Update Throughput: %.2f ops/sec\n", getThroughput);

        executor.shutdown();
    }

    private static String fetchDummyData(int index) {
        // Simulate an API call to fetch dummy data
        return "{\"data\":\"some_dummy_data_" + index + "\"}";
    }
}
