package io.github.kaivian.kupdater.core.database.execution;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AsyncPersistenceExecutorTest {

    @Test
    void testAsyncExecution() throws Exception {
        Logger logger = Logger.getLogger("AsyncTest");
        AsyncPersistenceExecutor executor = new AsyncPersistenceExecutor(logger, 2);

        CompletableFuture<String> future = executor.executeAsync(() -> {
            assertTrue(Thread.currentThread().getName().startsWith("KUpdater-DB-Worker"));
            return "SUCCESS";
        });

        assertEquals("SUCCESS", future.get());

        CompletableFuture<Void> runFuture = executor.runAsync(() -> {
            assertTrue(Thread.currentThread().getName().startsWith("KUpdater-DB-Worker"));
        });

        assertNull(runFuture.get());

        executor.shutdown();
    }
}
