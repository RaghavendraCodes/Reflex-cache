package org.example.core;

import java.util.concurrent.CompletableFuture;

public class CacheCommand {
    public enum Type { SET, GET }

    public final Type type;
    public final String key;
    public final String value;
    public final CompletableFuture<String> callback;

    // Main constructor
    public CacheCommand(Type type, String key, String value) {
        this(type, key, value, new CompletableFuture<>());
    }

    // Extra constructor for external CompletableFuture
    public CacheCommand(Type type, String key, String value, CompletableFuture<String> callback) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.callback = callback;
    }
}
