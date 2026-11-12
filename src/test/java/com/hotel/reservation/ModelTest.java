package com.hotel.reservation;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.StayPeriod;
import com.hotel.reservation.model.SuiteRoom;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Plain unit tests of the OOP model: polymorphism, encapsulation and the StayPeriod rules. */
class ModelTest {

    private static final LocalDate FRIDAY = LocalDate.of(2030, 1, 11);
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);

    private static final List<String> AMENITIES = List.of("Wi-Fi");

    @Test
    void polymorphicNightlyRates() {
        // superclass references holding subclass objects
        Room standard = new StandardRoom("R1", "101", "S", "d", new BigDecimal("10000"), 2, AMENITIES, null, false, "Queen");
        Room deluxe = new DeluxeRoom("R2", "201", "D", "d", new BigDecimal("10000"), 2, AMENITIES, null, false, "Ocean", true);
        Room suite = new SuiteRoom("R3", "301", "S", "d", new BigDecimal("10000"), 2, AMENITIES, null, false, 1, true);

        // same method call, different behaviour chosen at runtime
        assertThat(standard.calculateNightlyRate(FRIDAY)).isEqualByComparingTo("10000.00");
        assertThat(deluxe.calculateNightlyRate(FRIDAY)).isEqualByComparingTo("11000.00");
        assertThat(suite.calculateNightlyRate(FRIDAY)).isEqualByComparingTo("16500.00");   // 11,500 + 5,000 butler

        assertThat(standard.calculateNightlyRate(MONDAY)).isEqualByComparingTo("10000.00");
        assertThat(deluxe.calculateNightlyRate(MONDAY)).isEqualByComparingTo("10000.00");
        assertThat(suite.calculateNightlyRate(MONDAY)).isEqualByComparingTo("15000.00");

        // overloaded calculateStayCost gives the same result for both versions
        assertThat(deluxe.calculateStayCost(MONDAY, MONDAY.plusDays(7)))
                .isEqualByComparingTo(deluxe.calculateStayCost(new StayPeriod(MONDAY, MONDAY.plusDays(7))))
                .isEqualByComparingTo("72000.00");   // 5 x 10,000 + 2 x 11,000
    }

    @Test
    void overriddenFeaturesExtendTheAmenities() {
        Room deluxe = new DeluxeRoom("R2", "201", "D", "d", new BigDecimal("1"), 2, AMENITIES, null, false, "Ocean", true);
        assertThat(deluxe.getRoomFeatures()).containsExactly("Ocean view", "Private balcony", "Wi-Fi");
    }

    @Test
    void stayPeriodOverlapUsesExclusiveCheckout() {
        StayPeriod existing = new StayPeriod(MONDAY.plusDays(2), MONDAY.plusDays(5));

        assertThat(new StayPeriod(MONDAY.plusDays(5), MONDAY.plusDays(6)).overlaps(existing)).isFalse(); // starts on checkout
        assertThat(new StayPeriod(MONDAY, MONDAY.plusDays(2)).overlaps(existing)).isFalse();             // ends on check-in
        assertThat(new StayPeriod(MONDAY.plusDays(4), MONDAY.plusDays(6)).overlaps(existing)).isTrue();
        assertThat(new StayPeriod(MONDAY.plusDays(3), MONDAY.plusDays(4)).overlaps(existing)).isTrue();
        assertThat(existing.getNights()).isEqualTo(3);
        assertThatThrownBy(() -> new StayPeriod(MONDAY, MONDAY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encapsulationRejectsInvalidState() {
        Room room = new StandardRoom("R1", "101", "S", "d", new BigDecimal("10000"), 2, AMENITIES, null, false, "Queen");
        assertThatThrownBy(() -> room.setBasePrice(new BigDecimal("-5"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> room.setCapacity(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> room.getAmenities().add("Hack")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(room.getBasePrice()).isEqualByComparingTo("10000");

        assertThatThrownBy(() -> new Customer("C1", "Ann Lee", "bad-email", "0771234567", "Colombo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reservationStatusChangesOnlyThroughCancel() {
        Reservation r = new Reservation("RES-1", "C1", "R1", new StayPeriod(MONDAY, FRIDAY), 2,
                new BigDecimal("40000.00"), ReservationStatus.CONFIRMED, LocalDateTime.now());
        assertThat(r.conflictsWith("R1", new StayPeriod(MONDAY.plusDays(1), MONDAY.plusDays(2)))).isTrue();

        r.cancel();
        assertThat(r.isActive()).isFalse();
        assertThat(r.conflictsWith("R1", new StayPeriod(MONDAY.plusDays(1), MONDAY.plusDays(2)))).isFalse();
        assertThatThrownBy(() -> r.updateBooking("R1", new StayPeriod(MONDAY, FRIDAY), 1, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }
}
