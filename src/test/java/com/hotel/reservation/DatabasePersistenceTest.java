package com.hotel.reservation;

import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.hotel.reservation.TestHotel.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SQL persistence: data surviving a restart, subclass reconstruction, amenities table,
 * parameterised SQL, and the database constraints (foreign key, unique, check).
 */
class DatabasePersistenceTest {

    @Test
    void startsWithEmptyTables() {
        TestHotel hotel = new TestHotel();
        assertThat(hotel.roomRepository.count()).isZero();
        assertThat(hotel.customerRepository.count()).isZero();
        assertThat(hotel.reservationRepository.count()).isZero();
        assertThat(hotel.roomRepository.nextId()).isEqualTo("R001");
    }

    @Test
    void dataSurvivesARestart() {
        TestHotel hotel = new TestHotel();
        Room suite = hotel.roomService.create(TestHotel.suiteRoom("301", "40000", 4, true));
        Room deluxe = hotel.roomService.create(TestHotel.deluxeRoom("201", "20000", 3));
        Room standard = hotel.roomService.create(TestHotel.standardRoom("101", "10000", 2));
        Customer guest = hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        Reservation booking = hotel.reservationService.create(guest.getId(), suite.getId(), day(4), day(6), 3);
        Reservation cancelled = hotel.reservationService.create(guest.getId(), deluxe.getId(), day(1), day(3), 1);
        hotel.reservationService.cancel(cancelled.getId());

        TestHotel restarted = hotel.restart();

        // the room_type column rebuilds the correct subclass, with its type-specific columns
        Room loadedSuite = restarted.roomService.findById(suite.getId());
        assertThat(loadedSuite).isInstanceOf(SuiteRoom.class);
        assertThat(((SuiteRoom) loadedSuite).hasButlerService()).isTrue();
        assertThat(((SuiteRoom) loadedSuite).getBedrooms()).isEqualTo(2);
        assertThat(restarted.roomService.findById(deluxe.getId())).isInstanceOf(DeluxeRoom.class);
        assertThat(((DeluxeRoom) restarted.roomService.findById(deluxe.getId())).getView()).isEqualTo("Ocean");
        assertThat(((StandardRoom) restarted.roomService.findById(standard.getId())).getBedType()).isEqualTo("Queen");
        // amenities come back from the room_amenities table in their original order
        assertThat(loadedSuite.getAmenities()).containsExactly("Wi-Fi", "Air conditioning");
        assertThat(loadedSuite.getBasePrice()).isEqualByComparingTo("40000.00");

        assertThat(restarted.customerService.findById(guest.getId()).getEmail()).isEqualTo("nimal@example.com");

        Reservation loaded = restarted.reservationService.findById(booking.getId());
        assertThat(loaded.getCheckIn()).isEqualTo(day(4));
        assertThat(loaded.getCheckOut()).isEqualTo(day(6));
        assertThat(loaded.getGuests()).isEqualTo(3);
        assertThat(loaded.getTotalPrice()).isEqualByComparingTo(booking.getTotalPrice());
        assertThat(loaded.getCreatedAt()).isEqualTo(booking.getCreatedAt());
        assertThat(restarted.reservationService.findById(cancelled.getId()).getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        // the restored reservation still blocks double booking, and IDs continue
        assertThat(restarted.reservationService.quote(suite.getId(), day(5), day(7), 2).available()).isFalse();
        assertThat(restarted.reservationRepository.nextId()).isEqualTo("RES-0003");
    }

    @Test
    void updatesAndDeletesArePersisted() {
        TestHotel hotel = new TestHotel();
        Room a = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        Room b = hotel.roomService.create(TestHotel.standardRoom("102", "15000", 2));
        var form = TestHotel.suiteRoom("101", "17000", 3, false);   // change price AND type
        form.setAmenities("Bar, Soaking tub, Wi-Fi");
        hotel.roomService.update(a.getId(), form);
        hotel.roomService.delete(b.getId());

        TestHotel restarted = hotel.restart();
        assertThat(restarted.roomService.findAll()).hasSize(1);
        Room loaded = restarted.roomService.findById(a.getId());
        assertThat(loaded).isInstanceOf(SuiteRoom.class);
        assertThat(loaded.getBasePrice()).isEqualByComparingTo("17000");
        assertThat(loaded.getAmenities()).containsExactly("Bar", "Soaking tub", "Wi-Fi");
        // the deleted room's amenity rows were removed too (ON DELETE CASCADE)
        Integer orphanAmenities = hotel.jdbc.queryForObject(
                "SELECT COUNT(*) FROM room_amenities WHERE room_id = ?", Integer.class, b.getId());
        assertThat(orphanAmenities).isZero();
    }

    @Test
    void textWithQuotesAndSqlIsStoredSafely() {
        TestHotel hotel = new TestHotel();
        var form = TestHotel.customer("Seán O'Brien", "obrien@example.com");
        form.setAddress("1 Main St'); DROP TABLE customers; --");
        Customer c = hotel.customerService.register(form);

        Customer loaded = hotel.restart().customerService.findById(c.getId());
        assertThat(loaded.getFullName()).isEqualTo("Seán O'Brien");
        assertThat(loaded.getAddress()).isEqualTo("1 Main St'); DROP TABLE customers; --");
        assertThat(hotel.customerRepository.count()).isEqualTo(1);   // table still exists
    }

    @Test
    void foreignKeyStopsDeletingARoomThatHasReservations() {
        TestHotel hotel = new TestHotel();
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        Customer c = hotel.customerService.register(TestHotel.customer("Ayesha Fernando", "ayesha@example.com"));
        hotel.reservationService.create(c.getId(), room.getId(), day(1), day(2), 1);

        // bypassing the service rule: the database itself refuses
        assertThatThrownBy(() -> hotel.roomRepository.deleteById(room.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> hotel.customerRepository.deleteById(c.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueAndCheckConstraintsAreEnforcedByTheDatabase() {
        TestHotel hotel = new TestHotel();
        hotel.customerRepository.save(new Customer("C001", "Nimal Perera", "nimal@example.com", "0771234567", "Colombo"));
        assertThatThrownBy(() -> hotel.customerRepository.save(
                new Customer("C002", "Copy Cat", "nimal@example.com", "0771234567", "Kandy")))
                .isInstanceOf(DataIntegrityViolationException.class);

        // a reservation pointing to a room that does not exist is rejected by the foreign key
        assertThatThrownBy(() -> hotel.reservationRepository.save(new Reservation("RES-0001", "C001", "R999",
                new com.hotel.reservation.model.StayPeriod(day(1), day(2)), 1, BigDecimal.TEN,
                ReservationStatus.CONFIRMED, LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void searchQueriesUseTheDatabase() {
        TestHotel hotel = new TestHotel();
        hotel.roomService.create(TestHotel.standardRoom("A-12", "15000", 2));
        hotel.customerService.register(TestHotel.customer("Nimal Perera", "Nimal@Example.com"));

        assertThat(hotel.roomRepository.findByRoomNumber("a-12")).isPresent();       // case-insensitive
        assertThat(hotel.customerRepository.findByEmail(" NIMAL@example.com ")).isPresent();
        assertThat(hotel.reservationRepository.findByRoomId("R001")).isEqualTo(List.of());
    }
}
