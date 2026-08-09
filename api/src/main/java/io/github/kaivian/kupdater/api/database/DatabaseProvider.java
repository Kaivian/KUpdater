package io.github.kaivian.kupdater.api.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Common abstraction for database engine providers in KUpdater.
 */
public interface DatabaseProvider {

    /**
     * Gets the database type implemented by this provider.
     *
     * @return DatabaseType
     */
    DatabaseType getType();

    /**
     * Initializes the provider with configuration settings.
     *
     * @param configuration database configuration
     * @throws Exception if connection pool setup or driver loading fails
     */
    void initialize(DatabaseConfiguration configuration) throws Exception;

    /**
     * Checks if the provider is active and ready to supply connections.
     *
     * @return true if available, false otherwise
     */
    boolean isAvailable();

    /**
     * Gets the underlying HikariCP DataSource.
     *
     * @return DataSource instance
     */
    DataSource getDataSource();

    /**
     * Obtains a connection from the connection pool.
     *
     * @return active JDBC Connection
     * @throws SQLException if a database access error occurs
     */
    Connection getConnection() throws SQLException;

    /**
     * Closes resources and shuts down the connection pool safely.
     */
    void shutdown();
}
