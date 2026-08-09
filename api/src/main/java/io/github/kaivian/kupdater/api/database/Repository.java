package io.github.kaivian.kupdater.api.database;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base generic repository contract for KUpdater entities.
 * Feature modules can extend this or define specialized repositories.
 *
 * @param <T>  Entity type
 * @param <ID> Entity identifier type
 */
public interface Repository<T, ID> {

    /**
     * Finds an entity by its identifier.
     *
     * @param id identifier
     * @return Optional containing entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all stored entities.
     *
     * @return list of entities
     */
    List<T> findAll();

    /**
     * Saves or updates an entity.
     *
     * @param entity entity to save
     * @return saved entity
     */
    T save(T entity);

    /**
     * Deletes an entity by its identifier.
     *
     * @param id entity identifier
     */
    void deleteById(ID id);

    /**
     * Asynchronously finds an entity by its identifier.
     *
     * @param id entity identifier
     * @return CompletableFuture containing Optional entity
     */
    default CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> findById(id));
    }

    /**
     * Asynchronously saves or updates an entity.
     *
     * @param entity entity to save
     * @return CompletableFuture containing saved entity
     */
    default CompletableFuture<T> saveAsync(T entity) {
        return CompletableFuture.supplyAsync(() -> save(entity));
    }
}
