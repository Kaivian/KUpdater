package io.github.kaivian.kupdater.api.tools.event;

import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.model.ToolState;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a tool progression transitions state (e.g., ACTIVE -> LOST, ACTIVE -> DESTROYED).
 */
public class ToolStateChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ToolProgression progression;
    private final ToolState previousState;
    private final ToolState newState;

    public ToolStateChangeEvent(ToolProgression progression, ToolState previousState, ToolState newState) {
        this.progression = progression;
        this.previousState = previousState;
        this.newState = newState;
    }

    public ToolProgression getProgression() {
        return progression;
    }

    public ToolState getPreviousState() {
        return previousState;
    }

    public ToolState getNewState() {
        return newState;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
