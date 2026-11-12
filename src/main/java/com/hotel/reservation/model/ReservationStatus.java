package com.hotel.reservation.model;

/**
 * Life-cycle states of a reservation.
 * Each constant knows whether it occupies the room, so availability rules
 * do not need scattered if-statements throughout the code.
 */
public enum ReservationStatus {
    CONFIRMED("Confirmed", true),
    CANCELLED("Cancelled", false);

    private final String displayName;
    private final boolean blocksAvailability;

    ReservationStatus(String displayName, boolean blocksAvailability) {
        this.displayName = displayName;
        this.blocksAvailability = blocksAvailability;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** True when a reservation in this state prevents others from booking the same nights. */
    public boolean blocksAvailability() {
        return blocksAvailability;
    }
}
