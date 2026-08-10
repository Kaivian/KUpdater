package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration V6 for creating tool recovery state and audit history schema tables.
 */
public class V6__CreateToolRecoverySchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 6;
    }

    @Override
    public String getDescription() {
        return "Create Tool Recovery State and History Schema";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String dbProduct = metaData.getDatabaseProductName().toLowerCase();

        String pkType = "INTEGER PRIMARY KEY AUTOINCREMENT";
        if (dbProduct.contains("postgres")) {
            pkType = "BIGSERIAL PRIMARY KEY";
        } else if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
            pkType = "BIGINT AUTO_INCREMENT PRIMARY KEY";
        } else if (dbProduct.contains("microsoft") || dbProduct.contains("sql server")) {
            pkType = "BIGINT IDENTITY(1,1) PRIMARY KEY";
        }

        String stateTableSql = "CREATE TABLE IF NOT EXISTS kupdater_tool_recovery_state ("
                + "tool_uuid VARCHAR(36) NOT NULL PRIMARY KEY, "
                + "owner_uuid VARCHAR(36) NOT NULL, "
                + "recovery_count INT NOT NULL DEFAULT 0, "
                + "last_recovery_at TIMESTAMP NULL, "
                + "next_recovery_at TIMESTAMP NULL, "
                + "created_at TIMESTAMP NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL"
                + ")";

        String historyTableSql = "CREATE TABLE IF NOT EXISTS kupdater_tool_recovery_history ("
                + "id " + pkType + ", "
                + "tool_uuid VARCHAR(36) NOT NULL, "
                + "owner_uuid VARCHAR(36) NOT NULL, "
                + "recovery_count INT NOT NULL, "
                + "penalty_level INT NOT NULL, "
                + "currency_cost DOUBLE NOT NULL DEFAULT 0.0, "
                + "items_consumed TEXT NOT NULL, "
                + "durability_penalty_percent DOUBLE NOT NULL DEFAULT 0.0, "
                + "recovered_at TIMESTAMP NOT NULL"
                + ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(stateTableSql);
            stmt.execute(historyTableSql);

            try {
                stmt.execute("CREATE INDEX idx_recovery_state_owner ON kupdater_tool_recovery_state(owner_uuid)");
            } catch (SQLException ignored) {
            }

            try {
                stmt.execute("CREATE INDEX idx_recovery_history_tool ON kupdater_tool_recovery_history(tool_uuid)");
            } catch (SQLException ignored) {
            }

            try {
                stmt.execute("CREATE INDEX idx_recovery_history_owner ON kupdater_tool_recovery_history(owner_uuid)");
            } catch (SQLException ignored) {
            }
        }
    }
}
