# Minecraft Version Compatibility Strategy

This document outlines KUpdater's versioning strategy and compatibility design.

---

## Target Version

* **Current Paper API Target**: `26.2`
* **Current Java Target**: `25`

---

## Multi-Version Compatibility Strategy

1. **Paper API First**: KUpdater builds exclusively against stable Paper API interfaces. We avoid relying on `net.minecraft.server` (NMS) or internal obfuscated classes.
2. **Version Abstraction**: If a feature requires version-dependent mechanics (e.g. custom packets or complex NMS interactions in the future), the implementation must be hidden behind a version-agnostic interface.
3. **Graceful Degradation**: If a module encounters an unsupported Minecraft version at runtime, it should log a clear warning and disable itself without breaking the core plugin or other modules.
