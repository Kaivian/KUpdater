package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;
import io.github.kaivian.kupdater.api.database.SchemaMigration;
import io.github.kaivian.kupdater.core.database.dialect.SqliteDialect;
import io.github.kaivian.kupdater.core.database.provider.SqliteDatabaseProvider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MigrationRunnerTest {

    @Test
    void testMigrationExecutionAndTracking() throws Exception {
        File tempDir = File.createTempFile("kupdater_migration_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Logger logger = Logger.getLogger("MigrationTest");
        SqliteDatabaseProvider provider = new SqliteDatabaseProvider(logger, tempDir);
        DatabaseConfiguration config = new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(DatabaseType.SQLITE)
                .sqliteFile("migration_test.db")
                .maximumPoolSize(2)
                .build();
        provider.initialize(config);

        SqliteDialect dialect = new SqliteDialect();
        MigrationRunner runner = new MigrationRunner(provider, dialect, logger);

        runner.registerMigration(new V1__InitialSchemaMigration());
        runner.registerMigration(new SchemaMigration() {
            @Override
            public int getVersion() {
                return 2;
            }

            @Override
            public String getDescription() {
                return "Test Table Creation";
            }

            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50));");
                }
            }
        });

        // Run migrations
        runner.runMigrations();

        // Verify history table entries
        try (Connection conn = provider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version, description FROM kupdater_schema_history ORDER BY version ASC")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("version"));

            assertTrue(rs.next());
            assertEquals(2, rs.getInt("version"));
            assertEquals("Test Table Creation", rs.getString("description"));

            assertFalse(rs.next());
        }

        // Running migrations again should be idempotent (no exceptions, skips applied versions)
        assertDoesNotThrow(runner::runMigrations);

        provider.shutdown();
    }

    @Test
    void testMigrationRollbackOnFailure() throws Exception {
        File tempDir = File.createTempFile("kupdater_rollback_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Logger logger = Logger.getLogger("RollbackTest");
        SqliteDatabaseProvider provider = new SqliteDatabaseProvider(logger, tempDir);
        DatabaseConfiguration config = new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(DatabaseType.SQLITE)
                .sqliteFile("rollback_test.db")
                .maximumPoolSize(2)
                .build();
        provider.initialize(config);

        SqliteDialect dialect = new SqliteDialect();
        MigrationRunner runner = new MigrationRunner(provider, dialect, logger);

        runner.registerMigration(new SchemaMigration() {
            @Override
            public int getVersion() {
                return 10;
            }

            @Override
            public String getDescription() {
                return "Failing Migration";
            }

            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE TABLE rollback_dummy (id INT);");
                    // Force SQL exception
                    stmt.execute("INVALID SQL SYNTAX THAT FAILS;");
                }
            }
        });

        assertThrows(SQLException.class, runner::runMigrations);

        // Verify table was rolled back and not recorded in history
        try (Connection conn = provider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM kupdater_schema_history WHERE version = 10")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        provider.shutdown();
    }
}
