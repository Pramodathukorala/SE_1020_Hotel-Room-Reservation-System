package com.hotel.reservation.repository;

import com.hotel.reservation.model.Room;

import java.util.Optional;

/** Room-specific queries on top of the generic CRUD operations. */
public interface RoomRepository extends DataRepository<Room> {

    Optional<Room> findByRoomNumber(String roomNumber);
}
