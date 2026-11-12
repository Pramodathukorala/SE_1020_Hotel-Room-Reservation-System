package com.hotel.reservation.model;

/**
 * The three room types. An enum is a fixed list of values, so no other
 * room type can ever exist. (Each value matches one Room subclass.)
 * OOP - ENCAPSULATION: displayName is private with a getter.
 */
public enum RoomType {
    STANDARD("Standard Room"),
    DELUXE("Deluxe Room"),
    SUITE("Suite");

    private final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
