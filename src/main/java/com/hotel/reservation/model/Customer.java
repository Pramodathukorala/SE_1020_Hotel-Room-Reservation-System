package com.hotel.reservation.model;

import java.util.regex.Pattern;

/**
 * A hotel guest. ENCAPSULATION: fields are private and every setter
 * validates its value, so an invalid email or phone can never be stored.
 */
public class Customer implements Identifiable {

    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    /** Optional leading +, then 9 to 15 digits; spaces and dashes are allowed between digits. */
    public static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9](?:[ -]?[0-9]){8,14}$");

    private final String id;
    private String fullName;
    private String email;
    private String phone;
    private String address;

    public Customer(String id, String fullName, String email, String phone, String address) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required.");
        }
        this.id = id;
        setFullName(fullName);
        setEmail(email);
        setPhone(phone);
        setAddress(address);
    }

    @Override
    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 80) {
            throw new IllegalArgumentException("Full name must be between 2 and 80 characters.");
        }
        this.fullName = fullName.trim().replaceAll("\\s+", " ");
    }

    public String getEmail() {
        return email;
    }

    /** Emails are stored in lower case so duplicate checks are case-insensitive. */
    public void setEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Please enter a valid phone number (9-15 digits).");
        }
        this.phone = phone.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null || address.trim().length() < 3 || address.trim().length() > 200) {
            throw new IllegalArgumentException("Address must be between 3 and 200 characters.");
        }
        this.address = address.trim();
    }

    /** Initials for the avatar badge in the admin dashboard, e.g. "Nimal Perera" -> "NP". */
    public String getInitials() {
        StringBuilder initials = new StringBuilder();
        for (String part : fullName.split(" ")) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.toString();
    }

    @Override
    public String toString() {
        return fullName + " (" + id + ")";
    }
}
