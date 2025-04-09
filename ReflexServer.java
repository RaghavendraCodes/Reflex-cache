/**
 * 
 * -------------------
 *  ReflexServer.java
 * -------------------
 * 
 * ReflexCLI Server – A minimal in-memory key-value server inspired by Redis,
 * written in Java. This server accepts TCP client connections and processes
 * basic commands like SET, GET, DISPLAY, TIME, PING, and FILE operations.
 *
 * Features:
 * - In-memory key-value store.
 * - Append-Only File (AOF) based persistence for crash recovery.
 * - Per-client log and append-only file tracking with user identification.
 * - Supports recovery from AOF on startup per client.
 * - Supports command logging for session history inspection.
 *
 * Optional Advanced Concepts (Planned/Partial):
 * - AOF file versioning and rotation to prevent large monolithic logs.
 * - Snapshotting and indexed recovery (in development).
 *
 * Usage:
 * - Run the server. It listens on port 8080.
 * - Clients can connect via the ReflexClient.java CLI.
 *
 * Dependencies:
 * - Java SE 8 or higher
 * - No external libraries required
 *
 * Author: Raghavendra R
 * Date: 10-04-2025
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReflexServer {
    private static final int PORT = 8080;
    private static final HashMap<String, File> hashmap = new HashMap<>();
    private static final LinkedHashMap<String, String> memoryStore = new LinkedHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            toClient.println("Welcome to ReflexCLI Server!");
            toClient.println("Enter your unique name:");

            String clientName = fromClient.readLine();
            if (clientName == null || clientName.trim().isEmpty()) {
                toClient.println("Invalid name. Closing connection.");
                return;
            }

            File logFile = hashmap.computeIfAbsent(clientName, name -> {
                File file = new File("logs/" + name + ".log");
                file.getParentFile().mkdirs();
                return file;
            });

            if (logFile.exists()) {
                toClient.println("Do you want to open the file " + logFile.getName() + "? (yes/no)");
                String confirm = fromClient.readLine();
                if ("yes".equalsIgnoreCase(confirm)) {
                    toClient.println("--- Begin log ---");
                    try (BufferedReader fileReader = new BufferedReader(new FileReader(logFile))) {
                        String line;
                        while ((line = fileReader.readLine()) != null) {
                            toClient.println(line);
                        }
                    }
                    toClient.println("--- End log ---");
                }
            }

            toClient.println("Hello, " + clientName + "! You're now connected.");
            toClient.println("Type commands like SET, GET, ping, time, file, exit.");

            File appendOnlyFile = new File("aof/" + clientName + ".aof");
            appendOnlyFile.getParentFile().mkdirs();

            if (appendOnlyFile.exists()) {
                toClient.println("Starting recovery process.");
                try (BufferedReader fileReader = new BufferedReader(new FileReader(appendOnlyFile))) {
                    String line;
                    while ((line = fileReader.readLine()) != null) {
                        String[] parts = line.split("\\s+", 3);
                        if (parts.length == 3 && "SET".equalsIgnoreCase(parts[0])) {
                            memoryStore.put(parts[1], parts[2]);
                        }
                    }
                }
                toClient.println("Data recovered.");
            }

            try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
                String input;
                while ((input = fromClient.readLine()) != null) {
                    logWriter.write("[" + java.time.LocalTime.now() + "] " + input);
                    logWriter.newLine();
                    logWriter.flush();

                    String[] tokens = input.trim().split("\\s+", 3);
                    String command = tokens[0].toUpperCase();

                    switch (command) {
                        case "PING" -> toClient.println("pong");

                        case "TIME" -> toClient.println("time : " + java.time.LocalTime.now());

                        case "EXIT" -> {
                            toClient.println("Goodbye!");
                            logWriter.write("=== End of session ===");
                            logWriter.newLine();
                            logWriter.flush();
                            return;
                        }

                        case "FILE" -> {
                            toClient.println("--- Begin log ---");
                            try (BufferedReader fileReader = new BufferedReader(new FileReader(logFile))) {
                                String line;
                                while ((line = fileReader.readLine()) != null) {
                                    toClient.println(line);
                                }
                            }
                            toClient.println("--- End log ---");
                        }

                        case "SET" -> {
                            if (tokens.length < 3) {
                                toClient.println("Error: SET needs key and value.");
                            } else {
                                memoryStore.put(tokens[1], tokens[2]);
                                try (BufferedWriter aofWriter = new BufferedWriter(new FileWriter(appendOnlyFile, true))) {
                                    aofWriter.write("SET " + tokens[1] + " " + tokens[2]);
                                    aofWriter.newLine();
                                }
                                toClient.println("OK");
                            }
                        }

                        case "GET" -> {
                            if (tokens.length < 2) {
                                toClient.println("Error: GET needs key.");
                            } else {
                                String value = memoryStore.get(tokens[1]);
                                toClient.println(value != null ? value : "(nil)");
                            }
                        }

                        case "DISPLAY" -> {
                            if (tokens.length > 1) {
                                toClient.println("Error: DISPLAY does not accept arguments.");
                            } else {
                                toClient.println("+----------------------+----------------------+");
                                toClient.println("|        KEY           |        VALUE         |");
                                toClient.println("+----------------------+----------------------+");
                                for (Map.Entry<String, String> entry : memoryStore.entrySet()) {
                                    toClient.printf("| %-20s | %-20s |%n", entry.getKey(), entry.getValue());
                                }
                                toClient.println("+----------------------+----------------------+");
                            }
                        }

                        default -> toClient.println("Unknown command: " + command);
                    }

                    // Always end with prompt so client knows it's ready again
                    toClient.println("<END>");  
                    toClient.flush();
                }
            }

        } catch (IOException e) {
            System.out.println("Client disconnected due to error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
