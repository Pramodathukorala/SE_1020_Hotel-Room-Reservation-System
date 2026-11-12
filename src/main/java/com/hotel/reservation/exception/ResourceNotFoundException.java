package com.hotel.reservation.exception;

/** Thrown when a room, customer or reservation ID does not exist. */
public class ResourceNotFoundException extends HotelException {

    public ResourceNotFoundException(String resourceName, String id) {
        super(resourceName + " '" + id + "' was not found.");
    }
}
