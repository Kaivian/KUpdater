package io.github.kaivian.kupdater.features.progression;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Progression & Achievements module skeleton.
 */
public class ProgressionModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "progression";
    }

    @Override
    public String getName() {
        return "Progression Module";
    }

    @Override
    public String getDescription() {
        return "Overall gameplay progression and achievement framework.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.PROGRESSION;
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
