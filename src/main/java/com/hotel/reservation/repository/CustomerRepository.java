package com.hotel.reservation.repository;

import com.hotel.reservation.model.Customer;

import java.util.Optional;

/** Customer-specific queries on top of the generic CRUD operations. */
// OOP - INHERITANCE: this interface extends DataRepository<Customer> and adds its own search methods
public interface CustomerRepository extends DataRepository<Customer> {

    Optional<Customer> findByEmail(String email);
}
