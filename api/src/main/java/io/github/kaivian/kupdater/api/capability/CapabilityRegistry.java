package io.github.kaivian.kupdater.api.capability;

import java.util.Optional;

/**
 * Interface for optional cross-module capability and service registration/discovery.
 * Modules register capabilities they expose and discover optional capabilities provided by core or other modules.
 */
public interface CapabilityRegistry {

    /**
     * Registers a capability service implementation.
     *
     * @param capabilityClass capability interface or class
     * @param implementation implementation instance
     * @param <T> capability type
     */
    <T> void registerCapability(Class<T> capabilityClass, T implementation);

    /**
     * Retrieves an optional capability service if available.
     *
     * @param capabilityClass capability interface or class
     * @param <T> capability type
     * @return Optional containing the capability implementation if registered
     */
    <T> Optional<T> getCapability(Class<T> capabilityClass);

    /**
     * Unregisters a capability service.
     *
     * @param capabilityClass capability interface or class
     * @param <T> capability type
     */
    <T> void unregisterCapability(Class<T> capabilityClass);
}
