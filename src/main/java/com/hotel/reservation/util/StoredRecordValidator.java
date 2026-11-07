package com.hotel.reservation.util;

import com.hotel.reservation.model.*;

/** Reject incomplete stored objects before they reach services and templates. */
public final class StoredRecordValidator {
    private StoredRecordValidator() {}
    public static void validate(Object value) {
        boolean valid = switch (value) {
            case Room room -> text(room.getId()) && text(room.getNumber()) && text(room.getDescription())
                    && room.getCapacity() > 0 && room.getCapacity() <= 20
                    && room.getBasePrice() != null && room.getBasePrice().signum() > 0;
            case Customer guest -> text(guest.getId()) && text(guest.getName())
                    && text(guest.getEmail()) && text(guest.getPhone());
            case Reservation booking -> text(booking.getId()) && text(booking.getRoomId()) && text(booking.getCustomerId())
                    && booking.getCheckIn() != null && booking.getCheckOut() != null
                    && booking.getCheckOut().isAfter(booking.getCheckIn()) && booking.getGuests() > 0
                    && booking.getTotal() != null && booking.getTotal().signum() > 0;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Incomplete or invalid stored record");
    }
    private static boolean text(String value) { return value != null && !value.isBlank(); }
}
