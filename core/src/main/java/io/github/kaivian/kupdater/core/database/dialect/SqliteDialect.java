package io.github.kaivian.kupdater.core.database.dialect;

import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.StringJoiner;

/**
 * SQLite dialect implementation.
 */
public class SqliteDialect implements DatabaseDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String getAutoIncrementPrimaryKeyType() {
        return "INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    @Override
    public String getBooleanType() {
        return "INTEGER";
    }

    @Override
    public String getTimestampType() {
        return "TIMESTAMP";
    }

    @Override
    public String getCreateSchemaHistoryTableSql(String tableName) {
        String quotedTable = quoteIdentifier(tableName);
        return "CREATE TABLE IF NOT EXISTS " + quotedTable + " ("
                + quoteIdentifier("installed_rank") + " INT NOT NULL PRIMARY KEY, "
                + quoteIdentifier("version") + " INT NOT NULL, "
                + quoteIdentifier("description") + " VARCHAR(255) NOT NULL, "
                + quoteIdentifier("type") + " VARCHAR(20) NOT NULL, "
                + quoteIdentifier("script") + " VARCHAR(255) NOT NULL, "
                + quoteIdentifier("installed_on") + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, "
                + quoteIdentifier("execution_time") + " INT NOT NULL, "
                + quoteIdentifier("success") + " INTEGER NOT NULL"
                + ");";
    }

    @Override
    public String buildUpsertQuery(String tableName, String[] primaryKeyColumns, String[] updateColumns) {
        String quotedTable = quoteIdentifier(tableName);
        StringJoiner pkJoiner = new StringJoiner(", ");
        for (String pk : primaryKeyColumns) {
            pkJoiner.add(quoteIdentifier(pk));
        }

        StringJoiner updateJoiner = new StringJoiner(", ");
        for (String col : updateColumns) {
            String q = quoteIdentifier(col);
            updateJoiner.add(q + " = excluded." + q);
        }

        return "INSERT INTO " + quotedTable + " ON CONFLICT(" + pkJoiner + ") DO UPDATE SET " + updateJoiner;
    }
}
