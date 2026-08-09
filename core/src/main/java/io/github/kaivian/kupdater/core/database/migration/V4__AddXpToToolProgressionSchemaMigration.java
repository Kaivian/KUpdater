package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Migration V4: Add xp column to kupdater_tool_progression table.
 */
public class V4__AddXpToToolProgressionSchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 4;
    }

    @Override
    public String getDescription() {
        return "Add XP Column to Tool Progression Schema";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        String sql = "ALTER TABLE kupdater_tool_progression ADD COLUMN xp INT DEFAULT 0";

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
