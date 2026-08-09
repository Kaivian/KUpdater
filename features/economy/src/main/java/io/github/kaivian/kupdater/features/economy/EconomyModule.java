package io.github.kaivian.kupdater.features.economy;

import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;

/**
 * Economy module implementation.
 * Supported Minecraft version range: 1.12.0 to 26.2.0.
 */
public class EconomyModule extends AbstractKModule {

    public EconomyModule() {
        super(
                "economy",
                "Economy & Shops Module",
                "Currency, trade, and shop features.",
                ModuleCategory.ECONOMY,
                MinecraftVersion.of("1.12.0"),
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
