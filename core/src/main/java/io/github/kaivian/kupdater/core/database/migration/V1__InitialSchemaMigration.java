package io.github.kaivian.kupdater.core.database.migration;

import io.github.kaivian.kupdater.api.database.SchemaMigration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Baseline schema migration V1 for initializing KUpdater persistence infrastructure.
 */
public class V1__InitialSchemaMigration implements SchemaMigration {

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public String getDescription() {
        return "Initialize Persistence Infrastructure Baseline";
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        // Infrastructure baseline initialization. No gameplay tables added yet.
    }
}
