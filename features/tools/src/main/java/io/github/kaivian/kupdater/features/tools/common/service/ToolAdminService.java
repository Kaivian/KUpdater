package io.github.kaivian.kupdater.features.tools.common.service;

import io.github.kaivian.kupdater.api.tools.model.ToolMaterial;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import io.github.kaivian.kupdater.api.tools.model.ToolType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Application-level administrative service contract for Tool Module operations.
 */
public interface ToolAdminService {

    enum Status {
        SUCCESS,
        PLAYER_NOT_FOUND,
        TOOL_NOT_FOUND,
        ALREADY_OWNS_TOOL,
        INVALID_AMOUNT,
        INVALID_LEVEL,
        EXCEEDS_REQUIREMENT,
        CONFIRMATION_REQUIRED,
        CONCURRENT_CONFLICT,
        ERROR
    }

    final class Result {
        private final Status status;
        private final String message;
        private final ToolProgression progression;

        public Result(Status status, String message) {
            this(status, message, null);
        }

        public Result(Status status, String message, ToolProgression progression) {
            this.status = status;
            this.message = message;
            this.progression = progression;
        }

        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public Optional<ToolProgression> getProgression() { return Optional.ofNullable(progression); }
        public boolean isSuccess() { return status == Status.SUCCESS; }
    }

    Result addXp(UUID targetUuid, int amount, CommandSender executor);
    Result removeXp(UUID targetUuid, int amount, CommandSender executor);
    Result setXp(UUID targetUuid, int amount, CommandSender executor);
    Result addLevel(UUID targetUuid, int amount, CommandSender executor);
    Result setLevel(UUID targetUuid, int targetLevel, boolean confirmed, CommandSender executor);
    Result resetProgression(UUID targetUuid, boolean confirmed, CommandSender executor);
    Result repairTool(UUID targetUuid, CommandSender executor);
    Result rebuildToolItem(Player targetPlayer, CommandSender executor);
    Result setToolState(UUID targetUuid, ToolState newState, boolean confirmed, CommandSender executor);
    Result giveTool(Player targetPlayer, ToolType type, ToolMaterial material, boolean replace, boolean confirmed, CommandSender executor);
    Result removeTool(UUID targetUuid, boolean confirmed, CommandSender executor);
}
