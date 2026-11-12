package com.hotel.reservation;

import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.dto.RoomForm;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.repository.file.FileCustomerRepository;
import com.hotel.reservation.repository.file.FileReservationRepository;
import com.hotel.reservation.repository.file.FileRoomRepository;
import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Builds the real services on top of the real file repositories in a temporary folder,
 * without starting Spring. "Today" is fixed to Monday 7 January 2030 so that
 * weekend pricing and past-date rules give the same result every time the tests run.
 */
final class TestHotel {

    static final LocalDate TODAY = LocalDate.of(2030, 1, 7);   // a Monday
    static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-07T09:00:00Z"), ZoneOffset.UTC);

    final Path dataDir;
    final FileRoomRepository roomRepository;
    final FileCustomerRepository customerRepository;
    final FileReservationRepository reservationRepository;
    final RoomService roomService;
    final CustomerService customerService;
    final ReservationService reservationService;

    TestHotel(Path dataDir) {
        this.dataDir = dataDir;
        this.roomRepository = new FileRoomRepository(dataDir.toString());
        this.customerRepository = new FileCustomerRepository(dataDir.toString());
        this.reservationRepository = new FileReservationRepository(dataDir.toString());
        this.roomService = new RoomService(roomRepository, reservationRepository);
        this.customerService = new CustomerService(customerRepository, reservationRepository);
        this.reservationService = new ReservationService(reservationRepository, roomRepository,
                customerRepository, customerService, CLOCK);
    }

    /** Simulates an application restart: new repository objects re-read the same files. */
    TestHotel restart() {
        return new TestHotel(dataDir);
    }

    static RoomForm standardRoom(String number, String price, int capacity) {
        RoomForm form = baseRoom(number, RoomType.STANDARD, price, capacity);
        form.setBedType("Queen");
        return form;
    }

    static RoomForm deluxeRoom(String number, String price, int capacity) {
        RoomForm form = baseRoom(number, RoomType.DELUXE, price, capacity);
        form.setView("Ocean");
        form.setBalcony(true);
        return form;
    }

    static RoomForm suiteRoom(String number, String price, int capacity, boolean butler) {
        RoomForm form = baseRoom(number, RoomType.SUITE, price, capacity);
        form.setBedrooms(2);
        form.setButlerService(butler);
        return form;
    }

    private static RoomForm baseRoom(String number, RoomType type, String price, int capacity) {
        RoomForm form = new RoomForm();
        form.setRoomNumber(number);
        form.setName("Test " + type.getDisplayName() + " " + number);
        form.setType(type);
        form.setDescription("A room used by the automated tests.");
        form.setBasePrice(new BigDecimal(price));
        form.setCapacity(capacity);
        form.setAmenities("Wi-Fi, Air conditioning");
        form.setImageUrl("/images/rooms/standard-city.jpg");
        return form;
    }

    static CustomerForm customer(String name, String email) {
        CustomerForm form = new CustomerForm();
        form.setFullName(name);
        form.setEmail(email);
        form.setPhone("+94 77 123 4567");
        form.setAddress("12 Galle Road, Colombo");
        return form;
    }

    static LocalDate day(int plusDays) {
        return TODAY.plusDays(plusDays);
    }
}
