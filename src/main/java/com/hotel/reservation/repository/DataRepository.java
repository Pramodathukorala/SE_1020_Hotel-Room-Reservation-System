package com.hotel.reservation.repository;

import com.hotel.reservation.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * INFORMATION HIDING: services talk to this interface only.
 * They do not know (or care) that records are stored in text files -
 * the storage could be replaced by a database without changing any service code.
 *
 * @param <T> the entity type stored by the repository
 */
public interface DataRepository<T extends Identifiable> {

    List<T> findAll();

    Optional<T> findById(String id);

    boolean existsById(String id);

    /** Inserts a new record or replaces the record with the same ID, then persists. */
    T save(T entity);

    /** Removes the record and persists. Returns false if the ID did not exist. */
    boolean deleteById(String id);

    long count();

    /** Generates the next unused ID, e.g. R007. */
    String nextId();
}
