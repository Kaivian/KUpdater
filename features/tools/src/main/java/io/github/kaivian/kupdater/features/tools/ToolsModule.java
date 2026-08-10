package io.github.kaivian.kupdater.features.tools;

import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.tools.repository.ToolRepository;
import io.github.kaivian.kupdater.api.tools.service.ToolDurabilityService;
import io.github.kaivian.kupdater.api.tools.service.ToolOwnershipService;
import io.github.kaivian.kupdater.api.tools.service.ToolRecoveryService;
import io.github.kaivian.kupdater.api.tools.service.ToolService;
import io.github.kaivian.kupdater.api.tools.service.ToolUpgradeService;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;
import io.github.kaivian.kupdater.core.config.ConfigManager;
import io.github.kaivian.kupdater.core.database.DatabaseManager;
import io.github.kaivian.kupdater.features.tools.common.config.ToolConfigManager;
import io.github.kaivian.kupdater.features.tools.common.listener.ToolDurabilityListener;
import io.github.kaivian.kupdater.features.tools.common.listener.ToolLossListener;
import io.github.kaivian.kupdater.features.tools.common.listener.ToolUsageListener;
import io.github.kaivian.kupdater.features.tools.common.metadata.ToolItemMetadataService;
import io.github.kaivian.kupdater.features.tools.common.recipe.ToolRecipeManager;
import io.github.kaivian.kupdater.features.tools.common.repository.JdbcToolRepository;
import io.github.kaivian.kupdater.features.tools.common.service.ToolDurabilityServiceImpl;
import io.github.kaivian.kupdater.features.tools.common.service.ToolOwnershipServiceImpl;
import io.github.kaivian.kupdater.features.tools.common.service.ToolRecoveryServiceImpl;
import io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService;
import io.github.kaivian.kupdater.features.tools.common.service.ToolItemSynchronizer;
import io.github.kaivian.kupdater.features.tools.common.service.ToolServiceImpl;
import io.github.kaivian.kupdater.features.tools.common.validation.ProgressionBalanceValidator;
import io.github.kaivian.kupdater.api.tools.repository.ToolRecoveryRepository;
import io.github.kaivian.kupdater.features.tools.common.economy.EconomyProvider;
import io.github.kaivian.kupdater.features.tools.common.economy.VaultEconomyAdapter;
import io.github.kaivian.kupdater.features.tools.common.repository.JdbcToolRecoveryRepository;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryPenaltyEngine;
import io.github.kaivian.kupdater.features.tools.common.service.RecoveryTransactionManager;
import io.github.kaivian.kupdater.features.tools.pickaxe.gui.PickaxeRecoveryGui;
import io.github.kaivian.kupdater.features.tools.pickaxe.gui.PickaxeRecoveryGuiListener;
import io.github.kaivian.kupdater.features.tools.pickaxe.listener.PickaxeRegistrationListener;
import io.github.kaivian.kupdater.features.tools.pickaxe.service.PickaxeUpgradeServiceImpl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;

/**
 * Tools & Equipment Progression module implementation.
 * Supported Minecraft version range: 1.21.0 to 26.2.0.
 */
public class ToolsModule extends AbstractKModule {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;

    private ToolConfigManager toolConfigManager;
    private ToolRepository repository;
    private ToolItemMetadataService metadataService;
    private ToolService toolService;
    private ToolOwnershipService ownershipService;
    private ToolDurabilityService durabilityService;
    private ToolUpgradeService upgradeService;
    private ToolRecoveryService recoveryService;
    private ToolRecoveryRepository recoveryRepository;
    private EconomyProvider economyProvider;
    private RecoveryPenaltyEngine recoveryPenaltyEngine;
    private RecoveryTransactionManager recoveryTransactionManager;
    private PickaxeRecoveryGui recoveryGui;
    private ToolRecipeManager recipeManager;
    private ToolItemSynchronizer itemSynchronizer;
    private io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService adminService;
    private io.github.kaivian.kupdater.core.command.confirmation.CommandConfirmationService confirmationService;

    public ToolsModule() {
        this(null, null, null);
    }

