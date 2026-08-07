# Module Development Guide

This guide explains how to create and register a new **KModule** for KUpdater.

---

## 1. Module Contract

Every module must implement `io.github.kaivian.kupdater.api.module.KModule`.

```java
package io.github.kaivian.kupdater.modules.sample;

import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.List;

public class SampleModule implements KModule, Listener {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "sample-module";
    }

    @Override
    public String getName() {
        return "Sample Module";
    }

    @Override
    public String getDescription() {
        return "Demonstrates KModule implementation structure.";
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.QOL;
    }

    @Override
    public List<String> getSupportedMinecraftVersions() {
        return List.of("26.2", "1.21.x");
    }

    @Override
    public void onEnable() {
        // Register listeners, commands, tasks
    }

    @Override
    public void onDisable() {
        // Unregister listeners, tasks, clean up resources
        HandlerList.unregisterAll(this);
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
```

---

## 2. Module Registration

Modules are registered in `KUpdater.java` during the enable phase:

```java
moduleManager.registerModule(new SampleModule());
```

---

## 3. Best Practices

* **EventListener Cleanup**: Always unregister listeners in `onDisable()` using `HandlerList.unregisterAll(listener)`.
* **Configuration Defaults**: Provide sane default values in configuration files.
* **Logging**: Use the logger passed from the main plugin instance rather than `System.out.println()`.
* **Immutability**: Keep internal state isolated and clean up on disable.
