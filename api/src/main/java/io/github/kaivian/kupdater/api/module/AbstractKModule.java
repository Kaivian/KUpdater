package io.github.kaivian.kupdater.api.module;

import io.github.kaivian.kupdater.api.version.MinecraftVersion;

import java.util.Collections;
import java.util.List;

/**
 * Base abstract class for KUpdater gameplay modules.
 */
public abstract class AbstractKModule implements KModule {

    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final MinecraftVersion minVersion;
    private final MinecraftVersion maxVersion;

    private boolean enabled;
    private ModuleState lifecycleState = ModuleState.DISCOVERED;

    protected AbstractKModule(String id, String name, String description, ModuleCategory category) {
        this(id, name, description, category, MinecraftVersion.V1_8_8, MinecraftVersion.V26_2);
    }

    protected AbstractKModule(String id, String name, String description, ModuleCategory category, MinecraftVersion minVersion, MinecraftVersion maxVersion) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public List<String> getSupportedMinecraftVersions() {
        return Collections.singletonList(minVersion.getRawVersion() + " - " + maxVersion.getRawVersion());
    }

    @Override
    public MinecraftVersion getMinimumSupportedVersion() {
        return minVersion;
    }

    @Override
    public MinecraftVersion getMaximumSupportedVersion() {
        return maxVersion;
    }

    @Override
    public ModuleState getLifecycleState() {
        return lifecycleState;
    }

    @Override
    public void setLifecycleState(ModuleState state) {
        this.lifecycleState = state;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            this.lifecycleState = ModuleState.ENABLED;
        } else if (this.lifecycleState == ModuleState.ENABLED || this.lifecycleState == ModuleState.DISABLING) {
            this.lifecycleState = ModuleState.DISABLED;
        }
    }
}
