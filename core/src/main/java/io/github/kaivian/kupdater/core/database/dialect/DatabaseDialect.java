package io.github.kaivian.kupdater.core.database.dialect;

import io.github.kaivian.kupdater.api.database.DatabaseType;

/**
 * Abstraction for database dialect specific SQL syntax, type mappings, identifier quoting, and upsert handling.
 */
public interface DatabaseDialect {

    /**
     * Gets the database type corresponding to this dialect.
     *
     * @return DatabaseType
     */
    DatabaseType getType();

    /**
     * Quotes an identifier (table or column name) according to dialect rules.
     *
     * @param identifier identifier name
     * @return quoted identifier
     */
    String quoteIdentifier(String identifier);

    /**
     * SQL data type for an auto-incrementing integer primary key.
     *
     * @return auto-increment primary key data type clause
     */
    String getAutoIncrementPrimaryKeyType();

    /**
     * SQL data type for storing boolean values.
     *
     * @return boolean SQL type name
     */
    String getBooleanType();

    /**
     * SQL data type for storing timestamps.
     *
     * @return timestamp SQL type name
     */
    String getTimestampType();

    /**
     * SQL statement to create the schema history tracking table if it does not exist.
     *
     * @param tableName name of the history table (e.g. kupdater_schema_history)
     * @return DDL statement
     */
    String getCreateSchemaHistoryTableSql(String tableName);

    /**
     * Builds an upsert (merge / insert or update) SQL query for the target dialect.
     *
     * @param tableName          target table
     * @param primaryKeyColumns  primary key column names
     * @param updateColumns      columns to update on conflict
     * @return formatted SQL statement string template
     */
    String buildUpsertQuery(String tableName, String[] primaryKeyColumns, String[] updateColumns);
}
