package com.hotel.reservation.model;

/** The two kinds of user who can sign in: ADMIN or GUEST. (enum = fixed list of values) */
public enum Role {
    ADMIN("Administrator"),
    GUEST("Guest");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
