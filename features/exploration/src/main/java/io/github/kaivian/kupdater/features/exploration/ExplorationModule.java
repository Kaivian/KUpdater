package io.github.kaivian.kupdater.features.exploration;

import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;

/**
 * Exploration module implementation.
 * Supported Minecraft version range: 1.16.0 to 26.2.0.
 */
public class ExplorationModule extends AbstractKModule {

    public ExplorationModule() {
        super(
                "exploration",
                "Exploration & Structures Module",
                "Custom structures, biome rewards, and exploration stats.",
                ModuleCategory.EXPLORATION,
                MinecraftVersion.of("1.16.0"),
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
