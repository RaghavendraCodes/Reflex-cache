package org.example.serverMain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.example.ReflexApiTest;
import org.example.serverMain.AuthManager;
import org.example.ReflexTest;
import org.example.ReflexApiTest;

import static org.example.ReflexApiTest.runTest;

/**
 * {@code ReflexClient} is a command-line TCP client that connects to a {@link ReflexServer}
 * and allows interactive communication via a REPL-style CLI.
 * <p>
 * This client enables users to send commands such as {@code SET}, {@code GET}, {@code DISPLAY}, etc.,
 * and displays responses from the server in real-time.
 * </p>
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

    public static void main(String[] args) {
        // Create a scanner for input
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        // Default port for the client to connect to
        int port = 8080;

        boolean authenticated = false;

        // Ask the user for the client name
        System.out.print("Enter your client name: ");
        String clientName = scanner.nextLine().trim();

        // Handle authentication logic for the client
        System.out.print("Are you a new user? (yes/no): ");
        String isNew = scanner.nextLine().trim().toLowerCase();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine().trim();

        if (isNew.equals("yes")) {
            // Register new client
            if (!AuthManager.register(clientName, password)) {
                System.out.println("Client already exists. Authentication failed.");
                return;
            } else {
                System.out.println("Registered successfully.");
            }
        } else {
            // Authenticate existing client
            if (!AuthManager.authenticate(clientName, password)) {
                System.out.println("Authentication failed. Exiting...");
                return;
            } else {
                authenticated = true;
                System.out.println("Authenticated successfully.");
            }
        }

        if (authenticated) {
            System.out.print("Do you want to run the cachebase test? (yes/no): ");
            String runTest = scanner.nextLine().trim().toLowerCase();

            if (runTest.equals("yes")) {
                // ReflexTest.runTest("localhost", port, "testdb");
                ReflexApiTest.runTest("localhost", port, "testdb", 1000);
            } else {
                start(port, clientName);
            }
        }


        // Start the client after successful authentication
//        start(port, clientName);
    }
}
