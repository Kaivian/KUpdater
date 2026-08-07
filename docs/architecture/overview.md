# Architectural Overview

This document describes the high-level multi-module architecture of **KUpdater**.

For full architectural guidelines, dependency matrices, multi-version strategies, and extension guides, see [architecture.md](../architecture.md).

---

## High-Level Multi-Module Hierarchy

KUpdater is built as a modular framework with clear layer separation across Gradle subprojects.

```text
KUpdater
├── api                     (:api)
│   └── Public contracts (KModule, ModuleCategory, VersionAdapter)
├── core                    (:core)
│   └── Shared infrastructure (ModuleManager, ConfigManager)
├── platform                (:platform)
│   ├── common              (:platform:common - PlatformManager)
│   └── v1_21               (:platform:v1_21 - Paper1_21Adapter)
├── features                (:features)
│   ├── tools               (:features:tools - ToolsModule)
│   ├── progression         (:features:progression - ProgressionModule)
│   ├── combat              (:features:combat - CombatModule)
│   ├── farming             (:features:farming - FarmingModule)
│   ├── mining              (:features:mining - MiningModule)
│   ├── economy             (:features:economy - EconomyModule)
│   └── exploration         (:features:exploration - ExplorationModule)
└── bootstrap               (:bootstrap)
    └── Assembly & plugin entrypoint (KUpdaterPlugin, plugin.yml)
```

---

## Core Principles & Design Constraints

* **Strict Subproject Isolation**: Features operate as independent Gradle subprojects without depending on other features or version-specific implementations.
* **Version Compatibility Abstraction**: Platform/version-dependent code is isolated in `:platform:*` adapters.
* **Fail-Safe Lifecycle**: An exception during one module's execution will not crash the plugin or prevent other modules from enabling.
* **Single Shaded Artifact**: `:bootstrap` bundles all subprojects into a single deployable JAR output (`KUpdater-<version>.jar`).
