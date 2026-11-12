package com.hotel.reservation.model;

/**
 * The three room categories offered by the hotel.
 * An enum guarantees that only valid room types can exist in the system.
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
