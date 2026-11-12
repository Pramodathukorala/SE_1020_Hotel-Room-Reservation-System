package com.hotel.reservation;

import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.dto.RoomForm;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.DeluxeRoom;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.model.StandardRoom;
import com.hotel.reservation.model.SuiteRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static com.hotel.reservation.TestHotel.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CRUD operations for rooms and customers. */
class RoomAndCustomerServiceTest {

    @TempDir
    Path dataDir;

    private TestHotel hotel;

    @BeforeEach
    void setUp() {
        hotel = new TestHotel(dataDir);
    }

    // =============================================================== rooms

    @Test
    void createsRoomOfTheChosenSubclass() {
        Room standard = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        Room deluxe = hotel.roomService.create(TestHotel.deluxeRoom("201", "25000", 3));
        Room suite = hotel.roomService.create(TestHotel.suiteRoom("301", "50000", 4, false));

        assertThat(standard).isInstanceOf(StandardRoom.class);
        assertThat(deluxe).isInstanceOf(DeluxeRoom.class);
        assertThat(suite).isInstanceOf(SuiteRoom.class);
        assertThat(standard.getId()).isEqualTo("R001");
        assertThat(suite.getId()).isEqualTo("R003");
        assertThat(standard.getAmenities()).containsExactly("Wi-Fi", "Air conditioning");
        assertThat(hotel.roomService.findAll()).hasSize(3);
    }

    @Test
    void rejectsDuplicateRoomNumber() {
        hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        assertThatThrownBy(() -> hotel.roomService.create(TestHotel.deluxeRoom("101", "25000", 2)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void rejectsInvalidRoomValues() {
        RoomForm badPrice = TestHotel.standardRoom("101", "0", 2);
        assertThatThrownBy(() -> hotel.roomService.create(badPrice)).isInstanceOf(ValidationException.class);

        RoomForm badSuite = TestHotel.suiteRoom("301", "50000", 4, false);
        badSuite.setBedrooms(9);
        assertThatThrownBy(() -> hotel.roomService.create(badSuite)).isInstanceOf(ValidationException.class);
        assertThat(hotel.roomRepository.count()).isZero();
    }

    @Test
    void updatesRoomIncludingItsType() {
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));

        RoomForm form = TestHotel.deluxeRoom("101A", "27500.50", 3);
        Room updated = hotel.roomService.update(room.getId(), form);

        assertThat(updated.getId()).isEqualTo(room.getId());
        assertThat(updated).isInstanceOf(DeluxeRoom.class);
        assertThat(updated.getRoomNumber()).isEqualTo("101A");
        assertThat(updated.getBasePrice()).isEqualByComparingTo("27500.50");
        assertThat(hotel.roomService.findById(room.getId()).getType()).isEqualTo(RoomType.DELUXE);
    }

    @Test
    void updateKeepsOwnRoomNumberButRejectsAnotherRooms() {
        Room a = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        hotel.roomService.create(TestHotel.standardRoom("102", "15000", 2));

        hotel.roomService.update(a.getId(), TestHotel.standardRoom("101", "16000", 2));   // same number: fine
        assertThatThrownBy(() -> hotel.roomService.update(a.getId(), TestHotel.standardRoom("102", "16000", 2)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deletesRoomWithoutReservations() {
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        hotel.roomService.delete(room.getId());

        assertThat(hotel.roomRepository.existsById(room.getId())).isFalse();
        assertThatThrownBy(() -> hotel.roomService.findById(room.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeleteRoomWithReservations() {
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        Customer c = hotel.customerService.register(TestHotel.customer("Ayesha Fernando", "ayesha@example.com"));
        hotel.reservationService.create(c.getId(), room.getId(), day(1), day(2), 1);

        assertThatThrownBy(() -> hotel.roomService.delete(room.getId()))
                .isInstanceOf(ValidationException.class).hasMessageContaining("maintenance");
        assertThat(hotel.roomRepository.existsById(room.getId())).isTrue();
    }

    @Test
    void searchFiltersByKeywordTypePriceAndGuests() {
        hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        hotel.roomService.create(TestHotel.deluxeRoom("201", "25000", 3));
        hotel.roomService.create(TestHotel.suiteRoom("301", "50000", 4, false));

        assertThat(hotel.roomService.search(null, RoomType.DELUXE, null, null, null)).hasSize(1);
        assertThat(hotel.roomService.search(null, null, new BigDecimal("30000"), null, null)).hasSize(2);
        assertThat(hotel.roomService.search(null, null, null, 4, null)).extracting(Room::getRoomNumber).containsExactly("301");
        assertThat(hotel.roomService.search("suite", null, null, null, null)).hasSize(1);
        assertThat(hotel.roomService.search(null, null, null, null, "price-desc").get(0).getRoomNumber()).isEqualTo("301");
    }

    // =========================================================== customers

    @Test
    void registersCustomer() {
        Customer c = hotel.customerService.register(TestHotel.customer("Nimal Perera", "Nimal@Example.com"));

        assertThat(c.getId()).isEqualTo("C001");
        assertThat(c.getEmail()).isEqualTo("nimal@example.com");   // normalised to lower case
        assertThat(hotel.customerService.findById("C001").getFullName()).isEqualTo("Nimal Perera");
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        assertThatThrownBy(() -> hotel.customerService.register(TestHotel.customer("Someone Else", "NIMAL@example.com")))
                .isInstanceOf(ValidationException.class).hasMessageContaining("already registered");
    }

    @Test
    void rejectsInvalidCustomerData() {
        CustomerForm badEmail = TestHotel.customer("Nimal Perera", "not-an-email");
        assertThatThrownBy(() -> hotel.customerService.register(badEmail)).isInstanceOf(ValidationException.class);

        CustomerForm badPhone = TestHotel.customer("Nimal Perera", "nimal@example.com");
        badPhone.setPhone("12ab");
        assertThatThrownBy(() -> hotel.customerService.register(badPhone)).isInstanceOf(ValidationException.class);
        assertThat(hotel.customerRepository.count()).isZero();
    }

    @Test
    void updatesCustomer() {
        Customer c = hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        CustomerForm form = CustomerForm.fromCustomer(c);
        form.setPhone("0711234567");
        form.setAddress("45 Temple Street, Kandy");

        Customer updated = hotel.customerService.update(c.getId(), form);

        assertThat(updated.getPhone()).isEqualTo("0711234567");
        assertThat(hotel.customerService.findById(c.getId()).getAddress()).isEqualTo("45 Temple Street, Kandy");
    }

    @Test
    void deleteCustomerRules() {
        Customer free = hotel.customerService.register(TestHotel.customer("Daniel Silva", "daniel@example.com"));
        Customer booked = hotel.customerService.register(TestHotel.customer("Ayesha Fernando", "ayesha@example.com"));
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "15000", 2));
        hotel.reservationService.create(booked.getId(), room.getId(), day(1), day(2), 1);

        hotel.customerService.delete(free.getId());
        assertThat(hotel.customerRepository.existsById(free.getId())).isFalse();
        assertThatThrownBy(() -> hotel.customerService.delete(booked.getId())).isInstanceOf(ValidationException.class);
    }

    @Test
    void searchCustomers() {
        hotel.customerService.register(TestHotel.customer("Nimal Perera", "nimal@example.com"));
        hotel.customerService.register(TestHotel.customer("Ayesha Fernando", "ayesha@example.com"));

        assertThat(hotel.customerService.search("ayesha")).hasSize(1);
        assertThat(hotel.customerService.search("C001")).hasSize(1);
        assertThat(hotel.customerService.search("")).hasSize(2);
    }
}
