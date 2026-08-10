package io.github.kaivian.kupdater.api.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Unified execution context wrapping the CommandSender, invocation label,
 * and a map of pre-parsed typed arguments provided by the underlying platform command backend.
 */
public final class KCommandContext {

    private final CommandSender sender;
    private final String label;
    private final Map<String, Object> parsedArguments;

    public KCommandContext(CommandSender sender, String label, Map<String, Object> parsedArguments) {
        this.sender = Objects.requireNonNull(sender, "CommandSender cannot be null");
        this.label = label != null ? label : "kupdater";
        this.parsedArguments = parsedArguments != null ? Collections.unmodifiableMap(parsedArguments) : Collections.emptyMap();
    }

    /**
     * Gets the command sender.
     *
     * @return Bukkit CommandSender
     */
    public CommandSender sender() {
        return sender;
    }

    /**
     * Gets the command label used to invoke this command.
     *
     * @return label string
     */
    public String label() {
        return label;
    }

    /**
     * Checks if the sender is an in-game Player.
     *
     * @return true if sender is a Player
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Casts the sender to a Player.
     *
     * @return Player instance
     * @throws ClassCastException if sender is not a Player
     */
    public Player asPlayer() {
        return (Player) sender;
    }

    /**
     * Retrieves a pre-parsed argument by name cast to the specified class.
     *
     * @param name  argument name
     * @param clazz expected value type
     * @param <T>   argument type
     * @return Optional containing the argument value if present and matching type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getArgument(String name, Class<T> clazz) {
        if (name == null || clazz == null) return Optional.empty();
        Object val = parsedArguments.get(name);
        if (val != null && clazz.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }

    /**
     * Gets an unmodifiable view of all parsed arguments.
     *
     * @return map of argument names to parsed values
     */
    public Map<String, Object> getParsedArguments() {
        return parsedArguments;
    }

    /**
     * Sends a formatted message to the command sender with color code translation.
     *
     * @param message template message
     * @param args    formatting arguments
     */
    public void reply(String message, Object... args) {
        if (message == null || message.isEmpty()) return;
        String formatted = args.length > 0 ? String.format(message, args) : message;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', formatted));
    }

    /**
     * Sends a formatted success message.
     *
     * @param message message string
     * @param args    formatting arguments
     */
    public void replySuccess(String message, Object... args) {
        reply("&a★ " + message, args);
    }

    /**
     * Sends a formatted error message.
     *
     * @param message error message string
     * @param args    formatting arguments
     */
    public void replyError(String message, Object... args) {
        reply("&c" + message, args);
    }
}
