package io.github.kaivian.kupdater.core.database.dialect;

import io.github.kaivian.kupdater.api.database.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseDialectTest {

    @Test
    void testSqliteDialect() {
        DatabaseDialect dialect = new SqliteDialect();
        assertEquals(DatabaseType.SQLITE, dialect.getType());
        assertEquals("\"users\"", dialect.quoteIdentifier("users"));
        assertEquals("INTEGER PRIMARY KEY AUTOINCREMENT", dialect.getAutoIncrementPrimaryKeyType());
        assertEquals("INTEGER", dialect.getBooleanType());
        assertEquals("TIMESTAMP", dialect.getTimestampType());
        assertTrue(dialect.getCreateSchemaHistoryTableSql("history").contains("CREATE TABLE IF NOT EXISTS \"history\""));
        assertTrue(dialect.buildUpsertQuery("users", new String[]{"id"}, new String[]{"name"}).contains("ON CONFLICT"));
    }

    @Test
    void testMySqlDialect() {
        DatabaseDialect dialect = new MySqlDialect();
        assertEquals(DatabaseType.MYSQL, dialect.getType());
        assertEquals("`users`", dialect.quoteIdentifier("users"));
        assertEquals("INT AUTO_INCREMENT PRIMARY KEY", dialect.getAutoIncrementPrimaryKeyType());
        assertEquals("TINYINT(1)", dialect.getBooleanType());
        assertEquals("DATETIME", dialect.getTimestampType());
        assertTrue(dialect.getCreateSchemaHistoryTableSql("history").contains("CREATE TABLE IF NOT EXISTS `history`"));
        assertTrue(dialect.buildUpsertQuery("users", new String[]{"id"}, new String[]{"name"}).contains("ON DUPLICATE KEY UPDATE"));
    }

    @Test
    void testPostgresDialect() {
        DatabaseDialect dialect = new PostgresDialect();
        assertEquals(DatabaseType.POSTGRESQL, dialect.getType());
        assertEquals("\"users\"", dialect.quoteIdentifier("users"));
        assertEquals("SERIAL PRIMARY KEY", dialect.getAutoIncrementPrimaryKeyType());
        assertEquals("BOOLEAN", dialect.getBooleanType());
        assertEquals("TIMESTAMP WITH TIME ZONE", dialect.getTimestampType());
        assertTrue(dialect.getCreateSchemaHistoryTableSql("history").contains("CREATE TABLE IF NOT EXISTS \"history\""));
        assertTrue(dialect.buildUpsertQuery("users", new String[]{"id"}, new String[]{"name"}).contains("ON CONFLICT (\"id\") DO UPDATE SET"));
    }

    @Test
    void testMariaDbDialect() {
        DatabaseDialect dialect = new MariaDbDialect();
        assertEquals(DatabaseType.MARIADB, dialect.getType());
        assertEquals("`users`", dialect.quoteIdentifier("users"));
        assertEquals("INT AUTO_INCREMENT PRIMARY KEY", dialect.getAutoIncrementPrimaryKeyType());
        assertEquals("TINYINT(1)", dialect.getBooleanType());
        assertEquals("DATETIME", dialect.getTimestampType());
        assertTrue(dialect.getCreateSchemaHistoryTableSql("history").contains("CREATE TABLE IF NOT EXISTS `history`"));
        assertTrue(dialect.buildUpsertQuery("users", new String[]{"id"}, new String[]{"name"}).contains("ON DUPLICATE KEY UPDATE"));
    }

    @Test
    void testSqlServerDialect() {
        DatabaseDialect dialect = new SqlServerDialect();
        assertEquals(DatabaseType.SQL_SERVER, dialect.getType());
        assertEquals("[users]", dialect.quoteIdentifier("users"));
        assertEquals("INT IDENTITY(1,1) PRIMARY KEY", dialect.getAutoIncrementPrimaryKeyType());
        assertEquals("BIT", dialect.getBooleanType());
        assertEquals("DATETIME2", dialect.getTimestampType());
        assertTrue(dialect.getCreateSchemaHistoryTableSql("history").contains("IF NOT EXISTS"));
        assertTrue(dialect.buildUpsertQuery("users", new String[]{"id"}, new String[]{"name"}).contains("MERGE INTO [users]"));
    }
}
