/**
 * 
 * -------------------
 *  ReflexClient.java
 * -------------------
 * 
 * ReflexCLI Client – A command-line TCP client for interacting with the ReflexServer.
 * Connects to the server, identifies the user, and allows real-time command input.
 *
 * Features:
 * - Interactive CLI to send commands like SET, GET, DISPLAY, etc.
 * - Automatically handles server prompts like log file confirmations and recovery notices.
 * - Gracefully closes on `exit` command.
 * - Simple REPL-style input-output for usability.
 *
 * How it works:
 * - Establishes TCP connection to localhost:8080.
 * - Waits for welcome message and prompts.
 * - Sends input from terminal to server and prints the server's response.
 *
 * Notes:
 * - Should be run in a terminal or IDE console.
 * - Designed to work hand-in-hand with ReflexServer.java.
 *
 * Author: Raghavendra R
 * Date: 10-04-2025
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ReflexClient {
    public static void main(String[] args) {
        int port = 8080;

        try (
            Socket socket = new Socket("localhost", port);
            PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to ReflexCLI Server");

            // Initial welcome and name prompt
            String line;
            while ((line = fromServer.readLine()) != null) {
                System.out.println("Server: " + line);
                if (line.contains("Enter your unique name:")) break;
            }

            // Send unique name
            System.out.print("You > ");
            String name = userInput.readLine();
            toServer.println(name);

            // File prompt
            while ((line = fromServer.readLine()) != null) {
                System.out.println("Server: " + line);
                if (line.startsWith("Do you want to open the file")) {
                    System.out.print("You > ");
                    String confirm = userInput.readLine();
                    toServer.println(confirm);
                }
                if (line.contains("You're now connected.")) break;
            }

            // Main CLI loop
            while (true) {
                System.out.print("You > ");
                String input = userInput.readLine();

                if (input == null || input.equalsIgnoreCase("exit")) {
                    toServer.println("exit");
                    break;
                }

                toServer.println(input);

                // Read multi-line output until <END> or Goodbye!
                while ((line = fromServer.readLine()) != null) {
                    if (line.equals("<END>") || line.equals("Goodbye!")) {
                        break;
                    }
                    System.out.println("Server > " + line);
                }

                // If command was "exit", exit loop
                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
}
