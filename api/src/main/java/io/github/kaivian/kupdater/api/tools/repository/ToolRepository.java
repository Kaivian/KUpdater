package io.github.kaivian.kupdater.api.tools.repository;

import io.github.kaivian.kupdater.api.database.Repository;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence contract for managing tool progression domain objects.
 */
public interface ToolRepository extends Repository<ToolProgression, UUID> {

    /**
     * Finds a player's tool progression for a specific tool type.
     *
     * @param ownerUuid player UUID
     * @param toolType  tool category
     * @return Optional containing tool progression if found
     */
    Optional<ToolProgression> findByOwnerAndType(UUID ownerUuid, ToolType toolType);

    /**
     * Finds a player's tool progression asynchronously.
     *
     * @param ownerUuid player UUID
     * @param toolType  tool category
     * @return CompletableFuture containing tool progression if found
     */
    CompletableFuture<Optional<ToolProgression>> findByOwnerAndTypeAsync(UUID ownerUuid, ToolType toolType);

    /**
     * Updates the lifecycle state of a tool progression.
     *
     * @param toolUuid tool UUID
     * @param newState target state
     * @return true if update succeeded
     */
    boolean updateState(UUID toolUuid, ToolState newState);

    /**
     * Updates the current level of a tool progression.
     *
     * @param toolUuid tool UUID
     * @param newLevel new level
     * @return true if update succeeded
     */
    boolean updateLevel(UUID toolUuid, int newLevel);
}
