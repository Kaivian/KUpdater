package io.github.kaivian.kupdater.api.database;

import java.util.Locale;

/**
 * Enumeration of database engines supported by KUpdater.
 */
public enum DatabaseType {
    SQLITE("SQLite"),
    MYSQL("MySQL"),
    POSTGRESQL("PostgreSQL"),
    MARIADB("MariaDB"),
    SQL_SERVER("Microsoft SQL Server");

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets human-readable display name of the database engine.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string representation of database type.
     *
     * @param typeString string input (e.g. "sqlite", "mysql", "postgresql", "mariadb", "sql-server", "sqlserver")
     * @return matching DatabaseType or null if unsupported
     */
    public static DatabaseType fromString(String typeString) {
        if (typeString == null || typeString.trim().isEmpty()) {
            return null;
        }

        String normalized = typeString.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        switch (normalized) {
            case "sqlite":
                return SQLITE;
            case "mysql":
                return MYSQL;
            case "postgres":
            case "postgresql":
                return POSTGRESQL;
            case "mariadb":
                return MARIADB;
            case "sqlserver":
            case "mssql":
                return SQL_SERVER;
            default:
                return null;
        }
    }
}
