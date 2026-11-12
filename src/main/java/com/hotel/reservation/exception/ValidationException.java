package com.hotel.reservation.exception;

/**
 * Thrown when input breaks a business rule (duplicate room number, invalid dates, ...).
 * The optional field name lets the controller show the message next to the right form input.
 */
public class ValidationException extends HotelException {   // OOP - INHERITANCE: a kind of HotelException

    private final String field;

    public ValidationException(String message) {
        this(null, message);
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
