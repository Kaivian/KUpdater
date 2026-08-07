# Contributing to KUpdater

Thank you for your interest in contributing to **KUpdater**! We welcome bug reports, documentation improvements, architectural feedback, and feature contributions.

---

## Core Philosophy

All contributions must align with our primary design principle:

> **"Enhance vanilla, don't replace it."**

Features should feel like natural extensions of Minecraft rather than introducing unrelated game mechanics or unnecessary bloat.

---

## Git Branch Strategy

KUpdater follows a structured branch strategy for production stability:

```text
main           Production-ready branch (releases tagged here)
develop        Integration branch for upcoming features
feature/*      New gameplay modules or core API features
fix/*          Bug fixes targeting issues in develop
refactor/*     Code structure improvements
docs/*         Documentation additions and edits
chore/*        Build script or repository maintenance
release/*      Release candidates branch (merges into main & develop)
hotfix/*       Critical fixes applied directly to production (main)
```

### Pull Request Workflow

```text
feature/*  ───►  develop  ───►  release/*  ───►  main
                                                  │
hotfix/*   ───────────────────────────────────────┴──► develop
```

1. **Feature & Bug Fixes**: Create a topic branch off `develop` (`feature/description` or `fix/description`). Submit PR targeting `develop`.
2. **Releases**: Create a `release/vX.Y.Z` branch off `develop`. When ready, merge into `main` and tag the release (`vX.Y.Z`). Back-merge into `develop`.
3. **Hotfixes**: Create a `hotfix/description` branch off `main`. When resolved, merge into `main` and back-merge into `develop`.

---

## Conventional Commits

We follow a lightweight Conventional Commit standard for clear commit history:

```text
<type>(<scope>): <short summary>
```

### Allowed Types
- `feat`: A new gameplay module, core API feature, or enhancement
- `fix`: A bug fix or error correction
- `refactor`: Code rewrite without functional changes
- `docs`: Documentation changes only
- `chore`: Maintenance tasks, dependency updates, configuration tweaks
- `ci`: GitHub Actions workflow or CI pipeline updates
- `build`: Gradle configuration or build script updates
- `perf`: Performance optimizations
- `test`: Adding or updating test coverage

### Examples
- `feat(tools): add unbreakable tool upgrade tier`
- `fix(core): resolve null pointer exception in ModuleManager`
- `docs(api): update module lifecycle guide`
- `ci(workflows): enforce strict status checks on main`

---

## Branch Protection & Review Requirements

All pull requests to `main` and `develop` must satisfy the following criteria before merging:

1. **Automated CI Check**: The `build` status check must pass (compilation, tests, and artifact verification).
2. **Code Review**: Requires at least 1 approving review from a reviewer other than the author.
3. **Up-to-Date Branch**: Branches must be rebased/updated with the target branch (`strict` status check).
4. **Resolved Conversations**: All pull request comments and review discussions must be resolved.
5. **Linear History**: Rebase or squash merge strategy is required; merge commits are restricted on production branches.
6. **No Force Pushes / No Deletions**: Enforced on both `main` and `develop`.

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

## Pull Request Checklist

Before submitting a pull request, ensure:

- [ ] Code compiles cleanly with `./gradlew build`
- [ ] Tests pass cleanly
- [ ] Commits follow Conventional Commit conventions
- [ ] Code complies with project formatting guidelines (`.editorconfig`)
- [ ] No hardcoded NMS dependencies exist
- [ ] Feature is configurable by server administrators
- [ ] PR description specifies the module affected and testing performed
- [ ] Documentation is updated if necessary
