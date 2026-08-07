package io.github.kaivian.kupdater.features.tools;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;

import java.util.Collections;
import java.util.List;

/**
 * Tools & Equipment Progression module skeleton.
 */
public class ToolsModule implements KModule {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "tools";
    }

    @Override
    public String getName() {
        return "Tools Progression Module";
    }

    @Override
    public String getDescription() {
        return "Tools and equipment progression enhancements.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.TOOLS;
    }

    @Override
    public List<String> getSupportedMinecraftVersions() {
        return Collections.singletonList("1.21.x");
    }

    @Override
    public void onEnable() {
        // Module enablement architecture hook
    }

    @Override
    public void onDisable() {
        // Module disablement architecture hook
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
