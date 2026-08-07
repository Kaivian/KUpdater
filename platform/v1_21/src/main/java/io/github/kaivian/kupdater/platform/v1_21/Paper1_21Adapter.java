package io.github.kaivian.kupdater.platform.v1_21;

import io.github.kaivian.kupdater.api.platform.VersionAdapter;

/**
 * Platform adapter implementation for Paper 1.21.x / 26.x server platforms.
 */
public class Paper1_21Adapter implements VersionAdapter {

    @Override
    public String getTargetVersion() {
        return "1.21.x";
    }

    @Override
    public boolean isCompatible(String serverVersion) {
        if (serverVersion == null) {
            return false;
        }
        return serverVersion.contains("1.21") || serverVersion.contains("26.");
    }
}
