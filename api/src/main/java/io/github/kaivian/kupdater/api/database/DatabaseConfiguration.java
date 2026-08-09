package io.github.kaivian.kupdater.api.database;

import java.util.Objects;

/**
 * Immutable configuration holder for KUpdater persistence infrastructure.
 */
public class DatabaseConfiguration {

    private final boolean enabled;
    private final DatabaseType type;
    private final String sqliteFile;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final boolean encrypt;
    private final boolean trustServerCertificate;

    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeout;
    private final long idleTimeout;
    private final long maxLifetime;

    public DatabaseConfiguration(
            boolean enabled,
            DatabaseType type,
            String sqliteFile,
            String host,
            int port,
            String database,
            String username,
            String password,
            boolean ssl,
            boolean encrypt,
            boolean trustServerCertificate,
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeout,
            long idleTimeout,
            long maxLifetime
    ) {
        this.enabled = enabled;
        this.type = type;
        this.sqliteFile = sqliteFile;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.encrypt = encrypt;
        this.trustServerCertificate = trustServerCertificate;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DatabaseType getType() {
        return type;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isSsl() {
        return ssl;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public boolean isTrustServerCertificate() {
        return trustServerCertificate;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    /**
     * Builder for creating DatabaseConfiguration instances.
     */
    public static class Builder {
        private boolean enabled = false;
        private DatabaseType type = DatabaseType.SQLITE;
        private String sqliteFile = "data/kupdater.db";

        private String host = "localhost";
        private int port = 3306;
        private String database = "kupdater";
        private String username = "root";
        private String password = "";
        private boolean ssl = false;
        private boolean encrypt = false;
        private boolean trustServerCertificate = true;

        private int maximumPoolSize = 10;
        private int minimumIdle = 2;
        private long connectionTimeout = 10000L;
        private long idleTimeout = 600000L;
        private long maxLifetime = 1800000L;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder type(DatabaseType type) {
            this.type = type;
            return this;
        }

        public Builder sqliteFile(String sqliteFile) {
            this.sqliteFile = sqliteFile;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder encrypt(boolean encrypt) {
            this.encrypt = encrypt;
            return this;
        }

        public Builder trustServerCertificate(boolean trustServerCertificate) {
            this.trustServerCertificate = trustServerCertificate;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public Builder connectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder idleTimeout(long idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder maxLifetime(long maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public DatabaseConfiguration build() {
            return new DatabaseConfiguration(
                    enabled, type, sqliteFile, host, port, database, username, password,
                    ssl, encrypt, trustServerCertificate, maximumPoolSize, minimumIdle,
                    connectionTimeout, idleTimeout, maxLifetime
            );
        }
    }
}
