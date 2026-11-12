package com.hotel.reservation.model;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * The PARENT class of every login account.
 *
 * OOP - ABSTRACTION: abstract class - you cannot create a plain UserAccount.
 * OOP - INHERITANCE: AdminAccount and GuestAccount extend this class.
 * OOP - ENCAPSULATION: the password is never stored as plain text - only a secret code
 *       ("BCrypt hash"). Other classes can only CHECK a password with passwordMatches().
 */
public abstract class UserAccount implements Identifiable {

    // OOP - ENCAPSULATION: private fields
    private final String id;
    private String passwordHash;   // BCrypt hash, never the real password
    private final LocalDateTime createdAt;

    protected UserAccount(String id, String passwordHash, LocalDateTime createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Account ID is required.");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Creation time is required.");
        }
        this.id = id;
        this.createdAt = createdAt;
        changePasswordHash(passwordHash);
    }

    // OOP - ABSTRACTION: abstract methods - each subclass gives its own answer (OOP - POLYMORPHISM)

    /** ADMIN or GUEST. */
    public abstract Role getRole();

    /** The page this user goes to after signing in ("/admin" or "/my-bookings"). */
    public abstract String getHomePage();

    /** OOP - ENCAPSULATION: the only way to use the hash - is the typed password correct? (true/false) */
    public boolean passwordMatches(String rawPassword, PasswordEncoder encoder) {
        return rawPassword != null && encoder.matches(rawPassword, passwordHash);
    }

    /** Replaces the hash, e.g. after a password change. Only hashes are accepted, never plain text. */
    public final void changePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || !newPasswordHash.startsWith("$2")) {
            throw new IllegalArgumentException("A BCrypt password hash is required.");
        }
        this.passwordHash = newPasswordHash;
    }

    @Override
    public String getId() {
        return id;
    }

    /** Only the repository layer uses this, to store the hash. The plain password is never available. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override   // OOP - POLYMORPHISM: overrides Object.toString()
    public String toString() {
        return getRole() + " account " + id;   // the hash is deliberately left out
    }
}
