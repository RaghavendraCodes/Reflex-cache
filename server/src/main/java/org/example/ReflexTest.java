package org.example;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ReflexTest {

    public static void runTest(String host, int port, String cachebaseName) {
        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Wait for the name prompt and send it
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                System.out.println("Server: " + line);
                if (line.contains("Enter your unique name:")) {
                    out.println("testClient");
                    break;
                }
            }

            // Skip recovery prompt
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                System.out.println("Server: " + line);
                if (line.contains("Do you want to open the file")) {
                    out.println("no");
                }
                if (line.contains("You're now connected.")) break;
            }

            send(out, in, "CREATE CACHEBASE " + cachebaseName);

            // Track time for SET
            System.out.println("🚀 Starting 50 SET commands...");
            long setStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                String key = "key" + i;
                String value = "value" + i + "_" + randomString(8);
                send(out, in, "SET " + cachebaseName + " " + key + " " + value);
            }
            long setEnd = System.nanoTime();
            long setDurationMs = (setEnd - setStart) / 1_000_000;
            System.out.println("⏱️ Time taken for 50 SETs: " + setDurationMs + " ms");

            // Track time for GET
            System.out.println("🔍 Performing 50 GETs...");
            long getStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                int randIndex = ThreadLocalRandom.current().nextInt(10);
                String key = "key" + randIndex;
                send(out, in, "GET " + cachebaseName + " " + key);
            }
            long getEnd = System.nanoTime();
            long getDurationMs = (getEnd - getStart) / 1_000_000;
            System.out.println("⏱️ Time taken for 50 GETs: " + getDurationMs + " ms");

            System.out.println("✅ Reflex test complete.");

            // Optional: Command to dump internal worker assignments if you expose it
            // send(out, in, "DUMP WORKER-STATS " + cachebaseName);

        } catch (IOException e) {
            System.err.println("❌ ReflexTest failed: " + e.getMessage());
        }
    }

    private static void send(PrintWriter out, BufferedReader in, String command) throws IOException {
        out.println(command);
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals("<END>") || line.equals("Goodbye!")) {
                break;
            }
            System.out.println("Server > " + line);
        }
    }

    private static String randomString(int len) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
