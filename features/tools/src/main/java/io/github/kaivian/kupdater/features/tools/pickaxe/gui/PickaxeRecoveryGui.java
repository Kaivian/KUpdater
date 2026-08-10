package io.github.kaivian.kupdater.features.tools.pickaxe.gui;

import io.github.kaivian.kupdater.api.tools.model.RecoveryPreview;
import io.github.kaivian.kupdater.api.tools.model.ToolProgression;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager.RecoveryConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds and opens the 5x9 (45-slot) Pickaxe Recovery GUI.
 *
 * Layout:
 * Row 0: ___  ___  ___  ___  [PREVIEW]  ___  ___  ___  ___
 * Row 1: ___  ___  ___  ___  [REC INFO]  ___  ___  ___  ___
 * Row 2: ___  [CURRENCY]  ___  [ITEMS]  ___  [DURABILITY]  ___  [COOLDOWN]  ___
 * Row 3: ___  ___  ___  ___  [ACTION]  ___  ___  ___  ___
 * Row 4: (glass filler border)
 */
public class PickaxeRecoveryGui {

    // Row 0 center
    public static final int SLOT_PICKAXE_PREVIEW = 4;
    // Row 1 center
    public static final int SLOT_RECOVERY_INFO = 13;
    // Row 2: evenly spread penalty details
    public static final int SLOT_CURRENCY_COST = 19;
    public static final int SLOT_REQUIRED_ITEMS = 21;
    public static final int SLOT_DURABILITY_PENALTY = 23;
    public static final int SLOT_COOLDOWN_STATUS = 25;
    // Row 3 center: merged confirm/cancel action button
    public static final int SLOT_ACTION = 31;

    private final ToolService toolService;
    private final ToolConfigManager configManager;

    public PickaxeRecoveryGui(ToolService toolService, ToolConfigManager configManager) {
        this.toolService = toolService;
        this.configManager = configManager;
    }

    public void open(Player player, ToolProgression progression, RecoveryPreview preview) {
        if (player == null || progression == null || preview == null) return;

        RecoveryConfig cfg = configManager.getRecoveryConfig();
        String rawTitle = cfg != null ? cfg.getGuiTitle() : "\u00268Pickaxe Recovery";
        String title = ChatColor.translateAlternateColorCodes('&', rawTitle);

        PickaxeRecoveryHolder holder = new PickaxeRecoveryHolder(player, progression, preview);
        Inventory inventory = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inventory);

        // Fill background glass
        ItemStack filler = createItem(getMaterialSafe("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE"), "&r", null);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 4: Pickaxe Preview
        inventory.setItem(SLOT_PICKAXE_PREVIEW, createPickaxePreviewItem(progression, preview));

        // Slot 13: Recovery Info
        inventory.setItem(SLOT_RECOVERY_INFO, createRecoveryInfoItem(preview));

        // Slot 19: Currency Cost
        inventory.setItem(SLOT_CURRENCY_COST, createCurrencyCostItem(preview));

        // Slot 21: Required Items
        inventory.setItem(SLOT_REQUIRED_ITEMS, createRequiredItemsItem(preview));

        // Slot 23: Durability Penalty
        inventory.setItem(SLOT_DURABILITY_PENALTY, createDurabilityPenaltyItem(preview));

        // Slot 25: Cooldown Status
        inventory.setItem(SLOT_COOLDOWN_STATUS, createCooldownStatusItem(preview));

        // Slot 31: Action Button (Confirm or Barrier)
        inventory.setItem(SLOT_ACTION, createActionButton(preview));

