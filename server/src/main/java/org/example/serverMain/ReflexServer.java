package org.example.serverMain;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The {@code ReflexServer} class acts as the entry point for starting the ReflexDB server.
 * <p>
 * It listens for incoming client connections on a specified port and spawns a new thread
 * for each client session using {@link ClientSessionHandler}.
 * </p>
 *
 * <p>This server is designed to handle multiple concurrent client connections.</p>
 *
 * @author Raghavendra R
 * @version 1.0
 * @since 2025-04-10
 */

public class ReflexServer {
    /**
     * The port on which the ReflexDB server listens for incoming connections.
     */
    // private static final int PORT = 8080;

    /**
     * Starts the ReflexDB server.
     * <p>
     * This method opens a {@link ServerSocket} on the specified port and waits for
     * client connections. For each client that connects, a new {@code ClientSessionHandler}
     * is instantiated and run in a separate thread.
     * </p>
     *
     * <p>Logs will be printed to the console when the server starts and when a client connects.</p>
     *
     * @throws RuntimeException if the server fails to start due to an {@link IOException}
     */

    public static void start(int port, String clientName) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ReflexServer running on port " + port + " for client: " + clientName);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getInetAddress());

                // Ensure the clientName is passed to the session handler
                new Thread(() -> new ClientSessionHandler(clientSocket, clientName).run()).start();
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}