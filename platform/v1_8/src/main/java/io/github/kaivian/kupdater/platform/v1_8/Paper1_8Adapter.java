package io.github.kaivian.kupdater.platform.v1_8;

import io.github.kaivian.kupdater.api.platform.VersionAdapter;

/**
 * Platform adapter implementation for legacy 1.8.8 - 1.12.2 server platforms.
 */
public class Paper1_8Adapter implements VersionAdapter {

    @Override
    public String getTargetVersion() {
        return "1.8.8-1.12.2";
    }

    @Override
    public boolean isCompatible(String serverVersion) {
        if (serverVersion == null) {
            return false;
        }
        return serverVersion.contains("1.8") || serverVersion.contains("1.9")
                || serverVersion.contains("1.10") || serverVersion.contains("1.11") || serverVersion.contains("1.12");
    }
}
