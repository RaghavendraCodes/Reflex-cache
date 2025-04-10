package org.example.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p><strong>MemoryStore.java</strong></p>
 *
 * Acts as the in-memory key-value store for ReflexDB.
 * All data is stored in a {@code LinkedHashMap} to preserve insertion order.
 *
 * <p>This class provides basic operations to manipulate the store, such as:</p>
 * <ul>
 *     <li>Inserting key-value pairs using {@link #put(String, String)}</li>
 *     <li>Retrieving values by key using {@link #get(String)}</li>
 *     <li>Viewing all key-value pairs using {@link #getAll()}</li>
 *     <li>Clearing the entire store using {@link #clear()}</li>
 * </ul>
 *
 * <p>This is a static utility class and does not support instantiation.</p>
 *
 * <p><strong>Thread Safety:</strong> This implementation is <em>not</em> thread-safe.
 * In future versions, synchronization or concurrent data structures may be introduced.</p>
 *
 * @author Raghavendra R
 * @since 10-04-2025
 */
public class MemoryStore {

    /** Internal map storing all key-value pairs in memory. */
    private static final LinkedHashMap<String, String> store = new LinkedHashMap<>();

    /**
     * Inserts or updates a key-value pair in the in-memory store.
     *
     * @param key   The key to store.
     * @param value The value to associate with the key.
     */
    public static void put(String key, String value) {
        store.put(key, value);
    }

    /**
     * Retrieves the value associated with a given key.
     *
     * @param key The key whose value is to be fetched.
     * @return The value if the key exists, or {@code null} otherwise.
     */
    public static String get(String key) {
        return store.get(key);
    }

    /**
     * Returns an unmodifiable view of the current in-memory store.
     *
     * @return A {@code Map} of all key-value pairs.
     */
    public static Map<String, String> getAll() {
        return Collections.unmodifiableMap(store);
    }

    /**
     * Clears all entries from the in-memory store.
     */
    public static void clear() {
        store.clear();
    }
}
