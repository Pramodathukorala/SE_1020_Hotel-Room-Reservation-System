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
 * ABSTRACTION + INHERITANCE: Room holds everything common to all rooms,
 * but it is abstract because a "generic" room cannot be priced.
 * Each subclass (StandardRoom, DeluxeRoom, SuiteRoom) supplies its own
 * nightly pricing rule by overriding {@link #calculateNightlyRate(LocalDate)}.
 *
 * ENCAPSULATION: all fields are private; setters validate their input so a
 * Room object can never hold a negative price or an impossible capacity.
 */
public abstract class Room implements Identifiable {

    public static final int MAX_CAPACITY = 10;
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000000");

    private final String id;          // never changes after creation
    private String roomNumber;
    private String name;
    private String description;
    private BigDecimal basePrice;     // BigDecimal avoids floating-point rounding errors with money
    private int capacity;
    private final List<String> amenities = new ArrayList<>();
    private String imageUrl;
    private boolean underMaintenance;

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

    // ---------------------------------------------------------------------
    // Abstract behaviour - every subclass MUST implement these (polymorphism)
    // ---------------------------------------------------------------------

    /** Which category this room belongs to. */
    public abstract RoomType getType();

    /** Price for one specific night. Subclasses apply their own rules. */
    public abstract BigDecimal calculateNightlyRate(LocalDate night);

    /** Short human-readable explanation of the pricing rule, shown on the room page. */
    public abstract String getPricingNote();

    // ---------------------------------------------------------------------
    // Shared behaviour
    // ---------------------------------------------------------------------

    /**
     * Features shown to guests. Subclasses override this and call super
     * to add their own type-specific features on top of the amenities.
     */
    public List<String> getRoomFeatures() {
        return new ArrayList<>(amenities);
    }

    /**
     * Total cost of a stay. This method is final: the algorithm (add up every night)
     * is the same for every room, but the call to calculateNightlyRate() is
     * dispatched at runtime to the correct subclass (runtime polymorphism).
     */
    public final BigDecimal calculateStayCost(StayPeriod stay) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate night : stay.getNightDates()) {
            total = total.add(calculateNightlyRate(night));
        }
        return money(total);
    }

    /** METHOD OVERLOADING: same operation, accepting two dates instead of a StayPeriod. */
    public final BigDecimal calculateStayCost(LocalDate checkIn, LocalDate checkOut) {
        return calculateStayCost(new StayPeriod(checkIn, checkOut));
    }

    /** Night-by-night prices, used to show a transparent price breakdown. */
    public Map<LocalDate, BigDecimal> getNightlyBreakdown(StayPeriod stay) {
        Map<LocalDate, BigDecimal> breakdown = new LinkedHashMap<>();
        for (LocalDate night : stay.getNightDates()) {
            breakdown.put(night, calculateNightlyRate(night));
        }
        return breakdown;
    }

    public boolean canAccommodate(int guests) {
        return guests >= 1 && guests <= capacity;
    }

    public boolean isBookable() {
        return !underMaintenance;
    }

    public String getDisplayTitle() {
        return name + " · Room " + roomNumber;
    }

    /** Helper for subclasses: Friday and Saturday nights are weekend nights. */
    protected static boolean isWeekendNight(LocalDate night) {
        DayOfWeek day = night.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }

    /** Helper for subclasses: round a monetary value to 2 decimal places. */
    protected static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------------
    // Getters and validating setters (encapsulation)
    // ---------------------------------------------------------------------

    @Override
    public String getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
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
        if (basePrice == null || basePrice.signum() <= 0 || basePrice.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price per night must be greater than zero.");
        }
        this.basePrice = money(basePrice);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        if (capacity < 1 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("Capacity must be between 1 and " + MAX_CAPACITY + " guests.");
        }
        this.capacity = capacity;
    }

    /** Returns a read-only view so callers cannot modify the internal list directly. */
    public List<String> getAmenities() {
        return Collections.unmodifiableList(amenities);
    }

    public void setAmenities(List<String> newAmenities) {
        amenities.clear();
        if (newAmenities != null) {
            for (String amenity : newAmenities) {
                if (amenity != null && !amenity.isBlank() && !amenities.contains(amenity.trim())) {
                    amenities.add(amenity.trim());
                }
            }
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = (imageUrl == null || imageUrl.isBlank()) ? "/images/rooms/standard-city.jpg" : imageUrl.trim();
    }

    public boolean isUnderMaintenance() {
        return underMaintenance;
    }

    public void setUnderMaintenance(boolean underMaintenance) {
        this.underMaintenance = underMaintenance;
    }

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

    @Override
    public String toString() {
        return getType() + " " + roomNumber + " (" + id + ")";
    }
}
