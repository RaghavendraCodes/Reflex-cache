package org.example.core;

import java.util.concurrent.*;

public class Cachebase {
    private final String name;
    private final ExecutorService dispatcher;
    private final Worker[] workers;
    private final int NUM_WORKERS = 4;

    private final BlockingQueue<CacheCommand> queue = new LinkedBlockingQueue<>();

    public Cachebase(String name) {
        this.name = name;
        this.dispatcher = Executors.newSingleThreadExecutor();
        this.workers = new Worker[NUM_WORKERS];

        for (int i = 0; i < NUM_WORKERS; i++) {
            workers[i] = new Worker(i); // Assign worker ID
        }
    }

    public void startDispatcher() {
        dispatcher.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CacheCommand cmd = queue.take(); // Blocking call

                    int hash = stableHash(cmd.key);
                    int workerIndex = hash % NUM_WORKERS;

                    System.out.printf("[Dispatcher] Key '%s' | hash=%d | routed to Worker-%d%n",
                            cmd.key, hash, workerIndex);

                    workers[workerIndex].handle(cmd);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break; // Exit dispatcher loop on shutdown
                }
            }
        });
    }

    private int stableHash(String key) {
        return Math.abs(key.chars().reduce(0, (a, b) -> 31 * a + b));
    }

    public void shutdown() {
        dispatcher.shutdownNow();
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }

    public String getName() {
        return name;
    }

    public void submit(CacheCommand cmd) {
        queue.offer(cmd);
    }
}
