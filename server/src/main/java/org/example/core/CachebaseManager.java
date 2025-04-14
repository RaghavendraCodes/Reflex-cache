package org.example.core;

import java.util.concurrent.ConcurrentHashMap;

public class CachebaseManager {
    private static final ConcurrentHashMap<String, Cachebase> cachebases = new ConcurrentHashMap<>();

    public static synchronized String createCachebase(String name) {
        if (cachebases.containsKey(name)) return "Cachebase already exists.";
        Cachebase cb = new Cachebase(name);
        cachebases.put(name, cb);
        cb.startDispatcher(); // Start dispatcher after creation
        return "Cachebase '" + name + "' created.";
    }

    public static synchronized String removeCachebase(String name) {
        Cachebase cb = cachebases.remove(name);
        if (cb == null) return "Cachebase does not exist.";
        cb.shutdown(); // Gracefully stop workers and dispatcher
        return "Cachebase '" + name + "' removed.";
    }

    public static Cachebase getCachebase(String name) {
        return cachebases.get(name);
    }

}
