package io.github.kaivian.kupdater.features.combat;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Combat Mechanics module skeleton.
 */
public class CombatModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "combat";
    }

    @Override
    public String getName() {
        return "Combat Mechanics Module";
    }

    @Override
    public String getDescription() {
        return "Vanilla+ combat mechanics enhancements.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.COMBAT;
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
