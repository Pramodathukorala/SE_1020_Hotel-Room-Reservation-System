package com.hotel.reservation.model;

/**
 * Abstraction shared by every stored entity (Room, Customer, Reservation).
 * The generic file repository only needs to know that an object has an ID,
 * so it can store any type that implements this interface.
 */
public interface Identifiable {

    String getId();
}
