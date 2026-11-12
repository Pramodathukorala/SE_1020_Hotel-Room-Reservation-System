package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A booking of one room by one customer for a stay period.
 *
 * ASSOCIATION: a reservation refers to its Customer and Room by ID
 * (the same way the records are linked in the text files).
 * COMPOSITION: the stay dates are held in a StayPeriod value object.
 * ENCAPSULATION: the status can only change through cancel(), and the booking
 * details only through updateBooking(); there are no free setters.
 */
public class Reservation implements Identifiable {

    private final String id;
    private final String customerId;
    private String roomId;
    private StayPeriod stay;
    private int guests;
    private BigDecimal totalPrice;
    private ReservationStatus status;
    private final LocalDateTime createdAt;

    public Reservation(String id, String customerId, String roomId, StayPeriod stay, int guests,
                       BigDecimal totalPrice, ReservationStatus status, LocalDateTime createdAt) {
        if (id == null || id.isBlank() || customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID and customer ID are required.");
        }
        if (status == null || createdAt == null) {
            throw new IllegalArgumentException("Status and creation time are required.");
        }
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.createdAt = createdAt;
        applyBookingDetails(roomId, stay, guests, totalPrice);
    }

    /** Changes the room/dates/guests of a confirmed reservation. */
    public void updateBooking(String roomId, StayPeriod stay, int guests, BigDecimal totalPrice) {
        if (!isActive()) {
            throw new IllegalStateException("Only confirmed reservations can be changed.");
        }
        applyBookingDetails(roomId, stay, guests, totalPrice);
    }

    public void cancel() {
        if (status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation " + id + " is already cancelled.");
        }
        status = ReservationStatus.CANCELLED;
    }

    /** True when this reservation still occupies its room. */
    public boolean isActive() {
        return status.blocksAvailability();
    }

    /** Does this reservation stop someone else booking the given room for the given stay? */
    public boolean conflictsWith(String otherRoomId, StayPeriod otherStay) {
        return isActive() && roomId.equals(otherRoomId) && stay.overlaps(otherStay);
    }

    private void applyBookingDetails(String roomId, StayPeriod stay, int guests, BigDecimal totalPrice) {
        if (roomId == null || roomId.isBlank() || stay == null) {
            throw new IllegalArgumentException("Room and stay dates are required.");
        }
        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required.");
        }
        if (totalPrice == null || totalPrice.signum() < 0) {
            throw new IllegalArgumentException("Total price cannot be negative.");
        }
        this.roomId = roomId;
        this.stay = stay;
        this.guests = guests;
        this.totalPrice = totalPrice;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRoomId() {
        return roomId;
    }

    public StayPeriod getStay() {
        return stay;
    }

    public LocalDate getCheckIn() {
        return stay.getCheckIn();
    }

    public LocalDate getCheckOut() {
        return stay.getCheckOut();
    }

    public long getNights() {
        return stay.getNights();
    }

    public int getGuests() {
        return guests;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return id + " [" + status + "] room " + roomId + " " + stay;
    }
}
