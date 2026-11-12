package com.hotel.reservation.exception;

/**
 * The PARENT class of all the application's own errors.
 * OOP - INHERITANCE: HotelException extends Java's RuntimeException, and
 *       ResourceNotFoundException, ValidationException and BookingConflictException extend HotelException.
 * OOP - POLYMORPHISM: "catch (HotelException e)" catches all of these child exceptions.
 */
public class HotelException extends RuntimeException {   // OOP - INHERITANCE

    public HotelException(String message) {
        super(message);
    }

    public HotelException(String message, Throwable cause) {
        super(message, cause);
    }
}
