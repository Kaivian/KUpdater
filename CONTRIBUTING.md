# Contributing to KUpdater

Thank you for your interest in contributing to **KUpdater**! We welcome bug reports, documentation improvements, architectural feedback, and feature contributions.

---

## Core Philosophy

All contributions must align with our primary design principle:

> **"Enhance vanilla, don't replace it."**

Features should feel like natural extensions of Minecraft rather than introducing unrelated game mechanics or unnecessary bloat.

---

## Modular Architecture Guidelines

When developing features for KUpdater:

1. **Keep Modules Independent**: Modules must not directly depend on or tightly couple with other gameplay modules.
2. **Use Core APIs**: Interact with server resources through `KModule`, `ModuleManager`, and `ConfigManager`.
3. **Avoid NMS**: Use stable Paper API methods wherever possible. Avoid importing internal server implementation code (`net.minecraft.server`).
4. **Administrator Control**: All new features must expose configuration options so server administrators can toggle or tune them.
5. **Clean Lifecycle**: Ensure all listeners, tasks, and memory structures are properly unregistered or cleared in `onDisable()`.

---

## Development Setup

1. **Prerequisites**: JDK 25 installed and configured.
2. **Clone & Build**:
   ```bash
   git clone https://github.com/Kaivian/KUpdater.git
   cd KUpdater
   ./gradlew build
   ```
3. **IDE Import**: Open the root directory in IntelliJ IDEA or VS Code as a Gradle project.

---

## Creating a New Module

For a step-by-step walkthrough on creating independent modules, please read the [Module Development Guide](docs/modules/module-development.md).

Summary of steps:
1. Implement the `io.github.kaivian.kupdater.api.module.KModule` interface.
2. Assign a unique module ID and `ModuleCategory`.
3. Register your module in `ModuleManager`.
4. Add default configuration values.

---

## Pull Request Checklist

Before submitting a pull request, ensure:

- [ ] Code compiles cleanly with `./gradlew build`
- [ ] Code complies with project formatting guidelines (`.editorconfig`)
- [ ] No hardcoded NMS dependencies exist
- [ ] Feature is configurable by server administrators
- [ ] PR description specifies the module affected and testing performed
- [ ] Documentation is updated if necessary
