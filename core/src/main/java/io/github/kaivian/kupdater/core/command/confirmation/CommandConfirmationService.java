package io.github.kaivian.kupdater.core.command.confirmation;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reusable confirmation service managing 15-second pending action tokens
 * for destructive administrative operations (e.g. reset, remove, downward level set).
 */
public class CommandConfirmationService {

    public static final long DEFAULT_TTL_MILLIS = 15_000L;

    public static class PendingConfirmation {
        private final String actionKey;
        private final String details;
        private final long expirationTime;
        private final Runnable action;

        public PendingConfirmation(String actionKey, String details, long ttlMillis, Runnable action) {
            this.actionKey = actionKey;
            this.details = details;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
            this.action = action;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public String getActionKey() {
            return actionKey;
        }

        public String getDetails() {
            return details;
        }

        public Runnable getAction() {
            return action;
        }
    }

    private final Map<UUID, PendingConfirmation> playerPendingMap = new ConcurrentHashMap<>();
    private PendingConfirmation consolePending = null;
    private final long ttlMillis;

    public CommandConfirmationService() {
        this(DEFAULT_TTL_MILLIS);
    }

    public CommandConfirmationService(long ttlMillis) {
        this.ttlMillis = ttlMillis > 0 ? ttlMillis : DEFAULT_TTL_MILLIS;
    }

    private UUID getSenderId(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return null;
    }

    /**
     * Checks if an operation requires confirmation. If confirmed flag is true or a valid pending
     * confirmation exists, clears pending state and returns true to proceed.
     * Otherwise registers a pending confirmation and sends a warning prompt to the sender.
     *
     * @param sender        command executor
     * @param confirmed     whether --confirm flag was passed
     * @param actionKey     unique action identifier
     * @param detailsPrompt human-readable description of consequences
     * @param action        callback action to run when confirmed
     * @return true if operation can proceed immediately, false if confirmation prompt was sent
     */
    public boolean handleConfirmation(CommandSender sender, boolean confirmed, String actionKey, String detailsPrompt, Runnable action) {
        Objects.requireNonNull(sender, "sender cannot be null");
        Objects.requireNonNull(actionKey, "actionKey cannot be null");

        if (confirmed) {
            clearPending(sender);
            return true;
        }

        UUID senderId = getSenderId(sender);
        PendingConfirmation pending = senderId != null ? playerPendingMap.get(senderId) : consolePending;

        if (pending != null && !pending.isExpired() && pending.getActionKey().equals(actionKey)) {
            clearPending(sender);
            if (action != null) {
                action.run();
            }
            return true;
        }

        PendingConfirmation newPending = new PendingConfirmation(actionKey, detailsPrompt, ttlMillis, action);
        if (senderId != null) {
            playerPendingMap.put(senderId, newPending);
        } else {
            consolePending = newPending;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&lWARNING: &7" + detailsPrompt));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&eTo confirm this destructive operation, re-run the command with &f--confirm &ewithin 15 seconds."));
        return false;
    }

    /**
     * Clears any pending confirmation for the sender.
     *
     * @param sender command executor
     */
    public void clearPending(CommandSender sender) {
        UUID senderId = getSenderId(sender);
        if (senderId != null) {
            playerPendingMap.remove(senderId);
        } else {
            consolePending = null;
        }
    }
}
