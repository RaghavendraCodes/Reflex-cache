package org.example.core;

import java.util.*;

public class Databases {

    // Stores mapping of clientName -> list of database names
    private static final Map<Object, Object> databases =
            Collections.synchronizedMap(new LinkedHashMap<>());

    // Tracks clientName -> currently active database
    private static final Map<String, String> activeDatabase =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Creates a new database for a client.
     *
     * @param clientName The client requesting the database.
     * @param dbName     The name of the database to create.
     * @return OK if successful, error message otherwise.
     */
    public static String createDatabase(String clientName, String dbName) {
        if (clientName == null || dbName == null || clientName.isBlank() || dbName.isBlank()) {
            return "Error: Client name and database name must not be empty.";
        }

        List<String> dbList = (List<String>) databases.computeIfAbsent(clientName, k -> Collections.synchronizedList(new ArrayList<>()));
        if (dbList.contains(dbName)) return "Error: Database already exists.";
        dbList.add(dbName);
        return "OK";
    }

    /**
     * Lists all databases for a given client.
     *
     * @param clientName The client whose databases are requested.
     * @return A list of database names (empty list if none).
     */
    public static List<String> listDatabases(String clientName) {
        return (List<String>) databases.getOrDefault(clientName, Collections.emptyList());
    }

    /**
     * Removes all databases and active state for a given client.
     *
     * @param clientName The client to remove.
     */
    public static void removeClient(String clientName) {
        databases.remove(clientName);
        activeDatabase.remove(clientName);
    }

    /**
     * Removes a specific database for a given client.
     *
     * @param clientName The client.
     * @param dbName     The database to remove.
     */
    public static void removeDatabase(String clientName, String dbName) {
        List<String> dbList = (List<String>) databases.get(clientName);
        if (dbList != null) {
            dbList.remove(dbName);
        }
        if (dbName.equals(activeDatabase.get(clientName))) {
            activeDatabase.remove(clientName); // Unset active if deleted
        }
    }

    /**
     * Sets a database as active for the given client.
     *
     * @param clientName The client.
     * @param dbName     The database to set as active.
     * @return true if successful, false if invalid.
     */
    public static boolean setActiveDatabase(String clientName, String dbName) {
        List<String> dbList = (List<String>) databases.get(clientName);
        if (dbList != null && dbList.contains(dbName)) {
            activeDatabase.put(clientName, dbName);
            return true;
        }
        return false;
    }

    /**
     * Gets the currently active database for a client.
     *
     * @param clientName The client.
     * @return The active database name, or null if none is set.
     */
    public static String getActiveDatabase(String clientName) {
        return activeDatabase.get(clientName);
    }

    /**
     * Renames a database for a given client.
     *
     * @param clientName The client.
     * @param oldName    The current name.
     * @param newName    The new name.
     * @return true if rename is successful, false otherwise.
     */
    public static boolean renameDatabase(String clientName, String oldName, String newName) {
        if (clientName == null || oldName == null || newName == null) return false;
        List<String> dbs = (List<String>) databases.get(clientName);
        if (dbs == null || !dbs.contains(oldName) || dbs.contains(newName)) return false;

        dbs.remove(oldName);
        dbs.add(newName);

        // Update active database reference if necessary
        if (oldName.equals(activeDatabase.get(clientName))) {
            activeDatabase.put(clientName, newName);
        }
        return true;
    }

    /**
     * Gets all databases map — used internally.
     */
    public static Map<Object, Object> getAllDatabases() {
        return databases;
    }
}
