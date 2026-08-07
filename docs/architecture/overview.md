# Architectural Overview

This document describes the high-level architecture of **KUpdater**.

---

## High-Level Architecture

KUpdater is built as a lightweight core framework that manages independent, decoupled gameplay modules.

```text
                     +----------------------+
                     |   Bukkit / Paper     |
                     +----------+-----------+
                                |
                     +----------v-----------+
                     |    KUpdater Main     |
                     +----+-----------+-----+
                          |           |
            +-------------+           +-------------+
            |                                       |
+-----------v------------+             +------------v-----------+
|     ConfigManager      |             |     ModuleManager      |
+------------------------+             +------------+-----------+
                                                    |
                                       +------------v-----------+
                                       |     KModule Interface  |
                                       +-----+-------------+----+
                                             |             |
                                  +----------v---+     +---v----------+
                                  | ToolsModule  |     | CombatModule |
                                  +--------------+     +--------------+
```

---

## Core Components

### 1. Main Plugin (`KUpdater.java`)
Entry point for the Paper plugin. Responsibilities:
* Bootstrapping `ConfigManager` and `ModuleManager`.
* Triggering module lifecycle handlers during plugin enable/disable phases.

### 2. Configuration Manager (`ConfigManager.java`)
Handles plugin configuration files:
* Core configuration loading (`config.yml`).
* Directory routing for module-specific configuration files (`plugins/KUpdater/modules/<module-id>/`).

### 3. Module Manager (`ModuleManager.java`)
Registry and lifecycle controller for all `KModule` instances:
* Registering modules during initialization.
* Enabling/disabling registered modules safely.
* Querying modules by ID or `ModuleCategory`.

### 4. Module API (`KModule.java` & `ModuleCategory.java`)
Abstract contract defining how gameplay features interface with KUpdater:
* Unique identifier and display name.
* Category mapping.
* Lifecycle hooks (`onEnable()`, `onDisable()`).
* Dynamic status toggling (`isEnabled()`).

---

## Design Constraints

* **Strict Decoupling**: Modules must operate independently without hard dependencies on other gameplay modules.
* **No Direct NMS**: Avoid accessing internal `net.minecraft.server` code directly. Use standard Paper API abstractions.
* **Fail-Safe Isolation**: An error during a module's execution or enablement should not crash the entire plugin or prevent other modules from functioning.