        player.openInventory(inventory);
    }

    /**
     * Updates the action button in an open GUI inventory (used during confirm countdown).
     */
    public static void updateActionButton(Inventory inventory, ItemStack item) {
        if (inventory != null && inventory.getSize() > SLOT_ACTION) {
            inventory.setItem(SLOT_ACTION, item);
        }
    }

    private ItemStack createPickaxePreviewItem(ToolProgression progression, RecoveryPreview preview) {
        Material mat = configManager.getBukkitMaterial(progression.getMaterial());
        ItemStack item = new ItemStack(mat, 1);

        ToolProgression tempRestored = progression.withDurability(preview.getResultingDurability());
        toolService.applyMetadataToItem(item, tempRestored);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(0, ChatColor.translateAlternateColorCodes('&', "&e✦ RESTORED PREVIEW STATS ✦"));
            lore.add(1, "");
            lore.add(ChatColor.translateAlternateColorCodes('&', ""));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Recovered Durability: &f" + preview.getResultingDurability() + "&7/&f" + preview.getMaxDurability()));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Durability Penalty: &c-" + String.format("%.1f", preview.getDurabilityPenaltyPercent()) + "%"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRecoveryInfoItem(RecoveryPreview preview) {
        Material mat = getMaterialSafe("BOOK", "PAPER");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Current Recovery Number: &e#" + preview.getRecoveryNumber()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Calculated Penalty Level: &cLevel " + preview.getPenaltyLevel()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Tool Material Tier: &f" + preview.getProgression().getMaterial().getDisplayName()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Tool Level: &aLevel " + preview.getProgression().getLevel()));

        return createItem(mat, "&e&l✦ Recovery Summary", lore);
    }

    private ItemStack createCurrencyCostItem(RecoveryPreview preview) {
        Material mat = getMaterialSafe("GOLD_INGOT", "SUNFLOWER");
        List<String> lore = new ArrayList<>();
        if (preview.getCurrencyCost() <= 0.0) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Required Currency: &aFREE"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &aMet"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Required Currency: &e$" + String.format("%.2f", preview.getCurrencyCost())));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Your Balance: &f$" + String.format("%.2f", preview.getCurrencyAvailable())));
            if (preview.getStatus() == RecoveryPreview.Status.ECONOMY_UNAVAILABLE) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &cEconomy Disabled"));
            } else if (preview.isCurrencySufficient()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &aMet"));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &cInsufficient Funds"));
            }
        }
        return createItem(mat, "&6&l$ Currency Requirement", lore);
    }

    private ItemStack createRequiredItemsItem(RecoveryPreview preview) {
        Material mat = getMaterialSafe("CHEST", "CHEST_MINECART");
        List<String> lore = new ArrayList<>();
        if (preview.getRequiredItems() == null || preview.getRequiredItems().isEmpty()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Required Materials: &aNone"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &aMet"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Required Materials:"));
            for (ItemStack req : preview.getRequiredItems()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', " &8- &f" + req.getAmount() + "x " + formatMaterialName(req.getType())));
            }
            lore.add("");
            if (preview.hasRequiredItems()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &aMet"));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &cMissing Materials"));
            }
        }
        return createItem(mat, "&e&l\uD83D\uDCE6 Material Requirements", lore);
    }

    private ItemStack createDurabilityPenaltyItem(RecoveryPreview preview) {
        Material mat = getMaterialSafe("ANVIL", "DAMAGED_ANVIL");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Durability Penalty: &c-" + String.format("%.1f", preview.getDurabilityPenaltyPercent()) + "%"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Max Durability: &f" + preview.getMaxDurability()));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Resulting Durability: &a" + preview.getResultingDurability() + " / " + preview.getMaxDurability()));

        return createItem(mat, "&c&l\uD83D\uDEE1 Durability Penalty", lore);
    }

    private ItemStack createCooldownStatusItem(RecoveryPreview preview) {
        Material mat = getMaterialSafe("CLOCK", "WATCH");
        List<String> lore = new ArrayList<>();
        if (preview.getCooldownRemainingSeconds() > 0) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &cOn Cooldown"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Remaining: &e" + formatTime(preview.getCooldownRemainingSeconds())));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Status: &aReady"));
            if (preview.getCooldownDurationSeconds() > 0) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Next Cooldown: &f" + formatTime(preview.getCooldownDurationSeconds())));
            }
        }
        return createItem(mat, "&b&l⏳ Cooldown Status", lore);
    }

    private ItemStack createActionButton(RecoveryPreview preview) {
        if (preview.isAvailable()) {
            Material mat = getMaterialSafe("LIME_WOOL", "LIME_STAINED_GLASS_PANE");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to recover your pickaxe."));
            return createItem(mat, "&a&l✔ RECOVER PICKAXE", lore);
        } else {
            Material mat = getMaterialSafe("BARRIER", "REDSTONE_BLOCK");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&c" + preview.getFailureReason()));
            return createItem(mat, "&c&l✖ RECOVERY UNAVAILABLE", lore);
        }
    }

    /**
     * Creates a confirmation countdown button (used during 3s confirm).
     */
    public static ItemStack createConfirmingButton(int secondsRemaining) {
        Material mat;
        try {
            mat = Material.valueOf("YELLOW_WOOL");
        } catch (Throwable t) {
            try {
                mat = Material.valueOf("YELLOW_STAINED_GLASS_PANE");
            } catch (Throwable t2) {
                mat = Material.GOLD_BLOCK;
            }
        }
        ItemStack stack = new ItemStack(mat, Math.max(1, secondsRemaining));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&l⏳ CONFIRMING... " + secondsRemaining + "s"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Recovery will execute in &e" + secondsRemaining + "&7 seconds..."));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cClick again to cancel."));
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material != null ? material : Material.PAPER, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore != null) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private Material getMaterialSafe(String primary, String fallback) {
        Material mat = Material.matchMaterial(primary);
        if (mat != null) return mat;
        mat = Material.matchMaterial(fallback);
        return mat != null ? mat : Material.PAPER;
    }

    private String formatMaterialName(Material mat) {
        if (mat == null) return "";
        String[] parts = mat.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
