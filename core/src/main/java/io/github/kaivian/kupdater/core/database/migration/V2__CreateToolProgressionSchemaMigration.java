package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration V2 for initializing tool progression persistence schema.
 */
public class V2__CreateToolProgressionSchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public String getDescription() {
        return "Create Tool Progression Schema";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String dbProduct = metaData.getDatabaseProductName().toLowerCase();

        String pkType = "INTEGER PRIMARY KEY AUTOINCREMENT";
        if (dbProduct.contains("postgres")) {
            pkType = "SERIAL PRIMARY KEY";
        } else if (dbProduct.contains("mysql") || dbProduct.contains("mariadb")) {
            pkType = "INT AUTO_INCREMENT PRIMARY KEY";
        } else if (dbProduct.contains("microsoft") || dbProduct.contains("sql server")) {
            pkType = "INT IDENTITY(1,1) PRIMARY KEY";
        }

        String createTableSql = "CREATE TABLE IF NOT EXISTS kupdater_tool_progression ("
                + "id " + pkType + ", "
                + "tool_uuid VARCHAR(36) NOT NULL, "
                + "owner_uuid VARCHAR(36) NOT NULL, "
                + "tool_type VARCHAR(32) NOT NULL, "
                + "level INT NOT NULL DEFAULT 1, "
                + "state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', "
                + "current_durability INT NOT NULL DEFAULT 0, "
                + "schema_version INT NOT NULL DEFAULT 1, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "CONSTRAINT unique_owner_tool_type UNIQUE (owner_uuid, tool_type)"
                + ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);

            try {
                stmt.execute("CREATE INDEX idx_kupdater_tool_uuid ON kupdater_tool_progression(tool_uuid)");
            } catch (SQLException ignored) {
                // Index might already exist on some database engines
            }

            try {
                stmt.execute("CREATE INDEX idx_kupdater_owner_uuid ON kupdater_tool_progression(owner_uuid)");
            } catch (SQLException ignored) {
                // Index might already exist on some database engines
            }
        }
    }
}
