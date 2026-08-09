package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Base HikariCP-backed database provider implementation.
 */
public abstract class AbstractHikariDatabaseProvider implements DatabaseProvider {

    protected final Logger logger;
    protected HikariDataSource dataSource;
    protected volatile boolean available = false;

    protected AbstractHikariDatabaseProvider(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void initialize(DatabaseConfiguration configuration) throws Exception {
        HikariConfig hikariConfig = createHikariConfig(configuration);
        this.dataSource = new HikariDataSource(hikariConfig);
        
        // Test connectivity
        try (Connection conn = dataSource.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                this.available = true;
            }
        } catch (SQLException e) {
            shutdown();
            throw e;
        }
    }

    /**
     * Constructs the database-specific HikariConfig instance.
     *
     * @param configuration database configuration
     * @return populated HikariConfig
     * @throws Exception if configuration or driver registration fails
     */
    protected abstract HikariConfig createHikariConfig(DatabaseConfiguration configuration) throws Exception;

    @Override
    public boolean isAvailable() {
        return available && dataSource != null && !dataSource.isClosed();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!isAvailable()) {
            throw new SQLException("Database provider for " + getType() + " is not available.");
        }
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        this.available = false;
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }
}
