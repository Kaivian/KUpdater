# KUpdater Architecture Documentation

This document describes the modular architecture of **KUpdater**, detailing subproject responsibilities, dependency direction rules, multi-version abstractions, and extension guides for future developers.

---

## 1. Project Module Overview

KUpdater uses a multi-module Gradle structure to ensure strict layer separation, version independence, and feature module isolation.

```text
KUpdater
├── api                     (:api)
│   ├── module/             (KModule interface, ModuleCategory enum)
│   └── platform/           (VersionAdapter interface)
│
├── core                    (:core)
│   ├── config/             (ConfigManager)
│   └── module/             (ModuleManager)
│
├── platform                (:platform)
│   ├── common/             (:platform:common - PlatformManager)
│   └── v1_21/              (:platform:v1_21 - Paper1_21Adapter)
│
├── features                (:features)
│   ├── tools/              (:features:tools)
│   ├── progression/        (:features:progression)
│   ├── combat/             (:features:combat)
│   ├── farming/            (:features:farming)
│   ├── mining/             (:features:mining)
│   ├── economy/            (:features:economy)
│   └── exploration/        (:features:exploration)
│
└── bootstrap               (:bootstrap)
    ├── KUpdaterPlugin.java (Main JavaPlugin entrypoint)
    └── plugin.yml          (Paper plugin manifest)
```

---

## 2. Dependency Direction Rules

Dependencies follow strict single-direction flow rules to prevent tight coupling and circular dependencies.

```text
                             +----------------+
                             |   paper-api    | (compileOnly across all modules)
                             +-------+--------+
                                     |
                             +-------v--------+
                             |      :api      |
                             +---+-------+----+
                                 |       |
          +----------------------+       +----------------------+
          |                                                     |
  +-------v--------+                                    +-------v--------+
  |     :core      |                                    | :platform:common|
  +-------+--------+                                    +-------+--------+
          |                                                     |
          +----------------+                   +----------------+
          |                |                   |
  +-------v--------+ +-----v----------+ +------v---------+
  | :features:*    | | :features:*    | | :platform:v1_21|
  | (e.g. :tools)  | | (e.g. :combat) | +------+---------+
  +-------+--------+ +-----+----------+        |
          |                |                   |
          +----------------+-------------------+
                           |
                   +-------v--------+
                   |   :bootstrap   | (Shaded Final Jar)
                   +----------------+
```

### Layer Rules:

1. **`api`**: Public contracts only. Depends on NOTHING inside KUpdater.
2. **`core`**: Infrastructure (`ModuleManager`, `ConfigManager`). Depends on `:api`.
3. **`platform:common`**: Version manager and shared platform interfaces. Depends on `:api`.
4. **`platform:<version>`**: Version-specific implementation (e.g. `:platform:v1_21`). Depends on `:platform:common` and `:api`.
5. **`features:<feature>`**: Gameplay modules implementing `KModule`. Depends on `:api` and `:core`. **MUST NOT depend on other feature modules or version-specific platform modules.**
6. **`bootstrap`**: JavaPlugin assembly entry point. Depends on `:api`, `:core`, `:platform:common`, `:platform:v1_21`, and all `:features:*` modules.

---

## 3. Layer Responsibilities & Allowed Code

| Subproject | Allowed Code | Forbidden Code |
| :--- | :--- | :--- |
| **`:api`** | Public interfaces, contracts, enums. | Concrete implementation logic, state, heavy dependencies. |
| **`:core`** | Module registry, configuration managers, shared utility code. | Gameplay mechanics, event listeners, commands, version-specific NMS/Paper code. |
| **`:platform:common`** | Version-agnostic platform manager, version adapter interfaces. | Version-specific NMS, direct Bukkit version string checks scattered in code. |
| **`:platform:<version>`** | Version-specific adapters (`Paper1_21Adapter`). | Cross-module feature logic. |
| **`:features:<name>`** | Independent gameplay module implementation (e.g. `ToolsModule implements KModule`). | Direct dependencies on other feature modules or specific platform version adapters. |
| **`:bootstrap`** | `JavaPlugin` lifecycle entry point, plugin manifest (`plugin.yml`), artifact packaging. | Business logic that belongs in features or core. |

---

## 4. Multi-Version Compatibility Strategy

Version compatibility is handled using an **Adapter Pattern**:

- Version-independent code (Core and Features) interacts with platform capabilities solely through `VersionAdapter` in `:platform:common`.
- Core/Feature code NEVER performs `if (version.equals("1.21"))` checks.

### How to Add Support for a New Minecraft Version (e.g., 1.22):

1. Create a new subproject directory `platform/v1_22`.
2. Add `platform/v1_22/build.gradle.kts` referencing Paper 1.22 dependencies:
   ```kotlin
   dependencies {
       implementation(project(":platform:common"))
       api(project(":api"))
       compileOnly(libs.paper.api)
   }
   ```
3. Create `Paper1_22Adapter implements VersionAdapter` in `io.github.kaivian.kupdater.platform.v1_22`.
4. Register the new subproject in `settings.gradle.kts`:
   ```kotlin
   include("platform:v1_22")
   ```
5. Add `implementation(project(":platform:v1_22"))` to `bootstrap/build.gradle.kts`.
6. Register `new Paper1_22Adapter()` in `KUpdaterPlugin.java`.

*No changes are required in `:api`, `:core`, or any `:features:*` module.*

---

## 5. How to Add a New Feature Module (e.g., `magic`):

1. Create directory `features/magic`.
2. Create `features/magic/build.gradle.kts`:
   ```kotlin
   dependencies {
       api(project(":api"))
       implementation(project(":core"))
       compileOnly(libs.paper.api)
   }
   ```
3. Add `include("features:magic")` to `settings.gradle.kts`.
4. Add `implementation(project(":features:magic"))` to `bootstrap/build.gradle.kts`.
5. Create class `MagicModule implements KModule` in `io.github.kaivian.kupdater.features.magic`.
6. Register `new MagicModule()` in `KUpdaterPlugin.java`.
