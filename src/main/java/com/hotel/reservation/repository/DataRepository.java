package com.hotel.reservation.repository;

import com.hotel.reservation.model.Identifiable;

import java.util.List;
import java.util.Optional;

/**
 * OOP - ABSTRACTION (interface): the list of database operations every repository has
 *       (find, save, delete, count...). It says WHAT can be done, not HOW.
 * <T> = generics: the same interface works for Room, Customer, Reservation, ...
 *
 * INFORMATION HIDING: services talk to this interface only.
 * They do not know (or care) that records are stored in a MySQL database -
 * the implementation (repository.jdbc) could be replaced without changing any service code.
 * (This project first stored data in text files; switching to SQL did not change a single service.)
 *
 * @param <T> the entity type stored by the repository
 */
public interface DataRepository<T extends Identifiable> {

    List<T> findAll();

    Optional<T> findById(String id);

    boolean existsById(String id);

    /** Inserts a new record, or updates the record with the same ID. */
    T save(T entity);

    /** Removes the record. Returns false if the ID did not exist. */
    boolean deleteById(String id);

    long count();

    /** Generates the next unused ID, e.g. R007. */
    String nextId();
}
