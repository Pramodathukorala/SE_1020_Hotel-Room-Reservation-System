package com.hotel.reservation.repository;

import com.hotel.reservation.model.GuestAccount;

/**
 * Guest logins. The account ID is the customer ID, so the inherited
 * findById(customerId) finds a customer's account.
 */
// OOP - INHERITANCE: this interface extends DataRepository<GuestAccount> and adds its own search methods
public interface GuestAccountRepository extends DataRepository<GuestAccount> {
}
