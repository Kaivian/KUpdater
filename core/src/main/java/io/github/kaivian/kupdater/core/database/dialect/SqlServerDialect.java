package io.github.kaivian.kupdater.core.database.dialect;

import io.github.kaivian.kupdater.api.database.DatabaseType;

import java.util.StringJoiner;

/**
 * Microsoft SQL Server dialect implementation.
 */
public class SqlServerDialect implements DatabaseDialect {

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQL_SERVER;
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "[" + identifier.replace("]", "]]") + "]";
    }

    @Override
    public String getAutoIncrementPrimaryKeyType() {
        return "INT IDENTITY(1,1) PRIMARY KEY";
    }

    @Override
    public String getBooleanType() {
        return "BIT";
    }

    @Override
    public String getTimestampType() {
        return "DATETIME2";
    }

    @Override
    public String getCreateSchemaHistoryTableSql(String tableName) {
        String quotedTable = quoteIdentifier(tableName);
        return "IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'" + tableName + "') AND type in (N'U')) "
                + "CREATE TABLE " + quotedTable + " ("
                + quoteIdentifier("installed_rank") + " INT NOT NULL PRIMARY KEY, "
                + quoteIdentifier("version") + " INT NOT NULL, "
                + quoteIdentifier("description") + " NVARCHAR(255) NOT NULL, "
                + quoteIdentifier("type") + " NVARCHAR(20) NOT NULL, "
                + quoteIdentifier("script") + " NVARCHAR(255) NOT NULL, "
                + quoteIdentifier("installed_on") + " DATETIME2 DEFAULT CURRENT_TIMESTAMP NOT NULL, "
                + quoteIdentifier("execution_time") + " INT NOT NULL, "
                + quoteIdentifier("success") + " BIT NOT NULL"
                + ");";
    }

    @Override
    public String buildUpsertQuery(String tableName, String[] primaryKeyColumns, String[] updateColumns) {
        String quotedTable = quoteIdentifier(tableName);
        StringJoiner matchJoiner = new StringJoiner(" AND ");
        for (String pk : primaryKeyColumns) {
            String q = quoteIdentifier(pk);
            matchJoiner.add("target." + q + " = source." + q);
        }

        StringJoiner updateJoiner = new StringJoiner(", ");
        for (String col : updateColumns) {
            String q = quoteIdentifier(col);
            updateJoiner.add("target." + q + " = source." + q);
        }

        return "MERGE INTO " + quotedTable + " WITH (HOLDLOCK) AS target USING ("
                + "VALUES (...) ) AS source (...) ON " + matchJoiner
                + " WHEN MATCHED THEN UPDATE SET " + updateJoiner
                + " WHEN NOT MATCHED THEN INSERT (...) VALUES (...);";
    }
}
