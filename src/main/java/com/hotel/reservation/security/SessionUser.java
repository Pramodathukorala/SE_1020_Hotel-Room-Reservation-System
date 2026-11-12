package com.hotel.reservation.security;

import com.hotel.reservation.model.Role;

import java.io.Serializable;

/**
 * The small, read-only piece of information kept in the HTTP session after a successful login.
 * It never contains the password or its hash.
 *
 * @param accountId   admin ID (e.g. A001) or, for a guest, the customer ID (e.g. C004)
 * @param displayName name shown in the navigation bar
 * @param role        ADMIN or GUEST
 */
public record SessionUser(String accountId, String displayName, Role role) implements Serializable {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public String firstName() {
        int space = displayName.indexOf(' ');
        return space > 0 ? displayName.substring(0, space) : displayName;
    }
}
