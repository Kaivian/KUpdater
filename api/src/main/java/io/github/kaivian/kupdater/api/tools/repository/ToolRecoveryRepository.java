package io.github.kaivian.kupdater.api.tools.repository;

import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryHistoryEntry;
import io.github.kaivian.kupdater.api.tools.model.ToolRecoveryState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for managing tool recovery state and audit history persistence.
 */
public interface ToolRecoveryRepository {

    /**
     * Finds the recovery state record for a tool UUID.
     *
     * @param toolUuid tool logical identifier
     * @return Optional containing recovery state if present
     */
    Optional<ToolRecoveryState> findByToolUuid(UUID toolUuid);

    /**
     * Finds the recovery state record for a tool UUID asynchronously.
     *
     * @param toolUuid tool logical identifier
     * @return CompletableFuture containing recovery state if present
     */
    CompletableFuture<Optional<ToolRecoveryState>> findByToolUuidAsync(UUID toolUuid);

    /**
     * Finds the recovery state record for a player owner UUID.
     *
     * @param ownerUuid owner player UUID
     * @return Optional containing recovery state if present
     */
    Optional<ToolRecoveryState> findByOwnerUuid(UUID ownerUuid);

    /**
     * Saves or updates a tool recovery state.
     *
     * @param state recovery state to save
     * @return saved recovery state
     */
    ToolRecoveryState saveState(ToolRecoveryState state);

    /**
     * Saves a recovery audit history entry.
     *
     * @param entry audit history entry
     * @return saved entry
     */
    ToolRecoveryHistoryEntry saveHistory(ToolRecoveryHistoryEntry entry);

    /**
     * Finds recovery history entries for a specific tool UUID.
     *
     * @param toolUuid tool logical identifier
     * @return list of history entries
     */
    List<ToolRecoveryHistoryEntry> findHistoryByToolUuid(UUID toolUuid);
}
