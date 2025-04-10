package org.example.serverMain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * {@code ReflexClient} is a command-line TCP client that connects to a {@link ReflexServer}
 * and allows interactive communication via a REPL-style CLI.
 * <p>
 * This client enables users to send commands such as {@code SET}, {@code GET}, {@code DISPLAY}, etc.,
 * and displays responses from the server in real-time.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Interactive REPL-style interface for typing commands</li>
 *   <li>Handles server prompts including identity confirmation and file recovery</li>
 *   <li>Gracefully exits on {@code exit} command</li>
 * </ul>
 *
 * <h2>How it works:</h2>
 * <ol>
 *   <li>Establishes a TCP connection to localhost on port 8080</li>
 *   <li>Handles welcome and identification prompts from the server</li>
 *   <li>Processes user commands and prints multi-line server responses</li>
 * </ol>
 *
 * <p><strong>Note:</strong> This class is intended to be run from a terminal or console-enabled IDE.
 * It complements {@code ReflexServer} and forms the client-side CLI interface of ReflexDB.</p>
 *
 * @author Raghavendra R
 * @version 1.0
 * @since 2025-04-10
 */
public class ReflexClient {

    /**
     * Starts the ReflexClient, connects to the ReflexServer at {@code localhost:8080},
     * and launches an interactive command-line session.
     * <p>
     * Handles name input, file recovery prompts, and a continuous loop for entering commands
     * and receiving responses until the user types {@code exit}.
     * </p>
     */
    public static void start(int port, String clientName) {
        try (
                Socket socket = new Socket("localhost", port);
                PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to ReflexCLI Server on port " + port);

            // Wait for the server to prompt for name (if required)
            String line;
            while ((line = fromServer.readLine()) != null) {
                System.out.println("Server: " + line);
                if (line.contains("Enter your unique name:")) break;
            }

            // Automatically send the name passed from main
            System.out.println("Sending client name: " + clientName);
            toServer.println(clientName);

            // Handle optional file recovery
            while ((line = fromServer.readLine()) != null) {
                System.out.println("Server: " + line);
                if (line.startsWith("Do you want to open the file")) {
                    System.out.print("You > ");
                    String confirm = userInput.readLine();
                    toServer.println(confirm);
                }
                if (line.contains("You're now connected.")) break;
            }

            // REPL loop
            while (true) {
                System.out.print("You > ");
                String input = userInput.readLine();

                if (input == null || input.equalsIgnoreCase("exit")) {
                    toServer.println("exit");
                    break;
                }

                toServer.println(input);

                while ((line = fromServer.readLine()) != null) {
                    if (line.equals("<END>") || line.equals("Goodbye!")) {
                        break;
                    }
                    System.out.println("Server > " + line);
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
}
