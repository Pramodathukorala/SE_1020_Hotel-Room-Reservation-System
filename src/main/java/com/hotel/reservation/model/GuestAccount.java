package com.hotel.reservation.model;

import java.time.LocalDateTime;

/**
 * The login of a registered guest. Stored in the "guest_accounts" table.
 *
 * Its ID is the same as the customer's ID (one account per customer), so the account
 * only holds the password hash - name, email, phone and address stay in the Customer
 * record. The guest signs in with their customer email address.
 */
public class GuestAccount extends UserAccount {   // OOP - INHERITANCE: GuestAccount IS-A UserAccount

    public GuestAccount(String customerId, String passwordHash, LocalDateTime createdAt) {
        super(customerId, passwordHash, createdAt);   // OOP - INHERITANCE: parent constructor
    }

    @Override   // OOP - POLYMORPHISM: a guest's role
    public Role getRole() {
        return Role.GUEST;
    }

    @Override   // OOP - POLYMORPHISM: guests go to "My bookings" after signing in
    public String getHomePage() {
        return "/my-bookings";
    }

    public String getCustomerId() {
        return getId();
    }
}
