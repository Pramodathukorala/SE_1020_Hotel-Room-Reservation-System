package com.hotel.reservation.dto;

import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.StayPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Result of checking a room for specific dates: whether it can be booked,
 * why not (if not), and the calculated price.
 */
public record BookingQuote(Room room, StayPeriod stay, int guests, boolean available, String message,
                           BigDecimal total, Map<LocalDate, BigDecimal> nightlyRates) {

    public long nights() {
        return stay == null ? 0 : stay.getNights();
    }
}
