package com.hotel.reservation.service;

import com.hotel.reservation.dto.RegistrationForm;
import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.AdminAccount;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.model.GuestAccount;
import com.hotel.reservation.model.UserAccount;
import com.hotel.reservation.repository.AdminRepository;
import com.hotel.reservation.repository.CustomerRepository;
import com.hotel.reservation.repository.GuestAccountRepository;
import com.hotel.reservation.security.SessionUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registration and sign-in for guests, and sign-in for administrators.
 *
 * Passwords are hashed with BCrypt before they are stored; at sign-in the typed password
 * is checked against the hash. The same message is shown for "unknown user" and
 * "wrong password", so the login form does not reveal which accounts exist.
 */
@Service
public class AccountService {

    private static final String INVALID_LOGIN = "Incorrect email or password.";
    private static final String INVALID_ADMIN_LOGIN = "Incorrect username or password.";

    private final GuestAccountRepository guestAccountRepository;
    private final AdminRepository adminRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(GuestAccountRepository guestAccountRepository, AdminRepository adminRepository,
                          CustomerRepository customerRepository, CustomerService customerService,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.guestAccountRepository = guestAccountRepository;
        this.adminRepository = adminRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    // ============================================================ guests

    /**
     * Creates a guest login.
     * - New email: a customer record is created first, then the login.
     * - Email of an existing customer WITHOUT a login (e.g. someone who booked as a guest):
     *   the login is attached to that customer, so their earlier bookings appear in "My bookings".
     * - Email that already has a login: rejected.
     */
    public GuestAccount registerGuest(RegistrationForm form) {
        Optional<Customer> existing = customerRepository.findByEmail(form.getCustomer().getEmail());
        if (existing.isPresent() && guestAccountRepository.existsById(existing.get().getId())) {
            throw new ValidationException("customer.email", "An account with this email already exists. Please sign in.");
        }
        Customer customer = existing.orElseGet(() -> customerService.register(form.getCustomer()));
        GuestAccount account = new GuestAccount(customer.getId(), passwordEncoder.encode(form.getPassword()),
                LocalDateTime.now(clock));
        return guestAccountRepository.save(account);
    }

    /** Checks a guest's email and password. Returns the session information on success. */
    public SessionUser authenticateGuest(String email, String rawPassword) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(INVALID_LOGIN));
        GuestAccount account = guestAccountRepository.findById(customer.getId())
                .orElseThrow(() -> new ValidationException(INVALID_LOGIN));
        return toSessionUser(account, customer.getFullName(), INVALID_LOGIN, rawPassword);
    }

    /** Builds the session information for a guest who has just registered. */
    public SessionUser sessionFor(GuestAccount account) {
        return new SessionUser(account.getId(), customerService.findById(account.getCustomerId()).getFullName(),
                account.getRole());
    }

    // ============================================================ admins

    public SessionUser authenticateAdmin(String username, String rawPassword) {
        AdminAccount admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException(INVALID_ADMIN_LOGIN));
        return toSessionUser(admin, admin.getFullName(), INVALID_ADMIN_LOGIN, rawPassword);
    }

    /** Creates an administrator (used to set up the first admin). */
    public AdminAccount createAdmin(String username, String fullName, String rawPassword) {
        if (adminRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("username", "That admin username is already taken.");
        }
        try {
            AdminAccount admin = AdminAccount.create(adminRepository.nextId(), username, fullName, rawPassword,
                    passwordEncoder, LocalDateTime.now(clock));
            return adminRepository.save(admin);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    public long countAdmins() {
        return adminRepository.count();
    }

    // ============================================================ shared

    /**
     * OOP - POLYMORPHISM: the parameter type is the parent "UserAccount", so this ONE method
     * works for both AdminAccount and GuestAccount. account.getRole() returns ADMIN or GUEST
     * depending on the real object.
     * OOP - ENCAPSULATION: the password is checked by the account itself (passwordMatches).
     */
    private SessionUser toSessionUser(UserAccount account, String displayName, String errorMessage, String rawPassword) {
        if (!account.passwordMatches(rawPassword, passwordEncoder)) {
            throw new ValidationException(errorMessage);
        }
        return new SessionUser(account.getId(), displayName, account.getRole());
    }
}
