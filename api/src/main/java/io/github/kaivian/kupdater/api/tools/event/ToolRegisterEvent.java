package io.github.kaivian.kupdater.api.tools.event;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a new tool progression is registered for a player.
 */
public class ToolRegisterEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ToolProgression progression;

    public ToolRegisterEvent(Player player, ToolProgression progression) {
        this.player = player;
        this.progression = progression;
    }

    public Player getPlayer() {
        return player;
    }

    public ToolProgression getProgression() {
        return progression;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
