package io.github.kaivian.kupdater.platform.v1_13;

import io.github.kaivian.kupdater.api.platform.VersionAdapter;

/**
 * Platform adapter implementation for 1.13 - 1.20.x server platforms.
 */
public class Paper1_13Adapter implements VersionAdapter {

    @Override
    public String getTargetVersion() {
        return "1.13-1.20.x";
    }

    @Override
    public boolean isCompatible(String serverVersion) {
        if (serverVersion == null) {
            return false;
        }
        return serverVersion.contains("1.13") || serverVersion.contains("1.14") || serverVersion.contains("1.15")
                || serverVersion.contains("1.16") || serverVersion.contains("1.17") || serverVersion.contains("1.18")
                || serverVersion.contains("1.19") || serverVersion.contains("1.20");
    }
}
