package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration V3: Add material column to kupdater_tool_progression table.
 */
public class V3__AddMaterialToToolProgressionSchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String getDescription() {
        return "Add Material Tier Column to Tool Progression Schema";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        String sql = "ALTER TABLE kupdater_tool_progression ADD COLUMN material VARCHAR(32) DEFAULT 'WOODEN'";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // Column may already exist on some SQLite/MySQL instances if recreated
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (!msg.contains("duplicate") && !msg.contains("already exists")) {
                throw e;
            }
        }
    }
}