    public ToolsModule(Plugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        super(
                "tools",
                "Tools Progression Module",
                "Tools and equipment progression enhancements.",
                ModuleCategory.TOOLS,
                MinecraftVersion.V1_21,
                MinecraftVersion.V26_2
        );
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    @Override
    public boolean requiresPersistence() {
        return true;
    }

    @Override
    public void onEnable() {
        if (databaseManager == null || !databaseManager.isAvailable()) {
            throw new IllegalStateException("DatabaseManager persistence layer is required but unavailable.");
        }

        Logger logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ToolsModule");

        // 1. Isolated Module Config (plugins/KUpdater/modules/tools/tool-config.yml & pickaxe.yml)
        this.toolConfigManager = new ToolConfigManager(logger);
        if (configManager != null) {
            org.bukkit.configuration.file.FileConfiguration sharedConfig = configManager.loadModuleConfigFile("tools", "tool-config.yml");
            org.bukkit.configuration.file.FileConfiguration pickaxeConfig = configManager.loadModuleConfigFile("tools", "pickaxe.yml");
            this.toolConfigManager.load(sharedConfig, pickaxeConfig);
        } else {
            this.toolConfigManager.load(null);
        }

        // 2. Repository
        this.repository = new JdbcToolRepository(
                databaseManager.getProvider(),
                databaseManager.getDialect(),
                databaseManager.getExecutor()
        );
        this.recoveryRepository = new JdbcToolRecoveryRepository(
                databaseManager.getProvider(),
                databaseManager.getDialect(),
                databaseManager.getExecutor()
        );

        // 3. Metadata & Services
        this.metadataService = new ToolItemMetadataService(plugin, this.toolConfigManager);
        this.durabilityService = new ToolDurabilityServiceImpl(this.toolConfigManager, this.repository, this.metadataService, logger);
        this.toolService = new ToolServiceImpl(this.repository, this.metadataService, this.toolConfigManager, this.durabilityService, logger);
        this.ownershipService = new ToolOwnershipServiceImpl(this.toolService, this.toolConfigManager);
        this.upgradeService = new PickaxeUpgradeServiceImpl(this.toolService, this.repository, this.toolConfigManager, new ProgressionBalanceValidator(logger), logger);

        this.economyProvider = new VaultEconomyAdapter(logger);
        this.recoveryPenaltyEngine = new RecoveryPenaltyEngine(this.toolConfigManager, this.repository, this.recoveryRepository, this.durabilityService, this.economyProvider);
        this.recoveryTransactionManager = new RecoveryTransactionManager(this.toolConfigManager, this.repository, this.recoveryRepository, this.toolService, this.recoveryPenaltyEngine, this.economyProvider, logger);
        this.recoveryService = new ToolRecoveryServiceImpl(this.repository, this.recoveryRepository, this.recoveryPenaltyEngine, this.recoveryTransactionManager);
        this.recoveryGui = new PickaxeRecoveryGui(this.toolService, this.toolConfigManager);

        this.itemSynchronizer = new io.github.kaivian.kupdater.features.tools.common.service.ToolItemSynchronizer(plugin, this.toolService, this.metadataService, this.toolConfigManager, logger);
        this.confirmationService = new io.github.kaivian.kupdater.core.command.confirmation.CommandConfirmationService();
        this.adminService = new io.github.kaivian.kupdater.features.tools.common.service.ToolAdminServiceImpl(
                this.toolService, this.repository, this.toolConfigManager, this.itemSynchronizer, this.confirmationService, logger
        );

        // 4. Register Event Listeners
        if (plugin != null && plugin.getServer() != null && plugin.getServer().getPluginManager() != null) {
            PluginManager pm = plugin.getServer().getPluginManager();
            pm.registerEvents(new PickaxeRegistrationListener(this.toolService, plugin), plugin);
            pm.registerEvents(new ToolUsageListener(this.toolService, this.ownershipService, this.toolConfigManager), plugin);
            pm.registerEvents(new io.github.kaivian.kupdater.features.tools.pickaxe.listener.PickaxeBlockXpListener(this.toolService, this.ownershipService, this.upgradeService, this.repository, this.toolConfigManager), plugin);
            pm.registerEvents(new io.github.kaivian.kupdater.features.tools.pickaxe.listener.PickaxeCraftUpgradeListener(this.toolService, this.upgradeService, this.repository, this.toolConfigManager), plugin);
            pm.registerEvents(new io.github.kaivian.kupdater.features.tools.pickaxe.listener.PickaxeSmithingUpgradeListener(this.toolService, this.upgradeService, this.toolConfigManager), plugin);
            pm.registerEvents(new io.github.kaivian.kupdater.features.tools.pickaxe.listener.PickaxeMiningSpeedListener(this.toolService, this.toolConfigManager), plugin);
            pm.registerEvents(new ToolDurabilityListener(this.toolService, this.durabilityService), plugin);
            pm.registerEvents(new ToolLossListener(this.toolService), plugin);
            pm.registerEvents(new io.github.kaivian.kupdater.features.tools.common.listener.ToolCreativeListener(this.toolService, this.repository, this.toolConfigManager, plugin), plugin);
            pm.registerEvents(new PickaxeRecoveryGuiListener(this.recoveryTransactionManager, this.toolConfigManager, plugin), plugin);
        }

        // 5. Register Crafting Recipes
        if (plugin != null) {
            this.recipeManager = new ToolRecipeManager(plugin, this.toolConfigManager, this.metadataService);
            this.recipeManager.registerUpgradeRecipes();
        }

        logger.info("[ToolUpdater] Enabled Pickaxe progression module.");
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.unregisterUpgradeRecipes();
        }
        Logger logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ToolsModule");
        logger.info("[ToolUpdater] Disabled Pickaxe progression module.");
    }

