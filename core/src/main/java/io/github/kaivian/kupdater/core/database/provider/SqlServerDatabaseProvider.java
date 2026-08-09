package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.logging.Logger;

/**
 * Microsoft SQL Server database provider implementation.
 */
public class SqlServerDatabaseProvider extends AbstractHikariDatabaseProvider {

    public SqlServerDatabaseProvider(Logger logger) {
        super(logger);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQL_SERVER;
    }

    @Override
    protected HikariConfig createHikariConfig(DatabaseConfiguration configuration) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        int port = configuration.getPort() > 0 ? configuration.getPort() : 1433;
        String jdbcUrl = String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=%b;trustServerCertificate=%b;",
                configuration.getHost(),
                port,
                configuration.getDatabase(),
                configuration.isEncrypt(),
                configuration.isTrustServerCertificate()
        );

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());

        config.setMaximumPoolSize(configuration.getMaximumPoolSize());
        config.setMinimumIdle(configuration.getMinimumIdle());
        config.setConnectionTimeout(configuration.getConnectionTimeout());
        config.setIdleTimeout(configuration.getIdleTimeout());
        config.setMaxLifetime(configuration.getMaxLifetime());
        config.setPoolName("KUpdater-SQLServer-Pool");

        return config;
    }
}
