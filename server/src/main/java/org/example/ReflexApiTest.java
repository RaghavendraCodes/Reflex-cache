package org.example;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ReflexApiTest {

    public static void runTest(String host, int port, String cachebaseName, int total) {
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Auth and ready
            waitForPrompt(in, out, "Enter your unique name:", "pipelineClient");
            waitForPrompt(in, out, "Do you want to open the file", "no");
            waitForLine(in, "You're now connected.");
            send(out, in, "CREATE CACHEBASE " + cachebaseName);

            // Generate user data
            System.out.println("🌐 Fetching API data in parallel for " + total + " users...");
            Map<String, String> keyValueMap = fetchApiData(total);

            // Send all SETs as pipelined commands
            System.out.println("🚀 Sending " + total + " SET commands (pipelined)...");
            long setStart = System.nanoTime();
            for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
                out.println("SET " + cachebaseName + " " + entry.getKey() + " " + entry.getValue());
            }
            out.flush();

            // Read exactly `total` responses
            for (int i = 0; i < total; i++) {
                String line = in.readLine();
                if (line != null && !line.equals("<END>")) {
                    // System.out.println("Server > " + line); // Uncomment if needed
                }
            }
            long setEnd = System.nanoTime();
            System.out.println("⏱️ Time taken for SETs: " + ((setEnd - setStart) / 1_000_000) + " ms");

            // Perform a few GETs normally
            System.out.println("🔍 Performing 20 random GETs...");
            long getStart = System.nanoTime();
            List<String> keys = new ArrayList<>(keyValueMap.keySet());
            for (int i = 0; i < 20; i++) {
                String key = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
                send(out, in, "GET " + cachebaseName + " " + key);
            }
            long getEnd = System.nanoTime();
            System.out.println("⏱️ Time taken for GETs: " + ((getEnd - getStart) / 1_000_000) + " ms");

            System.out.println("✅ Reflex pipelining test complete.");

        } catch (IOException e) {
            System.err.println("❌ ReflexPipelinedTest failed: " + e.getMessage());
        }
    }

    // Parallel API simulation (fake JSON data)
    private static Map<String, String> fetchApiData(int total) {
        ExecutorService executor = Executors.newFixedThreadPool(16);
        Map<String, Future<String>> futures = new HashMap<>();

        for (int i = 0; i < total; i++) {
            final int id = i;
            futures.put("user:" + id, executor.submit(() -> {
                return "{\"id\":" + id + ",\"name\":\"User" + id + "\",\"status\":\"" + randomStatus() + "\"}";
            }));
        }

        executor.shutdown();

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Future<String>> entry : futures.entrySet()) {
            try {
                result.put(entry.getKey(), entry.getValue().get());
            } catch (Exception e) {
                result.put(entry.getKey(), "{\"error\":\"fetch_failed\"}");
            }
        }
        return result;
    }

    private static String randomStatus() {
        String[] statuses = {"active", "inactive", "pending", "banned"};
        return statuses[ThreadLocalRandom.current().nextInt(statuses.length)];
    }

    private static void send(PrintWriter out, BufferedReader in, String command) throws IOException {
        out.println(command);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("<END>") || line.equals("Goodbye!")) break;
            System.out.println("Server > " + line);
        }
    }

    private static void waitForPrompt(BufferedReader in, PrintWriter out, String expected, String reply) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("Server: " + line);
            if (line.contains(expected)) {
                out.println(reply);
                break;
            }
        }
    }

    private static void waitForLine(BufferedReader in, String expected) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            System.out.println("Server: " + line);
            if (line.contains(expected)) break;
        }
    }
}
