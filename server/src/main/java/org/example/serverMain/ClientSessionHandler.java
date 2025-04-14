package org.example.serverMain;

import org.example.persistance.FileManager;
import org.example.core.CommandProcessor;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;

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
 *     <li>Processes commands (SET, GET, FLUSHALL, etc.) using {@link CommandProcessor}.</li>
 *     <li>Maintains logs of all client inputs with timestamps.</li>
 *     <li>Sends server responses and command result termination markers (&lt;END&gt;).</li>
 * </ul>
 *
 * <p><strong>Protocol:</strong></p>
 * <ol>
 *     <li>Client connects via socket.</li>
 *     <li>Server sends welcome messages and asks for a name.</li>
 *     <li>Log and AOF files are initialized using {@link FileManager}.</li>
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
    private String clientName;

    public ClientSessionHandler(Socket socket, String clientName) {
        this.clientSocket = socket;
        this.clientName = clientName;  // Correct assignment here
    }

    public ClientSessionHandler(Socket socket) {
        this.clientSocket = socket;
        // don't need clientName here yet, will be read from input
    }



    @Override
    public void run() {
        try (
                BufferedReader fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter toClient = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // ASCII Banner
            toClient.println("██████╗ ███████╗███████╗██╗     ███████╗██╗  ██╗");
            toClient.println("██╔══██╗██╔════╝██╔════╝██║     ██╔════╝╚██╗██╔╝");
            toClient.println("██████╔╝█████╗  █████╗  ██║     █████╗   ╚███╔╝ ");
            toClient.println("██║██╗  ██╔══╝  ██╔══╝  ██║     ██╔══╝   ██╔██╗ ");
            toClient.println("██║ ██╗ ███████╗███████╗███████╗███████╗██╔╝ ██╗");
            toClient.println("╚═╝ ╚═╝ ╚══════╝╚══════╝╚══════╝╚══════╝╚═╝  ╚═╝");
            toClient.println();
            toClient.println(" ██████╗ █████╗  ██████╗██╗  ██╗███████╗");
            toClient.println("██╔════╝██╔══██╗██╔════╝██║  ██║██╔════╝");
            toClient.println("██║     ███████║██║     ███████║█████╗  ");
            toClient.println("██║     ██╔══██║██║     ██╔══██║██╔══╝  ");
            toClient.println("╚██████╗██║  ██║╚██████╗██║  ██║███████╗");
            toClient.println(" ╚═════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝");
            toClient.println();

            // System Info
            toClient.println("System Info:");
            toClient.println("  OS      : " + System.getProperty("os.name"));
            toClient.println("  Java    : " + System.getProperty("java.version"));
            toClient.println("  Threads : " + Thread.activeCount());
            toClient.println();

            toClient.println("Welcome to ReflexCLI Server!");
            toClient.println("Enter your unique name:");
            this.clientName = fromClient.readLine();
            // fromClient.readLine(); // Discard extra if old client

            File logFile = FileManager.initLogFile(clientName);
            File aofFile = FileManager.initAOFFile(clientName);

            FileManager.showLogOption(toClient, fromClient, logFile);
            FileManager.recoverFromAOF(toClient, aofFile);

            toClient.println("Hello, " + clientName + "! You're now connected.");
            toClient.println("Type commands like SET, GET, PING, TIME, FILE, FLUSHALL, EXIT.");

            try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
                String input;
                while ((input = fromClient.readLine()) != null) {
                    logWriter.write("[" + LocalTime.now() + "] " + input);
                    logWriter.newLine();
                    logWriter.flush();

                    CommandProcessor.process(input, toClient, fromClient, aofFile, logWriter, clientName);

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
