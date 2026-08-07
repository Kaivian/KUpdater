# KUpdater

> A modular Minecraft gameplay enhancement plugin that improves vanilla gameplay while preserving the original Minecraft experience.

---

## Overview

**KUpdater** is designed around a single guiding principle: **"Enhance vanilla, don't replace it."**

Rather than introducing completely foreign game systems or overhauling core mechanics, KUpdater provides independent, modular gameplay enhancements that feel like natural extensions of the original Minecraft experience.

---

## Design Philosophy

* **Vanilla-First Design**: Every feature is designed to align with vanilla Minecraft aesthetics and progression mechanics.
* **Modular Architecture**: Modules are strictly decoupled. Administrators can enable, disable, and configure individual features without unnecessary bloat.
* **Administrator Control**: Granular configuration files empower server owners to tune mechanics or disable modules to fit their server's balance.
* **Backward Compatibility**: Core APIs insulate modules from aggressive version changes, allowing functionality to remain stable across server upgrades.
* **Multiplayer Safety**: Built from the ground up for multiplayer environments, ensuring anti-grief safety, exploit prevention, and performance safety.
* **Performance Focused**: Minimal overhead with optimized event listeners and zero unnecessary background ticking.

---

## Planned Modules

The table below outlines planned module categories for KUpdater. All gameplay modules are currently in the planning stage.

| Module | Description | Status |
| :--- | :--- | :--- |
| **Tools** | Extend vanilla tool progression and durability mechanics | Planned |
| **Combat** | Refine vanilla combat interactions and weapon dynamics | Planned |
| **Farming** | Agricultural progression, crop interactions, and QoL | Planned |
| **Mining** | Ore progression, excavation helpers, and mining QoL | Planned |
| **Economy** | Vanilla-compatible trade and economy mechanics | Planned |
| **Exploration**| World exploration incentives and navigational QoL | Planned |
| **QoL** | General quality-of-life enhancements for players | Planned |

---

## Modular Architecture

KUpdater utilizes an event-driven modular architecture:

```text
KUpdater Core
│
├── Module API (KModule, ModuleCategory)
├── Module Manager (Lifecycle & Registry)
├── Configuration System (ConfigManager)
├── Permission System
├── Messaging & Utilities
│
└── Gameplay Modules (Tools, Combat, Farming, Mining, Economy, Exploration, QoL)
```

Each module operates independently:
* Enforces its own configuration settings
* Registers isolated event listeners and commands
* Manages lifecycle hooks (`onEnable()`, `onDisable()`) cleanly

---

## Configuration

KUpdater provides administrator-configurable options for all core features and modules. Settings are loaded from `config.yml` and per-module configuration files located in the `plugins/KUpdater/modules/` directory.

---

## Minecraft Compatibility

* **Target API Version**: `26.2` (Paper API)
* **Java Requirement**: Java 25

KUpdater relies on stable Paper API interfaces to ensure long-term stability and compatibility across server updates without depending on version-locked internal NMS (net.minecraft.server) code.

---

## Building

### Prerequisites

* Java 25 Development Kit (JDK 25)
* Git

### Build Commands

Clone the repository and build the JAR artifact using the Gradle wrapper:

**Linux / macOS:**
```bash
./gradlew build
```

**Windows (PowerShell / Command Prompt):**
```cmd
.\gradlew.bat build
```

The compiled plugin JAR file will be generated in:
`build/libs/KUpdater-0.1.0-SNAPSHOT.jar`

---

## Development

Developers interested in contributing or creating new modules can refer to:
* [Module Development Guide](docs/modules/module-development.md)
* [Architectural Overview](docs/architecture/overview.md)
* [Development Environment Setup](docs/development/development-environment.md)

---

## Contributing

We welcome contributions! Please review [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, modular requirements, and pull request procedures.

---

## License

*Notice: A license decision is pending. Please refer to [LICENSE](LICENSE) or project repository releases once finalized.*
