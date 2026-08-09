package io.github.kaivian.kupdater.api.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigurationTest {

    @Test
    void testDefaultBuilderValues() {
        DatabaseConfiguration config = new DatabaseConfiguration.Builder().build();

        assertFalse(config.isEnabled());
        assertEquals(DatabaseType.SQLITE, config.getType());
        assertEquals("data/kupdater.db", config.getSqliteFile());
        assertEquals("localhost", config.getHost());
        assertEquals(3306, config.getPort());
        assertEquals("kupdater", config.getDatabase());
        assertEquals("root", config.getUsername());
        assertEquals("", config.getPassword());
        assertFalse(config.isSsl());
        assertEquals(10, config.getMaximumPoolSize());
        assertEquals(2, config.getMinimumIdle());
    }

    @Test
    void testCustomBuilderValues() {
        DatabaseConfiguration config = new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(DatabaseType.POSTGRESQL)
                .host("db.example.com")
                .port(5432)
                .database("test_db")
                .username("postgres_user")
                .password("secret")
                .ssl(true)
                .maximumPoolSize(20)
                .minimumIdle(5)
                .connectionTimeout(5000L)
                .idleTimeout(300000L)
                .maxLifetime(900000L)
                .build();

        assertTrue(config.isEnabled());
        assertEquals(DatabaseType.POSTGRESQL, config.getType());
        assertEquals("db.example.com", config.getHost());
        assertEquals(5432, config.getPort());
        assertEquals("test_db", config.getDatabase());
        assertEquals("postgres_user", config.getUsername());
        assertEquals("secret", config.getPassword());
        assertTrue(config.isSsl());
        assertEquals(20, config.getMaximumPoolSize());
        assertEquals(5, config.getMinimumIdle());
        assertEquals(5000L, config.getConnectionTimeout());
        assertEquals(300000L, config.getIdleTimeout());
        assertEquals(900000L, config.getMaxLifetime());
    }
}
