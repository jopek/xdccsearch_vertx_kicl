package com.lxbluem.filesystem.repository;

import rx.Observable;
import rx.Single;

import java.util.List;
import java.util.Optional;

public interface SimpleCrudRepository<T, ID> {

    /**
     * Save an entity to the persistence.
     *
     * @param entity entity object
     * @return an observable async result
     */
    Single<Void> save(T entity);

    /**
     * Save an entity to the persistence.
     *
     * @param entities list of entity objects
     * @return an observable async result
     */
    Single<Void> saveAll(List<T> entities);

    /**
     * Retrieve one certain entity by `id`.
     *
     * @param id id of the entity
     * @return an observable async result
     */
    Single<Optional<T>> retrieveOne(ID id);

    /**
     * Retrieve all entities.
     *
     * @return an observable async result
     */
    Observable<T> retrieveAll();

    /**
     * Delete the entity by `id`.
     *
     * @param id id of the entity
     * @return an observable async result
     */
    Single<Void> delete(ID id);

}
