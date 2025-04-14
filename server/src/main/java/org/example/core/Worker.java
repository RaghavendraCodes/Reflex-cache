package org.example.core;

import java.util.Map;
import java.util.concurrent.*;

public class Worker {
    private final int workerId;
    private final Map<String, String> store = new ConcurrentHashMap<>();
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor();

    public Worker(int workerId) {
        this.workerId = workerId;
    }

    public void handle(CacheCommand cmd) {
        workerThread.submit(() -> {
            long start = System.nanoTime();
            String result;

            switch (cmd.type) {
                case SET -> {
                    store.put(cmd.key, cmd.value);
                    result = "OK";
                }
                case GET -> {
                    result = store.getOrDefault(cmd.key, "(nil)");
                }
                default -> {
                    result = "Invalid command";
                }
            }

            long end = System.nanoTime();
            long durationMicros = (end - start) / 1_000;

            System.out.printf("[Worker-%d] %s key='%s' | time=%dµs | result=%s%n",
                    workerId, cmd.type, cmd.key, durationMicros, result);

            cmd.callback.complete(result);
        });
    }

    public void shutdown() {
        workerThread.shutdownNow();
    }
}
