package io.github.kaivian.kupdater.api.tools.event;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player's tool progression is upgraded to a higher level.
 */
public class ToolUpgradeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ToolProgression oldProgression;
    private final ToolProgression newProgression;

    public ToolUpgradeEvent(Player player, ToolProgression oldProgression, ToolProgression newProgression) {
        this.player = player;
        this.oldProgression = oldProgression;
        this.newProgression = newProgression;
    }

    public Player getPlayer() {
        return player;
    }

    public ToolProgression getOldProgression() {
        return oldProgression;
    }

    public ToolProgression getNewProgression() {
        return newProgression;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
