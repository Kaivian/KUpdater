package io.github.kaivian.kupdater.features.tools.common.repository;

import io.github.kaivian.kupdater.api.database.DatabaseProvider;
import io.github.kaivian.kupdater.api.database.PersistenceExecutor;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryHistoryEntry;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
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
 * JDBC implementation of ToolRecoveryRepository supporting state upserts and history logging.
 */
public class JdbcToolRecoveryRepository extends AbstractJdbcRepository<ToolRecoveryState, UUID> implements ToolRecoveryRepository {

    private static final String STATE_TABLE = "kupdater_tool_recovery_state";
    private static final String HISTORY_TABLE = "kupdater_tool_recovery_history";

    public JdbcToolRecoveryRepository(DatabaseProvider provider, DatabaseDialect dialect, PersistenceExecutor executor) {
        super(provider, dialect, executor);
    }

    @Override
    public Optional<ToolRecoveryState> findByToolUuid(UUID toolUuid) {
        if (toolUuid == null) return Optional.empty();

        String sql = "SELECT tool_uuid, owner_uuid, recovery_count, last_recovery_at, next_recovery_at, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(STATE_TABLE)
                + " WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";

        return executeQuery(sql, stmt -> stmt.setString(1, toolUuid.toString()), this::mapSingleState);
    }

    @Override
    public CompletableFuture<Optional<ToolRecoveryState>> findByToolUuidAsync(UUID toolUuid) {
        if (executor != null) {
            return executor.executeAsync(() -> findByToolUuid(toolUuid));
        }
        return CompletableFuture.completedFuture(findByToolUuid(toolUuid));
    }

    @Override
    public Optional<ToolRecoveryState> findByOwnerUuid(UUID ownerUuid) {
        if (ownerUuid == null) return Optional.empty();

        String sql = "SELECT tool_uuid, owner_uuid, recovery_count, last_recovery_at, next_recovery_at, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(STATE_TABLE)
                + " WHERE " + dialect.quoteIdentifier("owner_uuid") + " = ?";

        return executeQuery(sql, stmt -> stmt.setString(1, ownerUuid.toString()), this::mapSingleState);
    }

