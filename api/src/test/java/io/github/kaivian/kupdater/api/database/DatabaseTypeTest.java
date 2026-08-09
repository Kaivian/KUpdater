package io.github.kaivian.kupdater.api.database;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class DatabaseTypeTest {

    @Test
    void testFromStringValidTypes() {
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromString("sqlite"));
        assertEquals(DatabaseType.SQLITE, DatabaseType.fromString("SQLite"));
        
        assertEquals(DatabaseType.MYSQL, DatabaseType.fromString("mysql"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.fromString("MySQL"));

        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("postgresql"));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("postgres"));

        assertEquals(DatabaseType.MARIADB, DatabaseType.fromString("mariadb"));
        assertEquals(DatabaseType.MARIADB, DatabaseType.fromString("MariaDB"));

        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.fromString("sql-server"));
        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.fromString("sqlserver"));
        assertEquals(DatabaseType.SQL_SERVER, DatabaseType.fromString("mssql"));
    }

    @Test
    void testFromStringInvalidTypes() {
        assertNull(DatabaseType.fromString("oracle"));
        assertNull(DatabaseType.fromString("invalid"));
        assertNull(DatabaseType.fromString(""));
        assertNull(DatabaseType.fromString(null));
    }

    @Test
    void testDisplayNames() {
        assertEquals("SQLite", DatabaseType.SQLITE.getDisplayName());
        assertEquals("MySQL", DatabaseType.MYSQL.getDisplayName());
        assertEquals("PostgreSQL", DatabaseType.POSTGRESQL.getDisplayName());
        assertEquals("MariaDB", DatabaseType.MARIADB.getDisplayName());
        assertEquals("Microsoft SQL Server", DatabaseType.SQL_SERVER.getDisplayName());
    }
}
