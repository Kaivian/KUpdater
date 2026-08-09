package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.DatabaseProvider;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import io.github.kaivian.kupdater.core.database.dialect.DatabaseDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Migration runner responsible for managing and executing versioned schema migrations.
 */
public class MigrationRunner {

    private static final String HISTORY_TABLE = "kupdater_schema_history";

    private final DatabaseProvider provider;
    private final DatabaseDialect dialect;
    private final Logger logger;
    private final List<SchemaMigration> migrations = new ArrayList<>();

    public MigrationRunner(DatabaseProvider provider, DatabaseDialect dialect, Logger logger) {
        this.provider = provider;
        this.dialect = dialect;
        this.logger = logger;
    }

    /**
     * Registers a migration step.
     *
     * @param migration migration instance
     */
    public void registerMigration(SchemaMigration migration) {
        migrations.add(migration);
    }

    /**
     * Runs all pending migrations sequentially within isolated transactions.
     *
     * @throws SQLException if a migration error occurs
     */
    public void runMigrations() throws SQLException {
        if (!provider.isAvailable()) {
            throw new SQLException("Cannot run migrations: database provider is unavailable.");
        }

        // Validate migration version uniqueness
        validateMigrations();

        try (Connection conn = provider.getConnection()) {
            // Step 1: Bootstrap schema history table if not exists
            bootstrapHistoryTable(conn);

            // Step 2: Query applied versions
            Set<Integer> appliedVersions = getAppliedVersions(conn);
            int nextRank = appliedVersions.size() + 1;

            // Sort registered migrations by version
            migrations.sort(Comparator.comparingInt(SchemaMigration::getVersion));

            for (SchemaMigration migration : migrations) {
                if (appliedVersions.contains(migration.getVersion())) {
                    continue;
                }

                logger.info("[KUpdater] Executing schema migration V" + migration.getVersion() + ": " + migration.getDescription());
                long startTime = System.currentTimeMillis();

                boolean originalAutoCommit = conn.getAutoCommit();
                try {
                    conn.setAutoCommit(false);

                    // Execute migration DDL/DML
                    migration.execute(conn);

                    // Record applied migration in history table
                    recordMigrationSuccess(conn, nextRank++, migration, System.currentTimeMillis() - startTime);

                    conn.commit();
                    logger.info("[KUpdater] Successfully applied schema migration V" + migration.getVersion());
                } catch (Exception e) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        logger.warning("[KUpdater] Error rolling back failed migration V" + migration.getVersion() + ": " + rollbackEx.getMessage());
                    }
                    throw new SQLException("Failed executing database migration V" + migration.getVersion() + ": " + e.getMessage(), e);
                } finally {
                    conn.setAutoCommit(originalAutoCommit);
                }
            }
        }
    }

    private void validateMigrations() {
        Set<Integer> versions = new HashSet<>();
        for (SchemaMigration m : migrations) {
            if (!versions.add(m.getVersion())) {
                throw new IllegalArgumentException("Duplicate migration version detected: V" + m.getVersion());
            }
        }
    }

    private void bootstrapHistoryTable(Connection conn) throws SQLException {
        String sql = dialect.getCreateSchemaHistoryTableSql(HISTORY_TABLE);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private Set<Integer> getAppliedVersions(Connection conn) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        String sql = "SELECT " + dialect.quoteIdentifier("version") + " FROM " + dialect.quoteIdentifier(HISTORY_TABLE)
                + " WHERE " + dialect.quoteIdentifier("success") + " = 1";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                versions.add(rs.getInt(1));
            }
        }
        return versions;
    }

    private void recordMigrationSuccess(Connection conn, int rank, SchemaMigration migration, long executionTimeMs) throws SQLException {
        String sql = "INSERT INTO " + dialect.quoteIdentifier(HISTORY_TABLE) + " ("
                + dialect.quoteIdentifier("installed_rank") + ", "
                + dialect.quoteIdentifier("version") + ", "
                + dialect.quoteIdentifier("description") + ", "
                + dialect.quoteIdentifier("type") + ", "
                + dialect.quoteIdentifier("script") + ", "
                + dialect.quoteIdentifier("execution_time") + ", "
                + dialect.quoteIdentifier("success")
                + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rank);
            pstmt.setInt(2, migration.getVersion());
            pstmt.setString(3, migration.getDescription());
            pstmt.setString(4, "JDBC");
            pstmt.setString(5, migration.getClass().getSimpleName());
            pstmt.setLong(6, executionTimeMs);
            pstmt.setInt(7, 1);
            pstmt.executeUpdate();
        }
    }
}
