package io.github.kaivian.kupdater.features.exploration;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Exploration & World module skeleton.
 */
public class ExplorationModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "exploration";
    }

    @Override
    public String getName() {
        return "Exploration Module";
    }

    @Override
    public String getDescription() {
        return "World exploration and adventure enhancements.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.EXPLORATION;
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
