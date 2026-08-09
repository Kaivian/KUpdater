package io.github.kaivian.kupdater.core.capability;

import io.github.kaivian.kupdater.api.capability.CapabilityRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe default implementation of CapabilityRegistry for optional service discovery.
 */
public class SimpleCapabilityRegistry implements CapabilityRegistry {

    private final Map<Class<?>, Object> capabilities = new ConcurrentHashMap<>();

    @Override
    public <T> void registerCapability(Class<T> capabilityClass, T implementation) {
        if (capabilityClass == null || implementation == null) {
            throw new IllegalArgumentException("Capability class and implementation cannot be null");
        }
        capabilities.put(capabilityClass, implementation);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getCapability(Class<T> capabilityClass) {
        if (capabilityClass == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) capabilities.get(capabilityClass));
    }

    @Override
    public <T> void unregisterCapability(Class<T> capabilityClass) {
        if (capabilityClass != null) {
            capabilities.remove(capabilityClass);
        }
    }

    public void clear() {
        capabilities.clear();
    }
}
