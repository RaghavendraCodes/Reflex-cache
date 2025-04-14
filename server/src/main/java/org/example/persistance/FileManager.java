package org.example.persistance;

import org.example.core.MemoryStore;

import java.io.*;
import java.util.HashMap;

/**
 * <p><strong>FileManager.java</strong></p>
 *
 * Handles initialization and management of log files and AOF (Append Only File) persistence
 * for each client session in ReflexDB.
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *     <li>Create or fetch log and AOF files per client using unique names</li>
 *     <li>Read logs interactively based on user confirmation</li>
 *     <li>Recover in-memory state from persisted AOF commands</li>
 * </ul>
 *
 * <p>Files are stored under:</p>
 * <ul>
 *     <li>{@code logs/} for .log files</li>
 *     <li>{@code aof/} for .aof files</li>
 * </ul>
 *
 * <p>Used internally by {@code ClientSessionHandler} and {@code CommandProcessor}.</p>
 *
 * @author Raghavendra R
 * @since 10-04-2025
 */
public class FileManager {

    /** Stores mapping of client names to their corresponding log files. */
    private static final HashMap<String, File> hashmap = new HashMap<>();

    /**
     * Initializes or retrieves a log file for a given client name.
     * The log is stored under {@code data/<clientName>/logs/<clientName>.log}.
     *
     * @param name The unique client name.
     * @return A File object pointing to the log file.
     */
    public static File initLogFile(String name) {
        return hashmap.computeIfAbsent(name, n -> {
            File file = new File("data/" + n + "/logs/" + n + ".log");
            file.getParentFile().mkdirs(); // Ensure directory exists
            return file;
        });
    }

    /**
     * Initializes the Append Only File (AOF) for a client.
     * Stored under {@code data/<clientName>/aof/<clientName>.aof}.
     *
     * @param name The unique client name.
     * @return A File object pointing to the AOF file.
     */
    public static File initAOFFile(String name) {
        File file = new File("data/" + name + "/aof/" + name + ".aof");
        file.getParentFile().mkdirs(); // Ensure directory exists
        return file;
    }

    /**
     * Displays the contents of the given log file if the client confirms.
     *
     * @param toClient    PrintWriter to send messages to client.
     * @param fromClient  BufferedReader to receive user input.
     * @param logFile     The log file to preview.
     * @throws IOException If reading from the file or socket fails.
     */

    public static void showLogOption(PrintWriter toClient, BufferedReader fromClient, File logFile) throws IOException {
        if (logFile.exists()) {
            toClient.println("Do you want to open the file " + logFile.getName() + "? (yes/no)");
            String confirm = fromClient.readLine();
            if ("yes".equalsIgnoreCase(confirm)) {
                toClient.println("--- Begin log ---");
                try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        toClient.println(line);
                    }
                }
                toClient.println("--- End log ---");
            }
        }
    }

    /**
     * Recovers in-memory state from the AOF file by replaying SET commands.
     *
     * @param toClient PrintWriter to send progress messages.
     * @param aofFile  The AOF file containing command history.
     * @throws IOException If reading the file fails.
     */
    public static void recoverFromAOF(PrintWriter toClient, File aofFile) throws IOException {
        if (aofFile.exists()) {
            toClient.println("Starting recovery process.");
            try (BufferedReader reader = new BufferedReader(new FileReader(aofFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length == 3 && "SET".equalsIgnoreCase(parts[0])) {
                        MemoryStore.put(parts[1], parts[2]);
                    }
                }
            }
            toClient.println("Data recovered.");
        }
    }
}
