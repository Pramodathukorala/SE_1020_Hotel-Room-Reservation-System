package com.hotel.reservation.repository;

import com.hotel.reservation.model.AdminAccount;

import java.util.Optional;

/** Admin logins, stored separately from guests. */
// OOP - INHERITANCE: this interface extends DataRepository<AdminAccount> and adds its own search methods
public interface AdminRepository extends DataRepository<AdminAccount> {

    Optional<AdminAccount> findByUsername(String username);
}
