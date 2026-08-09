package io.github.kaivian.kupdater.core.database.execution;

import io.github.kaivian.kupdater.api.database.PersistenceExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Thread-safe persistence executor managing a dedicated worker thread pool for database operations.
 */
public class AsyncPersistenceExecutor implements PersistenceExecutor {

    private final ExecutorService executorService;
    private final Logger logger;

    public AsyncPersistenceExecutor(Logger logger, int threadPoolSize) {
        this.logger = logger;
        int threads = Math.max(2, threadPoolSize);
        this.executorService = Executors.newFixedThreadPool(threads, new DaemonThreadFactory("KUpdater-DB-Worker"));
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executorService.submit(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
