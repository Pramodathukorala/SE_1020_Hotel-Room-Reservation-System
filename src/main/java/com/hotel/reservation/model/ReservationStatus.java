package com.hotel.reservation.model;

/**
 * The possible states of a booking: CONFIRMED or CANCELLED.
 * Each value knows if it blocks the room (CONFIRMED = yes, CANCELLED = no),
 * so the availability check simply asks the status.
 * OOP - ENCAPSULATION: the fields are private, with getters.
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
