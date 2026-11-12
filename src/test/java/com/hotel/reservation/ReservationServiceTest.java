package com.hotel.reservation;

import com.hotel.reservation.dto.ReservationForm;
import com.hotel.reservation.exception.BookingConflictException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.StayPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.hotel.reservation.TestHotel.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reservation rules: availability, double booking, dates, capacity, pricing, update, cancel, delete. */
class ReservationServiceTest {

    private TestHotel hotel;
    private Room standard;   // R001 - LKR 10,000 flat, 2 guests
    private Room deluxe;     // R002 - LKR 20,000, +10% Fri/Sat, 3 guests
    private Room suite;      // R003 - LKR 40,000, +15% Fri/Sat, + butler LKR 5,000, 4 guests
    private Customer guest;

    @BeforeEach
    void setUp() {
        hotel = new TestHotel();
        standard = hotel.roomService.create(TestHotel.standardRoom("101", "10000", 2));
        deluxe = hotel.roomService.create(TestHotel.deluxeRoom("201", "20000", 3));
        suite = hotel.roomService.create(TestHotel.suiteRoom("301", "40000", 4, true));
        guest = hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
    }

    private Reservation book(Room room, int fromDay, int toDay, int guests) {
        return hotel.reservationService.create(guest.getId(), room.getId(), day(fromDay), day(toDay), guests);
    }

    // ------------------------------------------------------------ creation & pricing

