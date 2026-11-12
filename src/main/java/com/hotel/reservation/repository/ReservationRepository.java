package com.hotel.reservation.repository;

import com.hotel.reservation.model.Reservation;

import java.util.List;

/** Reservation-specific queries on top of the generic CRUD operations. */
// OOP - INHERITANCE: this interface extends DataRepository<Reservation> and adds its own search methods
public interface ReservationRepository extends DataRepository<Reservation> {

    List<Reservation> findByRoomId(String roomId);

    List<Reservation> findByCustomerId(String customerId);
}
