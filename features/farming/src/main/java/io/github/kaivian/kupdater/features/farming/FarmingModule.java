package io.github.kaivian.kupdater.features.farming;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Farming & Agriculture module skeleton.
 */
public class FarmingModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "farming";
    }

    @Override
    public String getName() {
        return "Farming Module";
    }

    @Override
    public String getDescription() {
        return "Farming and agriculture enhancements.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.FARMING;
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
