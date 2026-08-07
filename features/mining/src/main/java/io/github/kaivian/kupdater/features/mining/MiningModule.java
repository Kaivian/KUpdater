package io.github.kaivian.kupdater.features.mining;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Mining Progression module skeleton.
 */
public class MiningModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "mining";
    }

    @Override
    public String getName() {
        return "Mining Module";
    }

    @Override
    public String getDescription() {
        return "Mining progression and quality of life enhancements.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.MINING;
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
