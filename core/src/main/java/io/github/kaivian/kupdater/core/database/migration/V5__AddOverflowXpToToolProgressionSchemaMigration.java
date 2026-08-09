package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration V5: Add overflow_xp column to kupdater_tool_progression table.
 */
public class V5__AddOverflowXpToToolProgressionSchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public String getDescription() {
        return "Add Overflow XP Column to Tool Progression Schema";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        String sql = "ALTER TABLE kupdater_tool_progression ADD COLUMN overflow_xp INT DEFAULT 0";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (!msg.contains("duplicate") && !msg.contains("already exists")) {
                throw e;
            }
        }
    }
}
