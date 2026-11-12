package com.hotel.reservation.repository;

import com.hotel.reservation.model.Reservation;

import java.util.List;

/** Reservation-specific queries on top of the generic CRUD operations. */
public interface ReservationRepository extends DataRepository<Reservation> {

    List<Reservation> findByRoomId(String roomId);

    List<Reservation> findByCustomerId(String customerId);
}
