package org.example;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class BenchmarkParallel {

    private static final int TOTAL_OPERATIONS = 10000;
    private static final int THREADS = 10;
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        System.out.println("Starting parallel SET benchmark...");
        long setStart = System.nanoTime();

        CountDownLatch setLatch = new CountDownLatch(TOTAL_OPERATIONS);
        for (int i = 0; i < TOTAL_OPERATIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try (Socket socket = new Socket(HOST, PORT);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                    writer.write("SET key" + index + " value" + index + "\n");
                    writer.flush();
                    reader.readLine(); // read OK
                } catch (IOException ignored) {
                }
                setLatch.countDown();
            });
        }
        setLatch.await();
        long setEnd = System.nanoTime();
        double setTime = (setEnd - setStart) / 1_000_000.0;
        System.out.printf("SET %d keys in %.2f ms (%.2f ms/op)\n", TOTAL_OPERATIONS, setTime, setTime / TOTAL_OPERATIONS);

        System.out.println("Starting parallel GET benchmark...");
        long getStart = System.nanoTime();

        CountDownLatch getLatch = new CountDownLatch(TOTAL_OPERATIONS);
        for (int i = 0; i < TOTAL_OPERATIONS; i++) {
            final int index = i;
            executor.submit(() -> {
                try (Socket socket = new Socket(HOST, PORT);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                    writer.write("GET key" + index + "\n");
                    writer.flush();
                    reader.readLine(); // read value
                } catch (IOException ignored) {
                }
                getLatch.countDown();
            });
        }
        getLatch.await();
        long getEnd = System.nanoTime();
        double getTime = (getEnd - getStart) / 1_000_000.0;
        System.out.printf("GET %d keys in %.2f ms (%.2f ms/op)\n", TOTAL_OPERATIONS, getTime, getTime / TOTAL_OPERATIONS);

        executor.shutdown();
    }
}
