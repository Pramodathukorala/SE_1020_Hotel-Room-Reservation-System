package com.hotel.reservation;

import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.dto.RoomForm;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.repository.jdbc.JdbcAdminRepository;
import com.hotel.reservation.repository.jdbc.JdbcCustomerRepository;
import com.hotel.reservation.repository.jdbc.JdbcGuestAccountRepository;
import com.hotel.reservation.repository.jdbc.JdbcReservationRepository;
import com.hotel.reservation.repository.jdbc.JdbcRoomRepository;
import com.hotel.reservation.service.AccountService;
import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Builds the real services on top of the real JDBC repositories, without starting Spring.
 *
 * Each test gets its own fresh in-memory H2 database running in MySQL compatibility mode,
 * created from the same schema.sql the application uses with MySQL.
 * "Today" is fixed to Monday 7 January 2030 so weekend pricing and past-date rules
 * give the same result every time the tests run.
 */
final class TestHotel {

    static final LocalDate TODAY = LocalDate.of(2030, 1, 7);   // a Monday
    static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-07T09:00:00Z"), ZoneOffset.UTC);

    final DataSource dataSource;
    final JdbcTemplate jdbc;
    final JdbcRoomRepository roomRepository;
    final JdbcCustomerRepository customerRepository;
    final JdbcReservationRepository reservationRepository;
    final RoomService roomService;
    final CustomerService customerService;
    final ReservationService reservationService;
    final JdbcGuestAccountRepository guestAccountRepository;
    final JdbcAdminRepository adminRepository;
    final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);   // low cost = fast tests
    final AccountService accountService;

    /** A brand-new, empty database. */
    TestHotel() {
        this(createDatabase());
    }

    private TestHotel(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.roomRepository = new JdbcRoomRepository(jdbc);
        this.customerRepository = new JdbcCustomerRepository(jdbc);
        this.reservationRepository = new JdbcReservationRepository(jdbc);
        this.roomService = new RoomService(roomRepository, reservationRepository);
        this.customerService = new CustomerService(customerRepository, reservationRepository);
        this.reservationService = new ReservationService(reservationRepository, roomRepository,
                customerRepository, customerService, CLOCK);
        this.guestAccountRepository = new JdbcGuestAccountRepository(jdbc);
        this.adminRepository = new JdbcAdminRepository(jdbc);
        this.accountService = new AccountService(guestAccountRepository, adminRepository, customerRepository,
                customerService, passwordEncoder, CLOCK);
    }

    /** Simulates an application restart: brand-new repository and service objects, same database. */
    TestHotel restart() {
        return new TestHotel(dataSource);
    }

    private static DataSource createDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:hotel-" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        return dataSource;
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
