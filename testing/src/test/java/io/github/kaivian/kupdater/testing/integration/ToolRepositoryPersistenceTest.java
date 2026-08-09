package io.github.kaivian.kupdater.testing.integration;

import io.github.kaivian.kupdater.api.database.DatabaseConfiguration;
import io.github.kaivian.kupdater.api.database.DatabaseType;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.core.database.dialect.SqliteDialect;
import io.github.kaivian.kupdater.core.database.execution.AsyncPersistenceExecutor;
import io.github.kaivian.kupdater.core.database.migration.MigrationRunner;
import io.github.kaivian.kupdater.core.database.migration.V1__InitialSchemaMigration;
import io.github.kaivian.kupdater.core.database.migration.V2__CreateToolProgressionSchemaMigration;
import io.github.kaivian.kupdater.core.database.migration.V3__AddMaterialToToolProgressionSchemaMigration;
import io.github.kaivian.kupdater.core.database.provider.SqliteDatabaseProvider;
import io.github.kaivian.kupdater.features.tools.common.repository.JdbcToolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ToolRepositoryPersistenceTest {

    private SqliteDatabaseProvider provider;
    private AsyncPersistenceExecutor executor;
    private JdbcToolRepository repository;
    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("kupdater_tools_repo_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        Logger logger = Logger.getLogger("ToolRepoTest");
        provider = new SqliteDatabaseProvider(logger, tempDir);
        DatabaseConfiguration config = new DatabaseConfiguration.Builder()
                .enabled(true)
                .type(DatabaseType.SQLITE)
                .sqliteFile("tools_repo_test.db")
                .maximumPoolSize(2)
                .build();
        provider.initialize(config);

        SqliteDialect dialect = new SqliteDialect();
        MigrationRunner runner = new MigrationRunner(provider, dialect, logger);
        runner.registerMigration(new V1__InitialSchemaMigration());
        runner.registerMigration(new V2__CreateToolProgressionSchemaMigration());
        runner.registerMigration(new V3__AddMaterialToToolProgressionSchemaMigration());
        runner.runMigrations();

        executor = new AsyncPersistenceExecutor(logger, 2);
        repository = new JdbcToolRepository(provider, dialect, executor);
    }

    @AfterEach
    void tearDown() {
        if (executor != null) executor.shutdown();
        if (provider != null) provider.shutdown();
    }

    @Test
    @DisplayName("Should persist and retrieve tool progression correctly in SQLite database")
    void testSaveAndFind() {
        UUID toolUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        ToolProgression progression = new ToolProgression(
                toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 1, ToolState.ACTIVE, 59, 1, Instant.now(), Instant.now()
        );

        repository.save(progression);

        Optional<ToolProgression> foundByOwner = repository.findByOwnerAndType(ownerUuid, ToolType.PICKAXE);
        assertTrue(foundByOwner.isPresent());
        assertEquals(toolUuid, foundByOwner.get().getToolUuid());
        assertEquals(ToolMaterial.WOODEN, foundByOwner.get().getMaterial());
        assertEquals(1, foundByOwner.get().getLevel());
        assertEquals(ToolState.ACTIVE, foundByOwner.get().getState());

        Optional<ToolProgression> foundById = repository.findById(toolUuid);
        assertTrue(foundById.isPresent());
        assertEquals(ownerUuid, foundById.get().getOwnerUuid());
    }

    @Test
    @DisplayName("Should update state to DESTROYED and preserve progression row")
    void testUpdateState() {
        UUID toolUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        ToolProgression progression = new ToolProgression(
                toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 2, ToolState.ACTIVE, 75, 1, Instant.now(), Instant.now()
        );

        repository.save(progression);

        boolean updated = repository.updateState(toolUuid, ToolState.DESTROYED);
        assertTrue(updated);

        Optional<ToolProgression> found = repository.findById(toolUuid);
        assertTrue(found.isPresent());
        assertEquals(ToolState.DESTROYED, found.get().getState());
        assertEquals(2, found.get().getLevel());
    }

    @Test
    @DisplayName("Should update state to LOST and preserve progression row")
    void testUpdateStateToLost() {
        UUID toolUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        ToolProgression progression = new ToolProgression(
                toolUuid, ownerUuid, ToolType.PICKAXE, ToolMaterial.WOODEN, 3, ToolState.ACTIVE, 95, 1, Instant.now(), Instant.now()
        );

        repository.save(progression);

        boolean updated = repository.updateState(toolUuid, ToolState.LOST);
        assertTrue(updated);

        Optional<ToolProgression> found = repository.findById(toolUuid);
        assertTrue(found.isPresent());
        assertEquals(ToolState.LOST, found.get().getState());
        assertEquals(3, found.get().getLevel());
    }
}
