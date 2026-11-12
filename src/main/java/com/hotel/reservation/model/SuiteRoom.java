package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A Suite.
 *
 * OOP - INHERITANCE: "extends Room" means a SuiteRoom IS-A Room.
 *       It adds two fields of its own: bedrooms and butlerService.
 * Pricing rule: base price, +15% on Friday and Saturday nights,
 *               plus LKR 5,000 per night when butler service is included.
 */
public class SuiteRoom extends Room {   // OOP - INHERITANCE

    public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.15");       // +15%
    public static final BigDecimal BUTLER_FEE_PER_NIGHT = new BigDecimal("5000.00");  // butler fee
    public static final int MAX_BEDROOMS = 4;

    // OOP - ENCAPSULATION: extra fields only suites have, kept private
    private int bedrooms;
    private boolean butlerService;

    public SuiteRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                     int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                     int bedrooms, boolean butlerService) {
        // OOP - INHERITANCE: call the parent (Room) constructor first
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setBedrooms(bedrooms);
        this.butlerService = butlerService;
    }

    @Override   // OOP - POLYMORPHISM
    public RoomType getType() {
        return RoomType.SUITE;
    }

    @Override   // OOP - POLYMORPHISM: the suite's own pricing rule
    public BigDecimal calculateNightlyRate(LocalDate night) {
        BigDecimal rate = getBasePrice();
        if (isWeekendNight(night)) {
            rate = rate.multiply(WEEKEND_MULTIPLIER);   // weekend: +15%
        }
        if (butlerService) {
            rate = rate.add(BUTLER_FEE_PER_NIGHT);      // butler: +5,000 per night
        }
        return money(rate);
    }

    @Override   // OOP - POLYMORPHISM
    public String getPricingNote() {
        String note = "Base rate Sunday-Thursday; +15% on Friday and Saturday nights.";
        return butlerService ? note + " Includes butler service (LKR 5,000 per night)." : note;
    }

    @Override   // OOP - POLYMORPHISM: parent's features + bedrooms + butler
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, bedrooms + (bedrooms == 1 ? " separate bedroom" : " separate bedrooms"));
        if (butlerService) {
            features.add(1, "Personal butler service");
        }
        return features;
    }

    // OOP - ENCAPSULATION: getters and a checked setter
    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        // a suite has 1 to 4 bedrooms
        if (bedrooms < 1 || bedrooms > MAX_BEDROOMS) {
            throw new IllegalArgumentException("A suite must have between 1 and " + MAX_BEDROOMS + " bedrooms.");
        }
        this.bedrooms = bedrooms;
    }

    public boolean hasButlerService() {
        return butlerService;
    }

    public void setButlerService(boolean butlerService) {
        this.butlerService = butlerService;
    }
}
