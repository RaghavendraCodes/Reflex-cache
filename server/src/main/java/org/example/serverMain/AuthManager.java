package org.example.serverMain;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private static final Map<String, String> credentials = new HashMap<>();
    private static final File CREDENTIAL_FILE = new File("credentials.txt");

    static {
        loadCredentialsFromFile();
    }

    /**
     * Registers a new client. Saves to file if successful.
     */
    public static boolean register(String clientName, String password) {
        if (credentials.containsKey(clientName)) return false;
        credentials.put(clientName, password);
        appendCredentialToFile(clientName, password);
        return true;
    }

    /**
     * Authenticates an existing client.
     */
    public static boolean authenticate(String clientName, String password) {
        return credentials.containsKey(clientName) && credentials.get(clientName).equals(password);
    }

    /**
     * Checks if a client already exists.
     */
    public static boolean clientExists(String clientName) {
        return credentials.containsKey(clientName);
    }

    /**
     * Loads credentials from file into memory at startup.
     */
    public static void loadCredentialsFromFile() {
        if (!CREDENTIAL_FILE.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(CREDENTIAL_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load credentials: " + e.getMessage());
        }
    }

    /**
     * Appends a new credential to the file.
     */
    private static void appendCredentialToFile(String clientName, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CREDENTIAL_FILE, true))) {
            writer.write(clientName + ":" + password);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write credentials: " + e.getMessage());
        }
    }
}
