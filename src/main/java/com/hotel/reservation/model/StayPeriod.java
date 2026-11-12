package com.hotel.reservation.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The dates of a stay: check-in day and check-out day.
 * (The check-out day is not a night - a guest can arrive on the day another leaves.)
 *
 * OOP - ENCAPSULATION: both fields are private and final, so a StayPeriod can never change
 *       after it is created (it is "immutable"). The constructor refuses wrong dates.
 * The overlap rule and the night count are written once here and reused everywhere.
 */
public final class StayPeriod {

    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public StayPeriod(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Check-in and check-out dates are required.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after the check-in date.");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    /** Number of nights charged, e.g. 10th to 12th = 2 nights. */
    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Two stays conflict when: newCheckIn &lt; existingCheckOut AND newCheckOut &gt; existingCheckIn.
     * Because check-out is exclusive, a guest may check in on the day another guest checks out.
     */
    public boolean overlaps(StayPeriod other) {
        return this.checkIn.isBefore(other.checkOut) && this.checkOut.isAfter(other.checkIn);
    }

    /** Every night that is charged (the check-out day itself is not a night). */
    public List<LocalDate> getNightDates() {
        List<LocalDate> nights = new ArrayList<>();
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            nights.add(night);
        }
        return Collections.unmodifiableList(nights);
    }

    @Override   // OOP - POLYMORPHISM: overrides Object.equals() - two stays are equal if the dates are equal
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StayPeriod other)) {
            return false;
        }
        return checkIn.equals(other.checkIn) && checkOut.equals(other.checkOut);
    }

    @Override   // OOP - POLYMORPHISM: overrides Object.hashCode() (must match equals)
    public int hashCode() {
        return Objects.hash(checkIn, checkOut);
    }

    @Override   // OOP - POLYMORPHISM: overrides Object.toString()
    public String toString() {
        return checkIn + " to " + checkOut;
    }
}
