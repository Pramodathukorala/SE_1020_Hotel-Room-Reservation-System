package com.hotel.reservation.exception;

/** Thrown when a data file cannot be read or written. */
public class StorageException extends HotelException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