    @Test
    @DisplayName("Creating a reservation stores a CONFIRMED booking with the calculated total")
    void createsReservation() {
        Reservation r = book(standard, 1, 4, 2);   // Tue-Fri, 3 nights

        assertThat(r.getId()).isEqualTo("RES-0001");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(r.getNights()).isEqualTo(3);
        assertThat(r.getTotalPrice()).isEqualByComparingTo("30000.00");
        assertThat(hotel.reservationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deluxe rooms charge +10% on Friday and Saturday nights")
    void deluxeWeekendPricing() {
        // Thu 10 Jan -> Sun 13 Jan: Thu 20,000 + Fri 22,000 + Sat 22,000
        Reservation r = book(deluxe, 3, 6, 2);
        assertThat(r.getTotalPrice()).isEqualByComparingTo("64000.00");
    }

    @Test
    @DisplayName("Suites charge +15% at weekends plus the butler fee every night")
    void suitePricingWithButler() {
        // Fri 11 Jan -> Sun 13 Jan: 2 x (40,000 x 1.15 + 5,000) = 2 x 51,000
        Reservation r = book(suite, 4, 6, 4);
        assertThat(r.getTotalPrice()).isEqualByComparingTo("102000.00");
        assertThat(r.getTotalPrice().scale()).isEqualTo(2);
    }

    // ------------------------------------------------------------ double booking

    @Test
    @DisplayName("An overlapping confirmed booking for the same room is rejected")
    void rejectsDoubleBooking() {
        book(standard, 2, 5, 1);

        assertThatThrownBy(() -> book(standard, 4, 7, 1)).isInstanceOf(BookingConflictException.class);
        assertThatThrownBy(() -> book(standard, 1, 3, 1)).isInstanceOf(BookingConflictException.class);
        assertThatThrownBy(() -> book(standard, 3, 4, 1)).isInstanceOf(BookingConflictException.class); // inside
        assertThatThrownBy(() -> book(standard, 1, 8, 1)).isInstanceOf(BookingConflictException.class); // around
        assertThat(hotel.reservationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("A new guest may check in on the day the previous guest checks out")
    void allowsSameDayTurnover() {
        book(standard, 2, 5, 1);
        Reservation next = book(standard, 5, 7, 1);       // check-in == previous check-out
        Reservation before = book(standard, 1, 2, 1);     // check-out == previous check-in

        assertThat(next.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(before.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Overlapping dates in a different room are allowed")
    void sameDatesDifferentRoom() {
        book(standard, 2, 5, 1);
        assertThat(book(deluxe, 2, 5, 1).getRoomId()).isEqualTo(deluxe.getId());
    }

    @Test
    @DisplayName("Cancelling a reservation releases the room for those dates")
    void cancellationReleasesAvailability() {
        Reservation first = book(standard, 2, 5, 1);
        StayPeriod stay = new StayPeriod(day(2), day(5));
        assertThat(hotel.reservationService.isRoomAvailable(standard.getId(), stay, null)).isFalse();

        hotel.reservationService.cancel(first.getId());

        assertThat(hotel.reservationService.findById(first.getId()).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(hotel.reservationService.isRoomAvailable(standard.getId(), stay, null)).isTrue();
        assertThat(book(standard, 2, 5, 2).getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void cancellingTwiceIsRejected() {
        Reservation r = book(standard, 2, 5, 1);
        hotel.reservationService.cancel(r.getId());
        assertThatThrownBy(() -> hotel.reservationService.cancel(r.getId())).isInstanceOf(ValidationException.class);
    }

    // ------------------------------------------------------------ validation

    @Test
    @DisplayName("Check-out must be after check-in")
    void rejectsInvalidDateOrder() {
        assertThatThrownBy(() -> book(standard, 5, 5, 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("after the check-in");
        assertThatThrownBy(() -> book(standard, 5, 3, 1)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> hotel.reservationService.create(guest.getId(), standard.getId(), null, day(2), 1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> book(standard, -1, 2, 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("past");
    }

    @Test
    void rejectsStaysLongerThanThirtyNights() {
        assertThatThrownBy(() -> book(standard, 1, 32, 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("30 nights");
    }

    @Test
    @DisplayName("Guest count cannot exceed the room capacity")
    void rejectsTooManyGuests() {
        assertThatThrownBy(() -> book(standard, 1, 2, 3))
                .isInstanceOf(ValidationException.class).hasMessageContaining("at most 2 guests");
        assertThatThrownBy(() -> book(standard, 1, 2, 0)).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsRoomUnderMaintenance() {
        var form = TestHotel.standardRoom("101", "10000", 2);
        form.setUnderMaintenance(true);
        hotel.roomService.update(standard.getId(), form);

        assertThatThrownBy(() -> book(standard, 1, 2, 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("maintenance");
    }

    @Test
    void rejectsUnknownCustomerOrRoom() {
        assertThatThrownBy(() -> hotel.reservationService.create("C999", standard.getId(), day(1), day(2), 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("C999");
        assertThatThrownBy(() -> hotel.reservationService.create(guest.getId(), "R999", day(1), day(2), 1))
                .isInstanceOf(ValidationException.class).hasMessageContaining("R999");
    }

    // ------------------------------------------------------------ update / delete

    @Test
    @DisplayName("Editing a reservation recalculates the total and does not conflict with itself")
    void updateRecalculatesTotal() {
        Reservation r = book(standard, 1, 3, 1);   // 20,000

        ReservationForm form = ReservationForm.fromReservation(r);
        form.setCheckOut(day(4));                  // extend over its own dates: 3 nights
        Reservation updated = hotel.reservationService.update(r.getId(), form);
        assertThat(updated.getTotalPrice()).isEqualByComparingTo("30000.00");

        form.setRoomId(deluxe.getId());            // move to the deluxe room, Tue-Fri, no weekend nights
        updated = hotel.reservationService.update(r.getId(), form);
        assertThat(updated.getRoomId()).isEqualTo(deluxe.getId());
        assertThat(updated.getTotalPrice()).isEqualByComparingTo("60000.00");
    }

    @Test
    void updateCannotCreateADoubleBooking() {
        book(standard, 5, 7, 1);
        Reservation other = book(standard, 1, 3, 1);
        ReservationForm form = ReservationForm.fromReservation(other);
        form.setCheckOut(day(6));
        assertThatThrownBy(() -> hotel.reservationService.update(other.getId(), form))
                .isInstanceOf(BookingConflictException.class);
        assertThat(hotel.reservationService.findById(other.getId()).getCheckOut()).isEqualTo(day(3));
    }

    @Test
    @DisplayName("Only cancelled reservations can be permanently deleted")
    void deleteRules() {
        Reservation r = book(standard, 1, 3, 1);
        assertThatThrownBy(() -> hotel.reservationService.delete(r.getId())).isInstanceOf(ValidationException.class);

        hotel.reservationService.cancel(r.getId());
        hotel.reservationService.delete(r.getId());
        assertThat(hotel.reservationRepository.existsById(r.getId())).isFalse();
    }

    @Test
    void quoteReportsAvailabilityAndPrice() {
        book(standard, 2, 4, 1);
        var free = hotel.reservationService.quote(standard.getId(), day(4), day(6), 2);
        var taken = hotel.reservationService.quote(standard.getId(), day(3), day(5), 2);

        assertThat(free.available()).isTrue();
        assertThat(free.total()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(taken.available()).isFalse();
        assertThat(taken.message()).contains("already booked");
    }

    @Test
    @DisplayName("Public booking re-uses an existing customer with the same email")
    void bookingReusesCustomerByEmail() {
        var form = new com.hotel.reservation.dto.BookingForm();
        form.setRoomId(standard.getId());
        form.setCheckIn(day(1));
        form.setCheckOut(day(2));
        form.setGuests(1);
        form.setCustomer(TestHotel.customer("Nimal Perera", "NIMAL@example.com"));

        Reservation r = hotel.reservationService.book(form);
        assertThat(r.getCustomerId()).isEqualTo(guest.getId());
        assertThat(hotel.customerRepository.count()).isEqualTo(1);
    }
}
