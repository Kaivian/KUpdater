package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.logging.Logger;

/**
 * PostgreSQL database provider implementation.
 */
public class PostgresDatabaseProvider extends AbstractHikariDatabaseProvider {

    public PostgresDatabaseProvider(Logger logger) {
        super(logger);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    protected HikariConfig createHikariConfig(DatabaseConfiguration configuration) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        int port = configuration.getPort() > 0 ? configuration.getPort() : 5432;
        String sslMode = configuration.isSsl() ? "require" : "disable";
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=%s",
                configuration.getHost(),
                port,
                configuration.getDatabase(),
                sslMode
        );

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());

        config.setMaximumPoolSize(configuration.getMaximumPoolSize());
        config.setMinimumIdle(configuration.getMinimumIdle());
        config.setConnectionTimeout(configuration.getConnectionTimeout());
        config.setIdleTimeout(configuration.getIdleTimeout());
        config.setMaxLifetime(configuration.getMaxLifetime());
        config.setPoolName("KUpdater-PostgreSQL-Pool");

        return config;
    }
}
