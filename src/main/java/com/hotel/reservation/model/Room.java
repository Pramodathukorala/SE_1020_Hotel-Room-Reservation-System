package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Room = the PARENT class of all hotel rooms.
 *
 * OOP - ABSTRACTION: this class is "abstract", so you cannot create a plain Room.
 *       A room must be a StandardRoom, DeluxeRoom or SuiteRoom.
 * OOP - INHERITANCE: StandardRoom, DeluxeRoom and SuiteRoom "extend" this class and
 *       get all its fields and methods for free.
 * OOP - ENCAPSULATION: all fields are private. Other classes must use the getters and
 *       setters, and the setters check the values first.
 */
public abstract class Room implements Identifiable {   // OOP - ABSTRACTION: implements the Identifiable interface

    // Limits used by the checks below
    public static final int MAX_CAPACITY = 10;
    public static final int MAX_AMENITY_LENGTH = 60;
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000000");

    // OOP - ENCAPSULATION: every field is private (hidden from other classes)
    private final String id;          // final = cannot change after the room is created
    private String roomNumber;
    private String name;
    private String description;
    private BigDecimal basePrice;     // BigDecimal = exact money values (no rounding errors like double)
    private int capacity;             // maximum number of guests
    private final List<String> amenities = new ArrayList<>();
    private String imageUrl;
    private boolean underMaintenance; // true = room cannot be booked

    /**
     * Constructor. It is "protected", so only subclasses can call it (with super(...)).
     * OOP - ENCAPSULATION: it uses the setters, so even a new room is checked.
     */
    protected Room(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                   int capacity, List<String> amenities, String imageUrl, boolean underMaintenance) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Room ID is required.");
        }
        this.id = id;
        setRoomNumber(roomNumber);
        setName(name);
        setDescription(description);
        setBasePrice(basePrice);
        setCapacity(capacity);
        setAmenities(amenities);
        setImageUrl(imageUrl);
        setUnderMaintenance(underMaintenance);
    }

    // =====================================================================
    // OOP - ABSTRACTION: abstract methods (no body here).
    // Every subclass MUST write its own version (OOP - POLYMORPHISM).
    // =====================================================================

    /** Which type of room this is (STANDARD, DELUXE or SUITE). */
    public abstract RoomType getType();

    /** Price for ONE night. Each room type has its own pricing rule. */
    public abstract BigDecimal calculateNightlyRate(LocalDate night);

    /** A short sentence that explains the pricing rule, shown on the website. */
    public abstract String getPricingNote();

    // =====================================================================
    // Methods shared by all rooms (inherited by every subclass)
    // =====================================================================

    /**
     * List of features shown to guests.
     * OOP - POLYMORPHISM: subclasses OVERRIDE this method and add their own features.
     */
    public List<String> getRoomFeatures() {
        return new ArrayList<>(amenities);   // return a copy, not the real list (encapsulation)
    }

    /**
     * Total price of a stay = add up the price of every night.
     *
     * OOP - POLYMORPHISM: calculateNightlyRate(night) below runs the version of the
     * REAL object - a SuiteRoom uses suite prices, a DeluxeRoom uses deluxe prices -
     * even though this code only knows it is a "Room". Java decides at runtime.
     *
     * "final" = subclasses cannot change how the total is added up.
     */
    public final BigDecimal calculateStayCost(StayPeriod stay) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate night : stay.getNightDates()) {
            total = total.add(calculateNightlyRate(night));   // OOP - POLYMORPHISM happens here
        }
        return money(total);
    }

    /**
     * OOP - POLYMORPHISM (method OVERLOADING): same method name as above,
     * but it takes two dates instead of a StayPeriod.
     */
    public final BigDecimal calculateStayCost(LocalDate checkIn, LocalDate checkOut) {
        return calculateStayCost(new StayPeriod(checkIn, checkOut));
    }

    /** Price of each night, used for the price breakdown on the website. */
    public Map<LocalDate, BigDecimal> getNightlyBreakdown(StayPeriod stay) {
        Map<LocalDate, BigDecimal> breakdown = new LinkedHashMap<>();
        for (LocalDate night : stay.getNightDates()) {
            breakdown.put(night, calculateNightlyRate(night));   // OOP - POLYMORPHISM
        }
        return breakdown;
    }

    /** Can this room hold the given number of guests? */
    public boolean canAccommodate(int guests) {
        return guests >= 1 && guests <= capacity;
    }

    /** A room can be booked only when it is NOT under maintenance. */
    public boolean isBookable() {
        return !underMaintenance;
    }

    public String getDisplayTitle() {
        return name + " · Room " + roomNumber;
    }

    /**
     * Helper used by the subclasses: Friday and Saturday nights are weekend nights.
     * "protected" = only this class and its subclasses can use it (OOP - INHERITANCE).
     */
    protected static boolean isWeekendNight(LocalDate night) {
        DayOfWeek day = night.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }

    /** Helper used by the subclasses: round money to 2 decimal places. */
    protected static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // =====================================================================
    // OOP - ENCAPSULATION: getters (read) and setters (change).
    // Each setter CHECKS the value and refuses bad data.
    // =====================================================================

    @Override   // required by the Identifiable interface
    public String getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        // only 1-10 letters, digits or dashes are allowed
        if (roomNumber == null || !roomNumber.trim().matches("[A-Za-z0-9-]{1,10}")) {
            throw new IllegalArgumentException("Room number must be 1-10 letters, digits or dashes.");
        }
        this.roomNumber = roomNumber.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "Room name", 80);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = requireText(description, "Description", 1000);
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        // the price must be more than 0 and not unrealistically large
        if (basePrice == null || basePrice.signum() <= 0 || basePrice.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price per night must be greater than zero.");
        }
        this.basePrice = money(basePrice);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        // a room holds 1 to 10 guests
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_CAPACITY + " guests.");
        }
        this.capacity = capacity;
    }

    /**
     * OOP - ENCAPSULATION: returns a READ-ONLY list.
     * Other classes can read the amenities but cannot add or remove items directly.
     */
    public List<String> getAmenities() {
        return Collections.unmodifiableList(amenities);
    }

    /** Replaces the amenity list. Empty and duplicate items are skipped. */
    public void setAmenities(List<String> newAmenities) {
        List<String> cleaned = new ArrayList<>();
        if (newAmenities != null) {
            for (String amenity : newAmenities) {
                if (amenity == null || amenity.isBlank() || cleaned.contains(amenity.trim())) {
                    continue;   // skip empty or repeated amenities
                }
                if (amenity.trim().length() > MAX_AMENITY_LENGTH) {
                    throw new IllegalArgumentException("Each amenity must be at most " + MAX_AMENITY_LENGTH + " characters.");
                }
                cleaned.add(amenity.trim());
            }
        }
        // check everything first, then replace - the list is never left half-changed
        amenities.clear();
        amenities.addAll(cleaned);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        // use a default photo when no image is given
        this.imageUrl = (imageUrl == null || imageUrl.isBlank()) ? "/images/rooms/standard-city.jpg" : imageUrl.trim();
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

    /** Helper: text must not be empty and must not be too long. */
    protected static String requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    @Override   // OOP - POLYMORPHISM: overrides toString() from Java's Object class
    public String toString() {
        return getType() + " " + roomNumber + " (" + id + ")";
    }
}
