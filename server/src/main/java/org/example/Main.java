package org.example;

import org.example.serverMain.AuthManager;
import org.example.serverMain.ReflexServer;
import org.example.serverMain.ReflexClient;

import java.util.Scanner;

import static org.example.serverMain.AuthManager.loadCredentialsFromFile;

public class Main {

    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("Start ReflexCache [server/client/both]: ");
//        String input = scanner.nextLine().trim().toLowerCase();
//
//        loadCredentialsFromFile();
//
//        System.out.print("Enter port (default: 6379): ");
//        String portInput = scanner.nextLine().trim();
//        int port = portInput.isEmpty() ? 6379 : Integer.parseInt(portInput);
//
//        System.out.print("Enter client name: ");
//        String clientName = scanner.nextLine().trim();
//
//        // Server should not ask for authentication
//        if ("client".equals(input) || "both".equals(input)) {
//            System.out.print("Are you a new user? (yes/no): ");
//            String isNew = scanner.nextLine().trim().toLowerCase();
//
//            System.out.print("Enter password: ");
//            String password = scanner.nextLine().trim();
//
//            if (isNew.equals("yes")) {
//                if (!AuthManager.register(clientName, password)) {
//                    System.out.println("Client already exists. Authentication failed.");
//                    return;
//                } else {
//                    System.out.println("Registered successfully.");
//                }
//            } else {
//                if (!AuthManager.authenticate(clientName, password)) {
//                    System.out.println("Authentication failed. Exiting...");
//                    return;
//                } else {
//                    System.out.println("Authenticated successfully.");
//                }
//            }
//        }
//
//        // Starting ReflexCache server, client or both based on user input
//        switch (input) {
//            case "server":
//                ReflexServer.start(port, clientName);  // Start server only without authentication
//                break;
//            case "client":
//                ReflexClient.start(port, clientName);  // Start client with authentication
//                break;
//            case "both":
//                Thread serverThread = new Thread(() -> ReflexServer.start(port, clientName));
//                Thread clientThread = new Thread(() -> {
//                    try {
//                        Thread.sleep(1000);  // Wait a second before starting the client
//                        ReflexClient.start(port, clientName);
//                    } catch (InterruptedException e) {
//                        System.err.println("Client thread interrupted.");
//                    }
//                });
//
//                serverThread.start();
//                clientThread.start();
//                break;
//            default:
//                System.out.println("Invalid input. Please choose 'server', 'client', or 'both'.");
//        }
    }
}
