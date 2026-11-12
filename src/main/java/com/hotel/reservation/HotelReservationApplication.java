package com.hotel.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Hotel Room Reservation System.
 * Spring Boot scans this package and its sub-packages for controllers,
 * services and repositories, and wires them together with constructor injection.
 */
@SpringBootApplication
public class HotelReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelReservationApplication.class, args);
    }
}
