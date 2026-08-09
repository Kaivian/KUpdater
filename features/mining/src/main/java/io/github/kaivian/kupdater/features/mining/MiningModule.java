package io.github.kaivian.kupdater.features.mining;

import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;

/**
 * Mining module implementation.
 * Supported Minecraft version range: 1.8.8 to 26.2.0.
 */
public class MiningModule extends AbstractKModule {

    public MiningModule() {
        super(
                "mining",
                "Mining & Ores Module",
                "Custom mining drops and vein mining mechanics.",
                ModuleCategory.MINING,
                MinecraftVersion.V1_8_8,
                MinecraftVersion.V26_2
        );
    }

    @Override
    public void onEnable() {
        // Module enablement logic
    }

    @Override
    public void onDisable() {
        // Module disablement logic
    }
}
