package io.github.kaivian.kupdater.core.database.provider;

import com.zaxxer.hikari.HikariConfig;
import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * SQLite specialized database provider.
 */
public class SqliteDatabaseProvider extends AbstractHikariDatabaseProvider {

    private final File dataFolder;

    public SqliteDatabaseProvider(Logger logger, File dataFolder) {
        super(logger);
        this.dataFolder = dataFolder;
    }

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    @Override
    protected HikariConfig createHikariConfig(DatabaseConfiguration configuration) throws Exception {
        String filePath = configuration.getSqliteFile();
        if (filePath == null || filePath.trim().isEmpty()) {
            filePath = "data/kupdater.db";
        }

        File dbFile = new File(filePath);
        if (!dbFile.isAbsolute()) {
            dbFile = new File(dataFolder, filePath);
        }

        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // SQLite connection pool optimizations
        config.setMaximumPoolSize(Math.max(1, configuration.getMaximumPoolSize()));
        config.setMinimumIdle(Math.max(1, configuration.getMinimumIdle()));
        config.setConnectionTimeout(configuration.getConnectionTimeout());
        config.setIdleTimeout(configuration.getIdleTimeout());
        config.setMaxLifetime(configuration.getMaxLifetime());
        config.setPoolName("KUpdater-SQLite-Pool");

        // Enable WAL mode and foreign keys via connection init sql
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;");

        return config;
    }
}
