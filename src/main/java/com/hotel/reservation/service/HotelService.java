package com.hotel.reservation.service;

import com.hotel.reservation.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface HotelService {
    List<Room> rooms();
    List<Customer> customers();
    List<Reservation> reservations();
    Room room(String id);
    Customer customer(String id);
    Reservation reservation(String id);
    Room saveRoom(String id, String category, String number, int capacity, BigDecimal price, String description, boolean maintenance);
    Customer saveCustomer(String id, String name, String email, String phone);
    Reservation book(String id, String roomId, String customerId, LocalDate in, LocalDate out, int guests);
    boolean available(String roomId, LocalDate in, LocalDate out, String excludedReservation);
    void deleteRoom(String id);
    void deleteCustomer(String id);
    void cancel(String id);
    void deleteReservation(String id);
}
