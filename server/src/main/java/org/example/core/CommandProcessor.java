package org.example.core;

import java.io.*;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.example.persistance.FileManager;
import org.example.core.*;

import static org.example.core.Databases.createDatabase;
import static org.example.core.Databases.listDatabases;
import static org.example.persistance.FileManager.recoverFromAOF;

/**
 * <p><strong>CommandProcessor.java</strong></p>
 *
 * The {@code CommandProcessor} class is responsible for handling all
 * commands issued by a client during a ReflexDB session. It interprets,
 * processes, and executes the commands, while interacting with
 * {@link MemoryStore} and persistence mechanisms (AOF file).
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *     <li>Parse and execute commands such as {@code SET}, {@code GET}, {@code FLUSHFULL}, etc.</li>
 *     <li>Write commands to the Append Only File (AOF) for persistence.</li>
 *     <li>Log client commands and manage session closure.</li>
 *     <li>Communicate appropriate responses back to the client via {@code PrintWriter}.</li>
 * </ul>
 *
 * <p><strong>Supported Commands:</strong></p>
 * <ul>
 *     <li><strong>PING</strong>: Responds with "pong".</li>
 *     <li><strong>TIME</strong>: Returns the current server time.</li>
 *     <li><strong>EXIT</strong>: Terminates the session gracefully.</li>
 *     <li><strong>FILE</strong>: Displays contents of the AOF file.</li>
 *     <li><strong>SET key value</strong>: Stores a key-value pair.</li>
 *     <li><strong>GET key</strong>: Retrieves a value by key.</li>
 *     <li><strong>DISPLAY</strong>: Displays all stored key-value pairs in a table format.</li>
 *     <li><strong>FLUSH</strong>: Clears only in-memory data.</li>
 *     <li><strong>RECOVER</strong>: Recovers in-memory data from AOF file.</li>
 *     <li><strong>FLUSHFULL</strong>: Clears both in-memory and AOF file.</li>
 * </ul>
 *
 * <p><strong>Input:</strong></p>
 * <ul>
 *     <li>Command line string input from the client</li>
 *     <li>Socket-based {@code BufferedReader} for additional confirmations (if required)</li>
 * </ul>
 *
 * <p><strong>Output:</strong></p>
 * <ul>
 *     <li>Result of command execution sent via {@code PrintWriter}</li>
 *     <li>AOF persistence for data-modifying commands</li>
 *     <li>Optional logging of command activity through {@code BufferedWriter}</li>
 * </ul>
 *
 * @author Raghavendra R
 * @since 10-04-2025

 * Processes a single command input from the client.
 * Based on the command keyword, the method performs the appropriate action,
 * such as reading/writing from memory store, persisting to AOF, or sending responses.
 *
 *
 */



public class CommandProcessor {

    /**
     * Processes a single command input from the client.
     * Based on the command keyword, the method performs the appropriate action,
     * such as reading/writing from memory store, persisting to AOF, or sending responses.
     *
     * @param input      The raw command input from the client.
     * @param toClient   The writer used to send responses back to the client.
     * @param fromClient The reader used to get additional input (e.g., confirmations).
     * @param aofFile    The Append Only File used for persistence of SET operations.
     * @param logWriter  The writer used to log client commands with timestamps.
     * @throws IOException If the client issues EXIT or IO operations fail.
     */

    public static void process(String input, PrintWriter toClient, BufferedReader fromClient,
                               File aofFile, BufferedWriter logWriter, String clientName) throws IOException {

        String[] tokens = input.trim().split("\\s+", 4);  // At most 3 parts to capture SET value with spaces
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            toClient.println("Error: Empty command.");
            return;
        }

        String command = tokens[0].toUpperCase();

