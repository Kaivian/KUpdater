package io.github.kaivian.kupdater.testing.unit;

import io.github.kaivian.kupdater.api.capability.CapabilityRegistry;
import io.github.kaivian.kupdater.api.module.AbstractKModule;
import io.github.kaivian.kupdater.api.module.KModule;
import io.github.kaivian.kupdater.api.module.ModuleCategory;
import io.github.kaivian.kupdater.api.module.ModuleState;
import io.github.kaivian.kupdater.api.version.MinecraftVersion;
import io.github.kaivian.kupdater.api.version.MinecraftVersionRange;
import io.github.kaivian.kupdater.core.capability.SimpleCapabilityRegistry;
import io.github.kaivian.kupdater.core.module.ModuleMetadataValidator;
import io.github.kaivian.kupdater.testing.matrix.CompatibilityMatrix;
import io.github.kaivian.kupdater.testing.model.FailureCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MinecraftVersionTest {

    @Test
    @DisplayName("Should parse major, minor, patch version numbers correctly")
    void testVersionParsing() {
        MinecraftVersion v1 = MinecraftVersion.of("1.8.8");
        assertEquals(1, v1.getMajor());
        assertEquals(8, v1.getMinor());
        assertEquals(8, v1.getPatch());

        MinecraftVersion v2 = MinecraftVersion.of("1.21.4");
        assertEquals(1, v2.getMajor());
        assertEquals(21, v2.getMinor());
        assertEquals(4, v2.getPatch());

        MinecraftVersion v3 = MinecraftVersion.of("26.2");
        assertEquals(26, v3.getMajor());
        assertEquals(2, v3.getMinor());
        assertEquals(0, v3.getPatch());
    }

    @Test
    @DisplayName("Should compare versions correctly")
    void testVersionComparison() {
        MinecraftVersion v1_8 = MinecraftVersion.of("1.8.8");
        MinecraftVersion v1_12 = MinecraftVersion.of("1.12.2");
        MinecraftVersion v1_20 = MinecraftVersion.of("1.20.4");
        MinecraftVersion v1_21 = MinecraftVersion.of("1.21.4");
        MinecraftVersion v26 = MinecraftVersion.of("26.2.0");

        assertTrue(v1_8.compareTo(v1_12) < 0);
        assertTrue(v1_12.compareTo(v1_20) < 0);
        assertTrue(v1_20.compareTo(v1_21) < 0);
        assertTrue(v1_21.compareTo(v26) < 0);
        assertEquals(0, v1_21.compareTo(MinecraftVersion.of("1.21.4")));
    }

    @Test
    @DisplayName("Should evaluate version range inclusion correctly")
    void testVersionRange() {
        MinecraftVersionRange range = MinecraftVersionRange.of("1.12.0", "26.2.0");

        assertTrue(range.contains("1.12.0"));
        assertTrue(range.contains("1.20.4"));
        assertTrue(range.contains("1.21.4"));
        assertTrue(range.contains("26.2.0"));
        assertFalse(range.contains("1.8.8"));
        assertFalse(range.contains("27.0.0"));
    }

    @Test
    @DisplayName("Should validate metadata and detect invalid version ranges or duplicate IDs")
    void testMetadataValidator() {
        KModule validModule = new AbstractKModule("tools", "Tools", "Desc", ModuleCategory.TOOLS,
                MinecraftVersion.of("1.12.0"), MinecraftVersion.of("26.2.0")) {
            @Override public void onEnable() {}
            @Override public void onDisable() {}
        };

        KModule invalidModule = new AbstractKModule("broken", "Broken", "Desc", ModuleCategory.COMBAT,
                MinecraftVersion.of("1.21.0"), MinecraftVersion.of("1.12.0")) {
            @Override public void onEnable() {}
            @Override public void onDisable() {}
        };

        KModule duplicateModule = new AbstractKModule("tools", "Tools Duplicate", "Desc", ModuleCategory.TOOLS) {
            @Override public void onEnable() {}
            @Override public void onDisable() {}
        };

        List<String> errors = ModuleMetadataValidator.validate(Arrays.asList(validModule, invalidModule, duplicateModule));
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("greater than maximum version")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Duplicate module ID")));
    }

    @Test
    @DisplayName("Should test capability registry registration and discovery")
    void testCapabilityRegistry() {
        CapabilityRegistry registry = new SimpleCapabilityRegistry();
        DummyService service = new DummyService();

        registry.registerCapability(DummyService.class, service);
        Optional<DummyService> discovered = registry.getCapability(DummyService.class);

        assertTrue(discovered.isPresent());
        assertEquals(service, discovered.get());

        registry.unregisterCapability(DummyService.class);
        assertFalse(registry.getCapability(DummyService.class).isPresent());
    }

    @Test
    @DisplayName("Should verify failure category descriptions and matrix levels")
    void testFailureCategoriesAndMatrix() {
        assertEquals("Unit test assertion failure", FailureCategory.UNIT_FAILURE.getDescription());

        CompatibilityMatrix matrix = CompatibilityMatrix.loadDefault();
        assertEquals("1.21.4", matrix.getPrimaryVersion());
        assertEquals("1.8.8", matrix.getOldestSupportedVersion());
        assertEquals("1.21.4", matrix.getLatestSupportedVersion());

        List<String> smokeVersions = matrix.resolveLevel("smoke");
        assertEquals(3, smokeVersions.size());
        assertTrue(smokeVersions.contains("1.8.8"));
        assertTrue(smokeVersions.contains("1.21.4"));
    }

    private static class DummyService {}
}
