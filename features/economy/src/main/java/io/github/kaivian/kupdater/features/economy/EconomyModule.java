package io.github.kaivian.kupdater.features.economy;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Economy Enhancements module skeleton.
 */
public class EconomyModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "economy";
    }

    @Override
    public String getName() {
        return "Economy Module";
    }

    @Override
    public String getDescription() {
        return "Economy enhancements and trade progression.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.ECONOMY;
    }

    @Override
    public List<String> getSupportedMinecraftVersions() {
        return Collections.singletonList("1.21.x");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
