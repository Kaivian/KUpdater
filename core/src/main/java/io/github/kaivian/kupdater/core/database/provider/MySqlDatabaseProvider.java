package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.logging.Logger;

/**
 * MySQL database provider implementation.
 */
public class MySqlDatabaseProvider extends AbstractHikariDatabaseProvider {

    public MySqlDatabaseProvider(Logger logger) {
        super(logger);
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    protected HikariConfig createHikariConfig(DatabaseConfiguration configuration) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        int port = configuration.getPort() > 0 ? configuration.getPort() : 3306;
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
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
        config.setPoolName("KUpdater-MySQL-Pool");

        return config;
    }
}
