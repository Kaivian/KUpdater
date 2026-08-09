package io.github.kaivian.kupdater.core.database.provider;

import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class SqliteDatabaseProviderTest {

    @Test
    void testSqliteProviderLifecycle() throws Exception {
        File tempDir = File.createTempFile("kupdater_test_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Logger logger = Logger.getLogger("SqliteTest");
        SqliteDatabaseProvider provider = new SqliteDatabaseProvider(logger, tempDir);

        assertEquals(DatabaseType.SQLITE, provider.getType());
        assertFalse(provider.isAvailable());

        DatabaseConfiguration config = new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(DatabaseType.SQLITE)
                .sqliteFile("sub/test.db")
                .maximumPoolSize(2)
                .build();

        provider.initialize(config);
        assertTrue(provider.isAvailable());
        assertNotNull(provider.getDataSource());

        try (Connection conn = provider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {
            assertTrue(rs.next());
            assertEquals("wal", rs.getString(1).toLowerCase());
        }

        provider.shutdown();
        assertFalse(provider.isAvailable());
    }
}
