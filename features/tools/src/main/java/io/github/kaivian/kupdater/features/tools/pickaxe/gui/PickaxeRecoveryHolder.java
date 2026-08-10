package io.github.kaivian.kupdater.features.tools.pickaxe.gui;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Custom InventoryHolder to uniquely identify and securely scope Pickaxe Recovery GUI sessions.
 * Tracks confirmation countdown state for the merged action button.
 */
public class PickaxeRecoveryHolder implements InventoryHolder {

    private final Player player;
    private final ToolProgression progression;
    private final RecoveryPreview preview;
    private Inventory inventory;

    /** Whether the player has clicked the action button and is in the 3s confirmation countdown. */
    private volatile boolean confirming = false;
    /** The scheduled countdown task (so it can be cancelled if the player clicks again or closes). */
    private volatile BukkitTask confirmTask;

    public PickaxeRecoveryHolder(Player player, ToolProgression progression, RecoveryPreview preview) {
        this.player = Objects.requireNonNull(player, "player cannot be null");
        this.progression = progression;
        this.preview = preview;
    }

    public Player getPlayer() {
        return player;
    }

    public ToolProgression getProgression() {
        return progression;
    }

    public RecoveryPreview getPreview() {
        return preview;
    }

    public boolean isConfirming() {
        return confirming;
    }

    public void setConfirming(boolean confirming) {
        this.confirming = confirming;
    }

    public BukkitTask getConfirmTask() {
        return confirmTask;
    }

    public void setConfirmTask(BukkitTask confirmTask) {
        this.confirmTask = confirmTask;
    }

    /**
     * Cancels any running confirmation countdown and resets the confirming state.
     */
    public void cancelConfirmation() {
        this.confirming = false;
        if (this.confirmTask != null) {
            try {
                this.confirmTask.cancel();
            } catch (Throwable ignored) {
            }
            this.confirmTask = null;
        }
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
