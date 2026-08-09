package io.github.kaivian.kupdater.api.database;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for executing database tasks asynchronously on a dedicated thread pool.
 */
public interface PersistenceExecutor {

    /**
     * Executes a supplier function asynchronously and returns a CompletableFuture with the result.
     *
     * @param task task producing a result
     * @param <T>  result type
     * @return CompletableFuture representing task completion
     */
    <T> CompletableFuture<T> executeAsync(Callable<T> task);

    /**
     * Executes a runnable task asynchronously without returning a value.
     *
     * @param runnable task to execute
     * @return CompletableFuture completing when the task finishes
     */
    CompletableFuture<Void> runAsync(Runnable runnable);

    /**
     * Shuts down the underlying executor pool.
     */
    void shutdown();
}
