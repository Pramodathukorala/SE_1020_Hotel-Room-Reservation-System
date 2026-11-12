package com.hotel.reservation;

import com.hotel.reservation.dto.RegistrationForm;
import com.hotel.reservation.dto.ReservationForm;
import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.AdminAccount;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.GuestAccount;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.model.Role;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.UserAccount;
import com.hotel.reservation.security.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.hotel.reservation.TestHotel.day;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Guest registration and sign-in, admin sign-in, password hashing, and "my bookings" ownership rules. */
class AccountServiceTest {

    private TestHotel hotel;

    @BeforeEach
    void setUp() {
        hotel = new TestHotel();
    }

    private static RegistrationForm registration(String name, String email, String password) {
        RegistrationForm form = new RegistrationForm();
        form.setCustomer(TestHotel.customer(name, email));
        form.setPassword(password);
        form.setConfirmPassword(password);
        return form;
    }

    @Test
    void registeringCreatesACustomerAndAHashedLogin() {
        GuestAccount account = hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));

        Customer customer = hotel.customerService.findById(account.getCustomerId());
        assertThat(customer.getEmail()).isEqualTo("nimal@example.com");

        String storedHash = hotel.jdbc.queryForObject("SELECT password_hash FROM guest_accounts WHERE id = ?",
                String.class, account.getId());
        assertThat(storedHash).isNotEqualTo("Secret123").startsWith("$2");   // never stored in plain text
    }

    @Test
    void guestSignsInWithEmailAndPassword() {
        hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));

        SessionUser user = hotel.accountService.authenticateGuest("NIMAL@example.com", "Secret123");
        assertThat(user.role()).isEqualTo(Role.GUEST);
        assertThat(user.displayName()).isEqualTo("Nimal Perera");

        assertThatThrownBy(() -> hotel.accountService.authenticateGuest("nimal@example.com", "Wrong1234"))
                .isInstanceOf(ValidationException.class).hasMessage("Incorrect email or password.");
        assertThatThrownBy(() -> hotel.accountService.authenticateGuest("nobody@example.com", "Secret123"))
                .isInstanceOf(ValidationException.class).hasMessage("Incorrect email or password.");
    }

    @Test
    void anEmailCanOnlyBeRegisteredOnce() {
        hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));
        assertThatThrownBy(() -> hotel.accountService.registerGuest(registration("Copy", "nimal@example.com", "Other1234")))
                .isInstanceOf(ValidationException.class).hasMessageContaining("already exists");
    }

    @Test
    void registeringWithTheEmailOfAPastGuestLinksTheirBookings() {
        Customer pastGuest = hotel.customerService.register(TestHotel.customer("Ayesha Fernando", "ayesha@example.com"));
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "10000", 2));
        hotel.reservationService.create(pastGuest.getId(), room.getId(), day(1), day(2), 1);

        GuestAccount account = hotel.accountService.registerGuest(registration("Ayesha Fernando", "ayesha@example.com", "Secret123"));

        assertThat(account.getCustomerId()).isEqualTo(pastGuest.getId());
        assertThat(hotel.customerRepository.count()).isEqualTo(1);
        assertThat(hotel.reservationService.findDetailsByCustomer(account.getCustomerId())).hasSize(1);
    }

    @Test
    void adminSignsInWithUsernameStoredInTheAdminsTable() {
        AdminAccount admin = hotel.accountService.createAdmin("Manager", "Hotel Manager", "AdminPass1");

        assertThat(admin.getUsername()).isEqualTo("manager");
        assertThat(hotel.adminRepository.findByUsername("manager")).isPresent();
        assertThat(hotel.accountService.authenticateAdmin("manager", "AdminPass1").role()).isEqualTo(Role.ADMIN);
        assertThatThrownBy(() -> hotel.accountService.authenticateAdmin("manager", "nope"))
                .isInstanceOf(ValidationException.class);
        // a guest login is not an admin login
        hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));
        assertThatThrownBy(() -> hotel.accountService.authenticateAdmin("nimal@example.com", "Secret123"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> hotel.accountService.createAdmin("manager", "Copy", "AdminPass2"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void accountsArePolymorphic() {
        UserAccount admin = hotel.accountService.createAdmin("admin", "Hotel Admin", "AdminPass1");
        UserAccount guest = hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));

        assertThat(admin.getHomePage()).isEqualTo("/admin");
        assertThat(guest.getHomePage()).isEqualTo("/my-bookings");
        assertThat(admin.passwordMatches("AdminPass1", hotel.passwordEncoder)).isTrue();
        assertThat(guest.toString()).doesNotContain("$2");   // the hash is never printed
    }

    @Test
    void guestsCanOnlyManageTheirOwnBookings() {
        Room room = hotel.roomService.create(TestHotel.standardRoom("101", "10000", 2));
        GuestAccount owner = hotel.accountService.registerGuest(registration("Owner", "owner@example.com", "Secret123"));
        GuestAccount stranger = hotel.accountService.registerGuest(registration("Stranger", "stranger@example.com", "Secret123"));
        Reservation booking = hotel.reservationService.create(owner.getCustomerId(), room.getId(), day(1), day(3), 1);

        // stranger: everything looks like "not found"
        assertThatThrownBy(() -> hotel.reservationService.getDetailsForCustomer(booking.getId(), stranger.getCustomerId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> hotel.reservationService.cancelForCustomer(booking.getId(), stranger.getCustomerId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // owner: update recalculates the total, delete only after cancelling
        ReservationForm form = ReservationForm.fromReservation(booking);
        form.setCheckOut(day(4));
        assertThat(hotel.reservationService.updateForCustomer(booking.getId(), owner.getCustomerId(), form).getTotalPrice())
                .isEqualByComparingTo("30000.00");
        assertThatThrownBy(() -> hotel.reservationService.deleteForCustomer(booking.getId(), owner.getCustomerId()))
                .isInstanceOf(ValidationException.class);
        assertThat(hotel.reservationService.cancelForCustomer(booking.getId(), owner.getCustomerId()).getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
        hotel.reservationService.deleteForCustomer(booking.getId(), owner.getCustomerId());
        assertThat(hotel.reservationRepository.existsById(booking.getId())).isFalse();
    }

    @Test
    void deletingACustomerAlsoDeletesTheirLogin() {
        GuestAccount account = hotel.accountService.registerGuest(registration("Nimal Perera", "nimal@example.com", "Secret123"));
        hotel.customerService.delete(account.getCustomerId());
        assertThat(hotel.guestAccountRepository.existsById(account.getId())).isFalse();   // ON DELETE CASCADE
    }
}
