package com.hotel.reservation.exception;

public class HotelException extends RuntimeException {
    public HotelException(String message) { super(message); }
    public HotelException(String message, Throwable cause) { super(message, cause); }
}
