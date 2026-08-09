package io.github.kaivian.kupdater.features.tools.common.repository;

import io.github.kaivian.kupdater.api.database.DatabaseProvider;
import io.github.kaivian.kupdater.api.database.PersistenceExecutor;
import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.core.database.dialect.DatabaseDialect;
import io.github.kaivian.kupdater.core.database.execution.AbstractJdbcRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JDBC implementation of ToolRepository supporting multi-database persistence operations.
 */
public class JdbcToolRepository extends AbstractJdbcRepository<ToolProgression, UUID> implements ToolRepository {

    private static final String TABLE_NAME = "kupdater_tool_progression";

    public JdbcToolRepository(DatabaseProvider provider, DatabaseDialect dialect, PersistenceExecutor executor) {
        super(provider, dialect, executor);
    }

    @Override
    public Optional<ToolProgression> findById(UUID toolUuid) {
        if (toolUuid == null) return Optional.empty();

        String sql = "SELECT tool_uuid, owner_uuid, tool_type, material, level, xp, overflow_xp, state, current_durability, schema_version, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(TABLE_NAME)
                + " WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";

        return executeQuery(sql, stmt -> stmt.setString(1, toolUuid.toString()), this::mapSingleResult);
    }

    @Override
    public Optional<ToolProgression> findByOwnerAndType(UUID ownerUuid, ToolType toolType) {
        if (ownerUuid == null || toolType == null) return Optional.empty();

        String sql = "SELECT tool_uuid, owner_uuid, tool_type, material, level, xp, overflow_xp, state, current_durability, schema_version, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(TABLE_NAME)
                + " WHERE " + dialect.quoteIdentifier("owner_uuid") + " = ? AND "
                + dialect.quoteIdentifier("tool_type") + " = ?";

        return executeQuery(sql, stmt -> {
            stmt.setString(1, ownerUuid.toString());
            stmt.setString(2, toolType.name());
        }, this::mapSingleResult);
    }

    @Override
    public CompletableFuture<Optional<ToolProgression>> findByOwnerAndTypeAsync(UUID ownerUuid, ToolType toolType) {
        if (executor != null) {
            return executor.executeAsync(() -> findByOwnerAndType(ownerUuid, toolType));
        }
        return CompletableFuture.completedFuture(findByOwnerAndType(ownerUuid, toolType));
    }

    @Override
    public ToolProgression save(ToolProgression entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");

        String upsertSql = buildUpsertSql();
        Timestamp now = Timestamp.from(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : Instant.now());
        Timestamp created = Timestamp.from(entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());

        executeUpdate(upsertSql, stmt -> {
            stmt.setString(1, entity.getToolUuid().toString());
            stmt.setString(2, entity.getOwnerUuid().toString());
            stmt.setString(3, entity.getToolType().name());
            stmt.setString(4, entity.getMaterial() != null ? entity.getMaterial().name() : ToolMaterial.WOODEN.name());
            stmt.setInt(5, entity.getLevel());
            stmt.setInt(6, entity.getXp());
            stmt.setInt(7, entity.getOverflowXp());
            stmt.setString(8, entity.getState().name());
            stmt.setInt(9, entity.getCurrentDurability());
            stmt.setInt(10, entity.getSchemaVersion());
            stmt.setTimestamp(11, created);
            stmt.setTimestamp(12, now);
        });

        return findById(entity.getToolUuid()).orElse(entity);
    }

