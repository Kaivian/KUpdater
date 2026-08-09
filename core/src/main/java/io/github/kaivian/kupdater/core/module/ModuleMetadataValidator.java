package io.github.kaivian.kupdater.core.module;

import io.github.kaivian.kupdater.api.module.KModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates module metadata before module lifecycle initialization.
 */
public final class ModuleMetadataValidator {

    private ModuleMetadataValidator() {
    }

    /**
     * Validates a collection of modules.
     *
     * @param modules modules to validate
     * @return list of validation error strings (empty if valid)
     */
    public static List<String> validate(Collection<KModule> modules) {
        List<String> errors = new ArrayList<>();
        if (modules == null) {
            return errors;
        }

        Set<String> seenIds = new HashSet<>();

        for (KModule module : modules) {
            if (module == null) {
                errors.add("Encountered null module instance");
                continue;
            }

            if (module.getId() == null || module.getId().trim().isEmpty()) {
                errors.add("Module " + module.getName() + " has null or empty ID");
            } else if (!seenIds.add(module.getId())) {
                errors.add("Duplicate module ID detected: '" + module.getId() + "'");
            }

            if (module.getName() == null || module.getName().trim().isEmpty()) {
                errors.add("Module ID '" + module.getId() + "' has null or empty display name");
            }

            try {
                if (module.getMinimumSupportedVersion().compareTo(module.getMaximumSupportedVersion()) > 0) {
                    errors.add("Module '" + module.getId() + "' has minimum version ("
                            + module.getMinimumSupportedVersion() + ") greater than maximum version ("
                            + module.getMaximumSupportedVersion() + ")");
                }
            } catch (Exception e) {
                errors.add("Module '" + module.getId() + "' version range evaluation threw exception: " + e.getMessage());
            }
        }

        return errors;
    }
}
