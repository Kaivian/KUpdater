package io.github.kaivian.kupdater.core.database;

import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseProvider;
import io.github.kaivian.kupdater.api.database.DatabaseType;
import io.github.kaivian.kupdater.api.database.PersistenceExecutor;
import io.github.kaivian.kupdater.api.database.PersistenceState;

import io.github.kaivian.kupdater.core.database.dialect.DatabaseDialect;
import io.github.kaivian.kupdater.core.database.dialect.MariaDbDialect;
import io.github.kaivian.kupdater.core.database.dialect.MySqlDialect;
import io.github.kaivian.kupdater.core.database.dialect.PostgresDialect;
import io.github.kaivian.kupdater.core.database.dialect.SqlServerDialect;
import io.github.kaivian.kupdater.core.database.dialect.SqliteDialect;

import io.github.kaivian.kupdater.core.database.execution.AsyncPersistenceExecutor;
import io.github.kaivian.kupdater.core.database.migration.MigrationRunner;
import io.github.kaivian.kupdater.core.database.migration.V1__InitialSchemaMigration;

import io.github.kaivian.kupdater.core.database.provider.MariaDbDatabaseProvider;
import io.github.kaivian.kupdater.core.database.provider.MySqlDatabaseProvider;
import io.github.kaivian.kupdater.core.database.provider.PostgresDatabaseProvider;
import io.github.kaivian.kupdater.core.database.provider.SqlServerDatabaseProvider;
import io.github.kaivian.kupdater.core.database.provider.SqliteDatabaseProvider;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central lifecycle manager for KUpdater persistence infrastructure.
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File dataFolder;

    private volatile PersistenceState state = PersistenceState.DISABLED;
    private DatabaseConfiguration configuration;
    private DatabaseProvider provider;
    private DatabaseDialect dialect;
    private PersistenceExecutor executor;
    private MigrationRunner migrationRunner;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger("DatabaseManager");
        this.dataFolder = plugin != null ? plugin.getDataFolder() : new File("build/tmp/test");
    }


    /**
     * Initializes persistence infrastructure based on plugin configuration.
     */
    public void setup() {
        logger.info("[KUpdater] Initializing persistence layer...");
        this.state = PersistenceState.INITIALIZING;

        FileConfiguration fileConfig = plugin.getConfig();
        boolean enabled = fileConfig.getBoolean("database.enabled", false);

        if (!enabled) {
            this.state = PersistenceState.DISABLED;
            logger.info("[KUpdater] Database persistence is disabled in configuration.");
            logger.info("[KUpdater] Skipping database initialization.");
            return;
        }

        try {
            // Load and parse configuration
            this.configuration = parseConfiguration(fileConfig);
            logger.info("[KUpdater] Database type: " + configuration.getType().getDisplayName());

            // Validate configuration for selected engine
            validateConfiguration(configuration);
            logger.info("[KUpdater] Validating database configuration...");
            logger.info("[KUpdater] Database configuration validated.");

            // Create dialect and provider
            this.dialect = createDialect(configuration.getType());
            this.provider = createProvider(configuration.getType());

            // Log non-sensitive metadata
            logSafeMetadata(configuration);

            // Initialize connection pool
            logger.info("[KUpdater] Initializing connection pool...");
            logger.info("[KUpdater] Testing database connection...");
            provider.initialize(configuration);
            logger.info("[KUpdater] Database connection established.");

            // Initialize async executor pool
            this.executor = new AsyncPersistenceExecutor(logger, configuration.getMaximumPoolSize());

            // Initialize and execute schema migrations
            logger.info("[KUpdater] Running database migrations...");
            this.migrationRunner = new MigrationRunner(provider, dialect, logger);
            this.migrationRunner.registerMigration(new V1__InitialSchemaMigration());
            this.migrationRunner.runMigrations();
            logger.info("[KUpdater] Database migrations completed.");

            this.state = PersistenceState.AVAILABLE;
            logger.info("[KUpdater] Persistence layer initialized successfully.");
        } catch (Exception e) {
            this.state = PersistenceState.FAILED;
            logger.log(Level.SEVERE, "[KUpdater] Failed to initialize persistence layer.", e);
            shutdownInternal();
        }
    }

    private DatabaseConfiguration parseConfiguration(FileConfiguration config) {
        String typeStr = config.getString("database.type", "sqlite");
        DatabaseType type = DatabaseType.fromString(typeStr);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported database type in configuration: '" + typeStr + "'");
        }

        String sqliteFile = config.getString("database.sqlite.file", "data/kupdater.db");

        String sectionKey = getSectionKeyForType(type);
        String host = config.getString("database." + sectionKey + ".host", "localhost");
        int port = config.getInt("database." + sectionKey + ".port", getDefaultPort(type));
        String database = config.getString("database." + sectionKey + ".database", "kupdater");
        String username = config.getString("database." + sectionKey + ".username", "root");
        String password = config.getString("database." + sectionKey + ".password", "");
        boolean ssl = config.getBoolean("database." + sectionKey + ".ssl", false);
        boolean encrypt = config.getBoolean("database." + sectionKey + ".encrypt", false);
        boolean trustCert = config.getBoolean("database." + sectionKey + ".trust-server-certificate", true);

        int maxPool = config.getInt("database.pool.maximum-size", 10);
        int minIdle = config.getInt("database.pool.minimum-idle", 2);
        long connTimeout = config.getLong("database.pool.connection-timeout", 10000L);
        long idleTimeout = config.getLong("database.pool.idle-timeout", 600000L);
        long maxLifetime = config.getLong("database.pool.max-lifetime", 1800000L);

        return new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(type)
                .sqliteFile(sqliteFile)
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .ssl(ssl)
                .encrypt(encrypt)
                .trustServerCertificate(trustCert)
                .maximumPoolSize(maxPool)
                .minimumIdle(minIdle)
                .connectionTimeout(connTimeout)
                .idleTimeout(idleTimeout)
                .maxLifetime(maxLifetime)
                .build();
    }

    private void validateConfiguration(DatabaseConfiguration config) {
        if (config.getType() == DatabaseType.SQLITE) {
            if (config.getSqliteFile() == null || config.getSqliteFile().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid SQLite configuration: database.sqlite.file path cannot be empty.");
            }
        } else {
            if (config.getHost() == null || config.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + config.getType().getDisplayName() + " configuration: host cannot be empty.");
            }
            if (config.getPort() <= 0 || config.getPort() > 65535) {
                throw new IllegalArgumentException("Invalid " + config.getType().getDisplayName() + " configuration: port must be between 1 and 65535.");
            }
            if (config.getDatabase() == null || config.getDatabase().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + config.getType().getDisplayName() + " configuration: database name cannot be empty.");
            }
            if (config.getUsername() == null || config.getUsername().trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + config.getType().getDisplayName() + " configuration: username cannot be empty.");
            }
        }
    }

    private DatabaseDialect createDialect(DatabaseType type) {
        switch (type) {
            case SQLITE:
                return new SqliteDialect();
            case MYSQL:
                return new MySqlDialect();
            case POSTGRESQL:
                return new PostgresDialect();
            case MARIADB:
                return new MariaDbDialect();
            case SQL_SERVER:
                return new SqlServerDialect();
            default:
                throw new IllegalArgumentException("Unsupported dialect type: " + type);
        }
    }

    private DatabaseProvider createProvider(DatabaseType type) {
        switch (type) {
            case SQLITE:
                return new SqliteDatabaseProvider(logger, dataFolder);
            case MYSQL:
                return new MySqlDatabaseProvider(logger);
            case POSTGRESQL:
                return new PostgresDatabaseProvider(logger);
            case MARIADB:
                return new MariaDbDatabaseProvider(logger);
            case SQL_SERVER:
                return new SqlServerDatabaseProvider(logger);
            default:
                throw new IllegalArgumentException("Unsupported provider type: " + type);
        }
    }

    private String getSectionKeyForType(DatabaseType type) {
        switch (type) {
            case MYSQL:
                return "mysql";
            case POSTGRESQL:
                return "postgresql";
            case MARIADB:
                return "mariadb";
            case SQL_SERVER:
                return "sql-server";
            default:
                return "sqlite";
        }
    }

    private int getDefaultPort(DatabaseType type) {
        switch (type) {
            case MYSQL:
            case MARIADB:
                return 3306;
            case POSTGRESQL:
                return 5432;
            case SQL_SERVER:
                return 1433;
            default:
                return 0;
        }
    }

    private void logSafeMetadata(DatabaseConfiguration config) {
        if (config.getType() == DatabaseType.SQLITE) {
            logger.info("[KUpdater] SQLite File: " + config.getSqliteFile());
        } else {
            logger.info("[KUpdater] Host: " + config.getHost());
            logger.info("[KUpdater] Port: " + config.getPort());
            logger.info("[KUpdater] Database: " + config.getDatabase());
            logger.info("[KUpdater] Username: " + config.getUsername());
        }
    }

    /**
     * Shuts down persistence connection pool and async executor cleanly.
     */
    public void shutdown() {
        logger.info("[KUpdater] Closing persistence layer...");
        shutdownInternal();
        this.state = PersistenceState.SHUTDOWN;
        logger.info("[KUpdater] Persistence layer closed.");
    }

    private void shutdownInternal() {
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (Exception e) {
                logger.log(Level.WARNING, "[KUpdater] Error closing database async executor", e);
            }
            executor = null;
        }

        if (provider != null) {
            try {
                provider.shutdown();
            } catch (Exception e) {
                logger.log(Level.WARNING, "[KUpdater] Error shutting down database provider", e);
            }
            provider = null;
        }
    }

    public PersistenceState getState() {
        return state;
    }

    public boolean isAvailable() {
        return state == PersistenceState.AVAILABLE && provider != null && provider.isAvailable();
    }

    public DatabaseConfiguration getConfiguration() {
        return configuration;
    }

    public DatabaseProvider getProvider() {
        return provider;
    }

    public DatabaseDialect getDialect() {
        return dialect;
    }

    public PersistenceExecutor getExecutor() {
        return executor;
    }
}
