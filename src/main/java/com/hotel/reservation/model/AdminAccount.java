package com.hotel.reservation.model;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A hotel staff member who signs in to the admin dashboard with a username.
 * Saved in the "admins" table.
 *
 * OOP - INHERITANCE: AdminAccount IS-A UserAccount (gets id, password hash, passwordMatches()...).
 */
public class AdminAccount extends UserAccount {   // OOP - INHERITANCE

    // OOP - ENCAPSULATION: extra private fields only admins have
    private final String username;
    private String fullName;

    public AdminAccount(String id, String username, String fullName, String passwordHash, LocalDateTime createdAt) {
        super(id, passwordHash, createdAt);   // OOP - INHERITANCE: parent constructor
        if (username == null || !username.trim().matches("[A-Za-z0-9._-]{3,40}")) {
            throw new IllegalArgumentException("Username must be 3-40 letters, digits, dots, dashes or underscores.");
        }
        this.username = username.trim().toLowerCase(Locale.ROOT);
        setFullName(fullName);
    }

    /** Convenience constructor: hashes a plain password with the given encoder. */
    public static AdminAccount create(String id, String username, String fullName, String rawPassword,
                                      PasswordEncoder encoder, LocalDateTime createdAt) {
        return new AdminAccount(id, username, fullName, encoder.encode(rawPassword), createdAt);
    }

    @Override   // OOP - POLYMORPHISM: an admin's role
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override   // OOP - POLYMORPHISM: admins go to the dashboard after signing in
    public String getHomePage() {
        return "/admin";
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 80) {
            throw new IllegalArgumentException("Full name is required (max 80 characters).");
        }
        this.fullName = fullName.trim();
    }
}
