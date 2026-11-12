package com.hotel.reservation.dto;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.Room;

/**
 * Read-only view object that combines a reservation with its customer and room,
 * so pages can show names and room details without looking them up themselves.
 */
public record ReservationDetails(Reservation reservation, Customer customer, Room room) {
}
