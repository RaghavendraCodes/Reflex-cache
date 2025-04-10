package org.example.serverMain;

import org.example.persistance.FileManager;
import org.example.core.CommandProcessor;

import java.io.*;
import java.net.Socket;

/**
 *
 *   <p><strong>ClientSessionHandler.java</strong></p>
 *
 * Handles an individual client session in ReflexServer.
 * This class implements {@code Runnable} and is executed
 * in a separate thread per connected client.
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *     <li>Greets the client and asks for a unique name.</li>
 *     <li>Initializes log and AOF (Append Only File) files per client.</li>
 *     <li>Restores state from previous sessions if AOF is available.</li>
 *     <li>Processes commands (SET, GET, FLUSHALL, etc.) using {@link org.example.core.CommandProcessor}.</li>
 *     <li>Maintains logs of all client inputs with timestamps.</li>
 *     <li>Sends server responses and command result termination markers (&lt;END&gt;).</li>
 * </ul>
 *
 * <p><strong>Protocol:</strong></p>
 * <ol>
 *     <li>Client connects via socket.</li>
 *     <li>Server sends welcome messages and asks for a name.</li>
 *     <li>Log and AOF files are initialized using {@link org.example.persistance.FileManager}.</li>
 *     <li>If available, session state is recovered.</li>
 *     <li>Client enters command loop and issues supported commands.</li>
 *     <li>Each command is logged and passed to {@code CommandProcessor}.</li>
 *     <li>Loop continues until client disconnects or issues "exit".</li>
 * </ol>
 *
 * <p><strong>Example Commands:</strong> {@code SET}, {@code GET}, {@code TIME}, {@code FLUSHALL}, {@code EXIT}, etc.</p>
 *
 * <p><strong>Input:</strong></p>
 * <ul>
 *     <li>Client socket stream (BufferedReader)</li>
 *     <li>Command strings sent by the client</li>
 * </ul>
 *
 * <p><strong>Output:</strong></p>
 * <ul>
 *     <li>Server messages to client (PrintWriter)</li>
 *     <li>Execution results of commands</li>
 *     <li>&lt;END&gt; marker after each command</li>
 * </ul>
 *
 * @author Raghavendra R
 * @since 10-04-2025
 */


public class ClientSessionHandler implements Runnable {

    private final Socket clientSocket;
    private final String clientName;

    /**
     * Constructs a new ClientSessionHandler for the given client socket and authenticated name.
     *
     * @param socket     The socket representing the connected client.
     * @param clientName The unique authenticated name for this session.
     */
    public ClientSessionHandler(Socket socket, String clientName) {
        this.clientSocket = socket;
        this.clientName = clientName;
    }

    /**
     * Starts the client session in its own thread.
     * Handles session setup, recovery, command processing, and cleanup.
     */
    @Override
    public void run() {
        try (
                BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            toClient.println("Welcome to ReflexCLI Server!");
            toClient.println("Enter your unique name:");
            // Skipping read because name is trusted and passed by ReflexServer
            fromClient.readLine(); // Discard line if sent (for compatibility with old clients)

            // Prepare log and AOF file paths for this client
            File logFile = FileManager.initLogFile(clientName);
            File aofFile = FileManager.initAOFFile(clientName);

            // Offer session recovery
            FileManager.showLogOption(toClient, fromClient, logFile);
            FileManager.recoverFromAOF(toClient, aofFile);

            toClient.println("Hello, " + clientName + "! You're now connected.");
            toClient.println("Type commands like SET, GET, PING, TIME, FILE, FLUSHALL, EXIT.");

            // Start CLI command loop
            try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
                String input;
                while ((input = fromClient.readLine()) != null) {
                    logWriter.write("[" + java.time.LocalTime.now() + "] " + input);
                    logWriter.newLine();
                    logWriter.flush();

                    CommandProcessor.process(input, toClient, fromClient, aofFile, logWriter);

                    toClient.println("<END>");
                    toClient.flush();

                    if (input.equalsIgnoreCase("exit")) {
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Client '" + clientName + "' disconnected or error occurred: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
