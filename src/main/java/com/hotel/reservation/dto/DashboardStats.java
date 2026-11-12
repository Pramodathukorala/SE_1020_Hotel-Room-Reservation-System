package com.hotel.reservation.dto;

import java.math.BigDecimal;

/** Numbers shown on the admin dashboard overview cards. */
public record DashboardStats(long totalRooms, long availableRooms, long roomsUnderMaintenance,
                             long totalCustomers, long totalReservations, long activeReservations,
                             long cancelledReservations, long arrivalsToday, long occupiedTonight,
                             BigDecimal confirmedRevenue) {

    /** Percentage of bookable rooms occupied tonight. */
    public int occupancyPercent() {
        long bookable = totalRooms - roomsUnderMaintenance;
        return bookable <= 0 ? 0 : (int) Math.round(occupiedTonight * 100.0 / bookable);
    }
}
