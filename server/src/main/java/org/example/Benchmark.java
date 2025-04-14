package org.example;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.lang.instrument.Instrumentation;

public class Benchmark {

    private static Instrumentation instrumentation;

    // This method should be used by the JVM agent to initialize Instrumentation
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
    }

    public static void main(String[] args) {
        final String host = "localhost";
        final int port = 8080;
        final int ops = 10000;

        // Get runtime memory before operations
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // Use a cachebase first (optional)
            out.write("USE benchmarkdb\n");
            out.flush();
            in.readLine(); // discard response

            // Benchmark SET
            long setStart = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                String key = "key" + i;
                String value = UUID.randomUUID().toString();
                out.write("SET " + key + " " + value + "\n");
                out.flush();
                in.readLine(); // read "OK"
            }
            long setEnd = System.nanoTime();
            double setDurationMs = (setEnd - setStart) / 1_000_000.0;

            // Benchmark GET
            long getStart = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                String key = "key" + i;
                out.write("GET " + key + "\n");
                out.flush();
                in.readLine(); // read value
            }
            long getEnd = System.nanoTime();
            double getDurationMs = (getEnd - getStart) / 1_000_000.0;

            // Memory usage after operations
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long totalUsedMemory = memoryAfter - memoryBefore;

            // Estimate raw data memory (with better measurement)
            long rawDataMemory = 0;
            for (int i = 0; i < ops; i++) {
                String key = "key" + i;
                String value = UUID.randomUUID().toString();
                rawDataMemory += getObjectSize(key) + getObjectSize(value);
            }

            // Calculate overhead
            long overhead = totalUsedMemory - rawDataMemory;

            System.out.printf("SET  %d keys in %.2f ms (%.2f ms/op)\n", ops, setDurationMs, setDurationMs / ops);
            System.out.printf("GET  %d keys in %.2f ms (%.2f ms/op)\n", ops, getDurationMs, getDurationMs / ops);

            // Print memory usage details
            System.out.printf("Memory Usage (approx):\n");
            System.out.printf("Raw Data:     %.2f KB\n", rawDataMemory / 1024.0);
            System.out.printf("Total Used:   %.2f KB\n", totalUsedMemory / 1024.0);
            System.out.printf("Overhead:     %.2f KB\n", overhead / 1024.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to get the size of an object using instrumentation
    private static long getObjectSize(Object obj) {
        return instrumentation.getObjectSize(obj);
    }
}