    public boolean reloadConfigAtomic() {
        Logger logger = plugin != null ? plugin.getLogger() : Logger.getLogger("ToolsModule");
        try {
            ToolConfigManager candidate = new ToolConfigManager(logger);
            if (configManager != null) {
                org.bukkit.configuration.file.FileConfiguration sharedConfig = configManager.loadModuleConfigFile("tools", "tool-config.yml");
                org.bukkit.configuration.file.FileConfiguration pickaxeConfig = configManager.loadModuleConfigFile("tools", "pickaxe.yml");
                candidate.load(sharedConfig, pickaxeConfig);
            } else {
                candidate.load(null);
            }

            this.toolConfigManager = candidate;
            if (recipeManager != null) {
                recipeManager.unregisterUpgradeRecipes();
                recipeManager.registerUpgradeRecipes();
            }
            logger.info("[ToolUpdater] Reloaded tools module configuration atomically.");
            return true;
        } catch (Throwable t) {
            logger.severe("[ToolUpdater] Failed to reload tools module configuration: " + t.getMessage());
            return false;
        }
    }

    public void reloadConfig() {
        reloadConfigAtomic();
    }

    public ToolConfigManager getToolConfigManager() {
        return toolConfigManager;
    }

    public ToolRepository getRepository() {
        return repository;
    }

    public ToolItemMetadataService getMetadataService() {
        return metadataService;
    }

    public ToolService getToolService() {
        return toolService;
    }

    public ToolOwnershipService getOwnershipService() {
        return ownershipService;
    }

    public ToolDurabilityService getDurabilityService() {
        return durabilityService;
    }

    public ToolUpgradeService getUpgradeService() {
        return upgradeService;
    }

    public ToolRecoveryService getRecoveryService() {
        return recoveryService;
    }

    public ToolRecoveryRepository getRecoveryRepository() {
        return recoveryRepository;
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public RecoveryPenaltyEngine getRecoveryPenaltyEngine() {
        return recoveryPenaltyEngine;
    }

    public RecoveryTransactionManager getRecoveryTransactionManager() {
        return recoveryTransactionManager;
    }

    public PickaxeRecoveryGui getRecoveryGui() {
        return recoveryGui;
    }

    public io.github.kaivian.kupdater.features.tools.common.service.ToolAdminService getAdminService() {
        return adminService;
    }

    public io.github.kaivian.kupdater.features.tools.common.service.ToolItemSynchronizer getItemSynchronizer() {
        return itemSynchronizer;
    }
}
