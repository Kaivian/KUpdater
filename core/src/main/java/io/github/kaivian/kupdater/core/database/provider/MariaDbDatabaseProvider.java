package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.logging.Logger;

/**
 * MariaDB database provider implementation.
 */
public class MariaDbDatabaseProvider extends AbstractHikariDatabaseProvider {

    public MariaDbDatabaseProvider(Logger logger) {
        super(logger);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MARIADB;
    }

    @Override
    protected HikariConfig createHikariConfig(DatabaseConfiguration configuration) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        int port = configuration.getPort() > 0 ? configuration.getPort() : 3306;
        String jdbcUrl = String.format(
                "jdbc:mariadb://%s:%d/%s?useSSL=%b",
                configuration.getHost(),
                port,
                configuration.getDatabase(),
                configuration.isSsl()
        );

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());

        config.setMaximumPoolSize(configuration.getMaximumPoolSize());
        config.setMinimumIdle(configuration.getMinimumIdle());
        config.setConnectionTimeout(configuration.getConnectionTimeout());
        config.setIdleTimeout(configuration.getIdleTimeout());
        config.setMaxLifetime(configuration.getMaxLifetime());
        config.setPoolName("KUpdater-MariaDB-Pool");

        return config;
    }
}
