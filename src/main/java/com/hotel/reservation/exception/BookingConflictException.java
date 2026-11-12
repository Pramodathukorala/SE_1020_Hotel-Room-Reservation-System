package com.hotel.reservation.exception;

/** Thrown when a requested stay overlaps an existing confirmed reservation (double booking). */
public class BookingConflictException extends HotelException {

    public BookingConflictException(String message) {
        super(message);
    }
}