    private String buildUpsertSql() {
        String table = dialect.quoteIdentifier(TABLE_NAME);
        String tUuid = dialect.quoteIdentifier("tool_uuid");
        String oUuid = dialect.quoteIdentifier("owner_uuid");
        String tType = dialect.quoteIdentifier("tool_type");
        String mat = dialect.quoteIdentifier("material");
        String lvl = dialect.quoteIdentifier("level");
        String xp = dialect.quoteIdentifier("xp");
        String overflowXp = dialect.quoteIdentifier("overflow_xp");
        String state = dialect.quoteIdentifier("state");
        String dur = dialect.quoteIdentifier("current_durability");
        String ver = dialect.quoteIdentifier("schema_version");
        String createdAt = dialect.quoteIdentifier("created_at");
        String updatedAt = dialect.quoteIdentifier("updated_at");

        String baseInsert = "INSERT INTO " + table + " ("
                + tUuid + ", " + oUuid + ", " + tType + ", " + mat + ", " + lvl + ", " + xp + ", " + overflowXp + ", " + state + ", "
                + dur + ", " + ver + ", " + createdAt + ", " + updatedAt + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        switch (dialect.getType()) {
            case MYSQL:
            case MARIADB:
                return baseInsert + " ON DUPLICATE KEY UPDATE "
                        + tUuid + " = VALUES(" + tUuid + "), "
                        + mat + " = VALUES(" + mat + "), "
                        + lvl + " = VALUES(" + lvl + "), "
                        + xp + " = VALUES(" + xp + "), "
                        + overflowXp + " = VALUES(" + overflowXp + "), "
                        + state + " = VALUES(" + state + "), "
                        + dur + " = VALUES(" + dur + "), "
                        + ver + " = VALUES(" + ver + "), "
                        + updatedAt + " = VALUES(" + updatedAt + ")";
            case POSTGRESQL:
                return baseInsert + " ON CONFLICT (" + oUuid + ", " + tType + ") DO UPDATE SET "
                        + tUuid + " = EXCLUDED." + tUuid + ", "
                        + mat + " = EXCLUDED." + mat + ", "
                        + lvl + " = EXCLUDED." + lvl + ", "
                        + xp + " = EXCLUDED." + xp + ", "
                        + overflowXp + " = EXCLUDED." + overflowXp + ", "
                        + state + " = EXCLUDED." + state + ", "
                        + dur + " = EXCLUDED." + dur + ", "
                        + ver + " = EXCLUDED." + ver + ", "
                        + updatedAt + " = EXCLUDED." + updatedAt;
            case SQLITE:
            default:
                return baseInsert + " ON CONFLICT(" + oUuid + ", " + tType + ") DO UPDATE SET "
                        + tUuid + " = excluded." + tUuid + ", "
                        + mat + " = excluded." + mat + ", "
                        + lvl + " = excluded." + lvl + ", "
                        + xp + " = excluded." + xp + ", "
                        + overflowXp + " = excluded." + overflowXp + ", "
                        + state + " = excluded." + state + ", "
                        + dur + " = excluded." + dur + ", "
                        + ver + " = excluded." + ver + ", "
                        + updatedAt + " = excluded." + updatedAt;
        }
    }

    @Override
    public boolean updateState(UUID toolUuid, ToolState newState) {
        if (toolUuid == null || newState == null) return false;

        String sql = "UPDATE " + dialect.quoteIdentifier(TABLE_NAME)
                + " SET " + dialect.quoteIdentifier("state") + " = ?, "
                + dialect.quoteIdentifier("updated_at") + " = ? "
                + "WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";

        int rows = executeUpdate(sql, stmt -> {
            stmt.setString(1, newState.name());
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, toolUuid.toString());
        });
        return rows > 0;
    }

    @Override
    public boolean updateLevel(UUID toolUuid, int newLevel) {
        if (toolUuid == null) return false;

        String sql = "UPDATE " + dialect.quoteIdentifier(TABLE_NAME)
                + " SET " + dialect.quoteIdentifier("level") + " = ?, "
                + dialect.quoteIdentifier("updated_at") + " = ? "
                + "WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";

        int rows = executeUpdate(sql, stmt -> {
            stmt.setInt(1, newLevel);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, toolUuid.toString());
        });
        return rows > 0;
    }

    public void delete(ToolProgression entity) {
        if (entity == null) return;
        deleteById(entity.getToolUuid());
    }

    @Override
    public void deleteById(UUID toolUuid) {
        if (toolUuid == null) return;

        String sql = "DELETE FROM " + dialect.quoteIdentifier(TABLE_NAME)
                + " WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";

        executeUpdate(sql, stmt -> stmt.setString(1, toolUuid.toString()));
    }

    @Override
    public List<ToolProgression> findAll() {
        String sql = "SELECT tool_uuid, owner_uuid, tool_type, material, level, xp, overflow_xp, state, current_durability, schema_version, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(TABLE_NAME);

        return executeQuery(sql, null, rs -> {
            List<ToolProgression> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        });
    }

    private Optional<ToolProgression> mapSingleResult(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return Optional.of(mapRow(rs));
        }
        return Optional.empty();
    }

    private ToolProgression mapRow(ResultSet rs) throws SQLException {
        UUID toolUuid = UUID.fromString(rs.getString("tool_uuid"));
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        ToolType toolType = ToolType.fromString(rs.getString("tool_type"));

        ToolMaterial material = ToolMaterial.WOODEN;
        try {
            String matStr = rs.getString("material");
            if (matStr != null) {
                material = ToolMaterial.fromString(matStr);
            }
        } catch (SQLException ignored) {
        }

        int level = rs.getInt("level");
        int xp = 0;
        try {
            xp = rs.getInt("xp");
        } catch (SQLException ignored) {
        }

        int overflowXp = 0;
        try {
            overflowXp = rs.getInt("overflow_xp");
        } catch (SQLException ignored) {
        }

        ToolState state = ToolState.fromString(rs.getString("state"));
        int currentDurability = rs.getInt("current_durability");
        int schemaVersion = rs.getInt("schema_version");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        Instant createdAt = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updatedAt = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new ToolProgression(toolUuid, ownerUuid, toolType, material, level, xp, overflowXp, state, currentDurability, schemaVersion, createdAt, updatedAt);
    }
}
