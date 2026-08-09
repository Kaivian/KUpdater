package io.github.kaivian.kupdater.api.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface representing a versioned database schema migration.
 */
public interface SchemaMigration {

    /**
     * Unique integer version number for this migration (e.g., 1 for V1, 2 for V2).
     *
     * @return version number
     */
    int getVersion();

    /**
     * Short description of what this migration accomplishes.
     *
     * @return description
     */
    String getDescription();

    /**
     * Executes the migration logic against the given database connection.
     * The connection will be managed inside an active transaction.
     *
     * @param connection active database connection
     * @throws SQLException if a database error occurs
     */
    void execute(Connection connection) throws SQLException;
}