        switch (command) {
            case "PING" -> toClient.println("pong");

            case "TIME" -> toClient.println("time : " + LocalTime.now());

            case "EXIT" -> {
                toClient.println("Goodbye!");
                logWriter.write("=== End of session ===");
                logWriter.newLine();
                logWriter.flush();
                throw new IOException("Client exit requested");
            }

            case "FILE" -> {
                if (aofFile.exists()) {
                    toClient.println("--- Begin AOF Log ---");
                    try (BufferedReader fileReader = new BufferedReader(new FileReader(aofFile))) {
                        String line;
                        while ((line = fileReader.readLine()) != null) {
                            toClient.println(line);
                        }
                    }
                    toClient.println("--- End AOF Log ---");
                } else {
                    toClient.println("No AOF file found.");
                }
            }

            // case "SET" -> {
            //     if (tokens.length < 3) {
            //         toClient.println("Error: SET requires a key and a value.");
            //     } else {
            //         MemoryStore.put(tokens[1], tokens[2]);
            //         try (BufferedWriter writer = new BufferedWriter(new FileWriter(aofFile, true))) {
            //             writer.write("SET " + tokens[1] + " " + tokens[2]);
            //             writer.newLine();
            //         }
            //         toClient.println("OK");
            //     }
            // }

            // case "GET" -> {
            //     if (tokens.length < 2) {
            //         toClient.println("Error: GET requires a key.");
            //     } else {
            //         String value = MemoryStore.get(tokens[1]);
            //         toClient.println(value != null ? value : "(nil)");
            //     }
            // }

            case "SET" -> {
                String cachebase, key, value;

                // System.out.println("Tokens: " + Arrays.toString(tokens));


                if (tokens.length == 4) {
                    // Format: SET <cachebase> <key> <value>
                    cachebase = tokens[1];
                    key = tokens[2];
                    value = tokens[3];
                } else {
                    toClient.println("Usage: SET <cachebase> <key> <value>");
                    return;
                }

                // Get the specified cachebase directly
                Cachebase cb = CachebaseManager.getCachebase(cachebase);
                if (cb == null) {
                    toClient.println("Cachebase not found: " + cachebase);
                } else {
                    CompletableFuture<String> result = new CompletableFuture<>();
                    cb.submit(new CacheCommand(CacheCommand.Type.SET, key, value, result));
                    toClient.println(result.join());
                }
            }

            case "GET" -> {
                if (tokens.length < 3) {
                    toClient.println("Usage: GET <cachebase> <key>");
                } else {
                    String cachebase = tokens[1];
                    String key = tokens[2];

                    // Get the specified cachebase directly
                    Cachebase cb = CachebaseManager.getCachebase(cachebase);
                    if (cb == null) {
                        toClient.println("Cachebase not found: " + cachebase);
                    } else {
                        CompletableFuture<String> result = new CompletableFuture<>();
                        cb.submit(new CacheCommand(CacheCommand.Type.GET, key, null, result));
                        toClient.println(result.join());
                    }
                }
            }


            case "DISPLAY" -> {
                Map<String, String> allData = MemoryStore.getAll();
                if (allData.isEmpty()) {
                    toClient.println("(empty)");
                } else {
                    toClient.println("+----------------------+----------------------+");
                    toClient.println("|        KEY           |        VALUE         |");
                    toClient.println("+----------------------+----------------------+");
                    for (Map.Entry<String, String> entry : allData.entrySet()) {
                        toClient.printf("| %-20s | %-20s |%n", entry.getKey(), entry.getValue());
                    }
                    toClient.println("+----------------------+----------------------+");
                }
            }

            case "FLUSH" -> {
                MemoryStore.clear();
                toClient.println("In-memory data cleared.");
            }

            case "RECOVER" -> {
                recoverFromAOF(toClient, aofFile);
                toClient.println("Recovery complete.");
            }

            case "FLUSHFULL" -> {
                MemoryStore.clear();
                try (FileWriter writer = new FileWriter(aofFile, false)) {
                    writer.write(""); // Clear AOF
                    writer.flush();
                }
                toClient.println("In-memory store and AOF file cleared.");
            }

            // case "CREATE" -> {
            //     if (tokens.length != 3 && !tokens[1].equalsIgnoreCase("CACHEBASE")) {
            //         toClient.println("Usage: CREATE CACHEBASE <databaseName>");
            //     } else {
            //         toClient.println(createDatabase(clientName, tokens[2]));
            //     }
            // }

            case "LISTDB" -> {
                if (tokens.length != 1) {
                    toClient.println("Usage: LISTDB");
                } else {
                    toClient.println(CachebaseManager.getCachebase(clientName));
                }
            }

            case "USE" -> {
                if(tokens.length != 2) {
                    toClient.println("Usage: USE <CachebaseName>");
                } else {
                    toClient.println();
                }
            }

            case "CREATE" -> {
                if (tokens.length != 3 || !tokens[1].equalsIgnoreCase("CACHEBASE")) {
                    toClient.println("Usage: CREATE CACHEBASE <name>");
                } else {
                    toClient.println(CachebaseManager.createCachebase(tokens[2]));
                }
            }
            
            case "REMOVE" -> {
                if (tokens.length != 3 || !tokens[1].equalsIgnoreCase("CACHEBASE")) {
                    toClient.println("Usage: REMOVE CACHEBASE <name>");
                } else {
                    toClient.println(CachebaseManager.removeCachebase(tokens[2]));
                }
            }
            

            default -> toClient.println("Unknown command: " + command);
        }
    }
}
