package com.hotel.reservation.exception;

/**
 * Base class for all business errors in the application.
 * INHERITANCE: the more specific exceptions below extend this class,
 * so a controller can catch them all with one catch block when convenient.
 */
public class HotelException extends RuntimeException {

    public HotelException(String message) {
        super(message);
    }

    public HotelException(String message, Throwable cause) {
        super(message, cause);
    }
}