    @Override
    public ToolRecoveryState saveState(ToolRecoveryState entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");

        String upsertSql = buildStateUpsertSql();
        Timestamp now = Timestamp.from(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : Instant.now());
        Timestamp created = Timestamp.from(entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
        Timestamp lastRec = entity.getLastRecoveryAt() != null ? Timestamp.from(entity.getLastRecoveryAt()) : null;
        Timestamp nextRec = entity.getNextRecoveryAt() != null ? Timestamp.from(entity.getNextRecoveryAt()) : null;

        executeUpdate(upsertSql, stmt -> {
            stmt.setString(1, entity.getToolUuid().toString());
            stmt.setString(2, entity.getOwnerUuid().toString());
            stmt.setInt(3, entity.getRecoveryCount());
            stmt.setTimestamp(4, lastRec);
            stmt.setTimestamp(5, nextRec);
            stmt.setTimestamp(6, created);
            stmt.setTimestamp(7, now);
        });

        return findByToolUuid(entity.getToolUuid()).orElse(entity);
    }

    private String buildStateUpsertSql() {
        String table = dialect.quoteIdentifier(STATE_TABLE);
        String tUuid = dialect.quoteIdentifier("tool_uuid");
        String oUuid = dialect.quoteIdentifier("owner_uuid");
        String count = dialect.quoteIdentifier("recovery_count");
        String lastRec = dialect.quoteIdentifier("last_recovery_at");
        String nextRec = dialect.quoteIdentifier("next_recovery_at");
        String createdAt = dialect.quoteIdentifier("created_at");
        String updatedAt = dialect.quoteIdentifier("updated_at");

        String baseInsert = "INSERT INTO " + table + " ("
                + tUuid + ", " + oUuid + ", " + count + ", " + lastRec + ", " + nextRec + ", " + createdAt + ", " + updatedAt
                + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        switch (dialect.getType()) {
            case MYSQL:
            case MARIADB:
                return baseInsert + " ON DUPLICATE KEY UPDATE "
                        + oUuid + " = VALUES(" + oUuid + "), "
                        + count + " = VALUES(" + count + "), "
                        + lastRec + " = VALUES(" + lastRec + "), "
                        + nextRec + " = VALUES(" + nextRec + "), "
                        + updatedAt + " = VALUES(" + updatedAt + ")";
            case POSTGRESQL:
                return baseInsert + " ON CONFLICT (" + tUuid + ") DO UPDATE SET "
                        + oUuid + " = EXCLUDED." + oUuid + ", "
                        + count + " = EXCLUDED." + count + ", "
                        + lastRec + " = EXCLUDED." + lastRec + ", "
                        + nextRec + " = EXCLUDED." + nextRec + ", "
                        + updatedAt + " = EXCLUDED." + updatedAt;
            case SQLITE:
            default:
                return baseInsert + " ON CONFLICT(" + tUuid + ") DO UPDATE SET "
                        + oUuid + " = excluded." + oUuid + ", "
                        + count + " = excluded." + count + ", "
                        + lastRec + " = excluded." + lastRec + ", "
                        + nextRec + " = excluded." + nextRec + ", "
                        + updatedAt + " = excluded." + updatedAt;
        }
    }

    @Override
    public ToolRecoveryHistoryEntry saveHistory(ToolRecoveryHistoryEntry entry) {
        if (entry == null) throw new IllegalArgumentException("History entry cannot be null");

        String table = dialect.quoteIdentifier(HISTORY_TABLE);
        String sql = "INSERT INTO " + table + " ("
                + dialect.quoteIdentifier("tool_uuid") + ", "
                + dialect.quoteIdentifier("owner_uuid") + ", "
                + dialect.quoteIdentifier("recovery_count") + ", "
                + dialect.quoteIdentifier("penalty_level") + ", "
                + dialect.quoteIdentifier("currency_cost") + ", "
                + dialect.quoteIdentifier("items_consumed") + ", "
                + dialect.quoteIdentifier("durability_penalty_percent") + ", "
                + dialect.quoteIdentifier("recovered_at")
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Timestamp recAt = Timestamp.from(entry.getRecoveredAt() != null ? entry.getRecoveredAt() : Instant.now());

        executeUpdate(sql, stmt -> {
            stmt.setString(1, entry.getToolUuid().toString());
            stmt.setString(2, entry.getOwnerUuid().toString());
            stmt.setInt(3, entry.getRecoveryCount());
            stmt.setInt(4, entry.getPenaltyLevel());
            stmt.setDouble(5, entry.getCurrencyCost());
            stmt.setString(6, entry.getItemsConsumedJson());
            stmt.setDouble(7, entry.getDurabilityPenaltyPercent());
            stmt.setTimestamp(8, recAt);
        });

        return entry;
    }

    @Override
    public List<ToolRecoveryHistoryEntry> findHistoryByToolUuid(UUID toolUuid) {
        if (toolUuid == null) return new ArrayList<>();

        String sql = "SELECT id, tool_uuid, owner_uuid, recovery_count, penalty_level, currency_cost, items_consumed, durability_penalty_percent, recovered_at "
                + "FROM " + dialect.quoteIdentifier(HISTORY_TABLE)
                + " WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?"
                + " ORDER BY id ASC";

        return executeQuery(sql, stmt -> stmt.setString(1, toolUuid.toString()), rs -> {
            List<ToolRecoveryHistoryEntry> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowHistory(rs));
            }
            return list;
        });
    }

    @Override
    public Optional<ToolRecoveryState> findById(UUID toolUuid) {
        return findByToolUuid(toolUuid);
    }

    @Override
    public ToolRecoveryState save(ToolRecoveryState entity) {
        return saveState(entity);
    }

    @Override
    public void deleteById(UUID toolUuid) {
        if (toolUuid == null) return;
        String sql = "DELETE FROM " + dialect.quoteIdentifier(STATE_TABLE)
                + " WHERE " + dialect.quoteIdentifier("tool_uuid") + " = ?";
        executeUpdate(sql, stmt -> stmt.setString(1, toolUuid.toString()));
    }

    @Override
    public List<ToolRecoveryState> findAll() {
        String sql = "SELECT tool_uuid, owner_uuid, recovery_count, last_recovery_at, next_recovery_at, created_at, updated_at "
                + "FROM " + dialect.quoteIdentifier(STATE_TABLE);

        return executeQuery(sql, null, rs -> {
            List<ToolRecoveryState> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRowState(rs));
            }
            return list;
        });
    }

    private Optional<ToolRecoveryState> mapSingleState(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return Optional.of(mapRowState(rs));
        }
        return Optional.empty();
    }

    private ToolRecoveryState mapRowState(ResultSet rs) throws SQLException {
        UUID toolUuid = UUID.fromString(rs.getString("tool_uuid"));
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        int recoveryCount = rs.getInt("recovery_count");

        Timestamp lastTs = rs.getTimestamp("last_recovery_at");
        Timestamp nextTs = rs.getTimestamp("next_recovery_at");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        Instant lastRec = lastTs != null ? lastTs.toInstant() : null;
        Instant nextRec = nextTs != null ? nextTs.toInstant() : null;
        Instant created = createdTs != null ? createdTs.toInstant() : Instant.now();
        Instant updated = updatedTs != null ? updatedTs.toInstant() : Instant.now();

        return new ToolRecoveryState(toolUuid, ownerUuid, recoveryCount, lastRec, nextRec, created, updated);
    }

    private ToolRecoveryHistoryEntry mapRowHistory(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        UUID toolUuid = UUID.fromString(rs.getString("tool_uuid"));
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        int recoveryCount = rs.getInt("recovery_count");
        int penaltyLevel = rs.getInt("penalty_level");
        double currencyCost = rs.getDouble("currency_cost");
        String itemsConsumed = rs.getString("items_consumed");
        double durabilityPenalty = rs.getDouble("durability_penalty_percent");
        Timestamp recTs = rs.getTimestamp("recovered_at");
        Instant recoveredAt = recTs != null ? recTs.toInstant() : Instant.now();

        return new ToolRecoveryHistoryEntry(id, toolUuid, ownerUuid, recoveryCount, penaltyLevel, currencyCost, itemsConsumed, durabilityPenalty, recoveredAt);
    }
}
