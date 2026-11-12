package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * INHERITANCE: a SuiteRoom IS-A Room with separate bedrooms and optional butler service.
 * Pricing rule: base price, +15% on Friday and Saturday nights,
 * plus a fixed nightly butler fee when butler service is included.
 */
public class SuiteRoom extends Room {

    public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.15");
    public static final BigDecimal BUTLER_FEE_PER_NIGHT = new BigDecimal("5000.00");
    public static final int MAX_BEDROOMS = 4;

    private int bedrooms;
    private boolean butlerService;

    public SuiteRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                     int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                     int bedrooms, boolean butlerService) {
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setBedrooms(bedrooms);
        this.butlerService = butlerService;
    }

    @Override
    public RoomType getType() {
        return RoomType.SUITE;
    }

    @Override
    public BigDecimal calculateNightlyRate(LocalDate night) {
        BigDecimal rate = getBasePrice();
        if (isWeekendNight(night)) {
            rate = rate.multiply(WEEKEND_MULTIPLIER);
        }
        if (butlerService) {
            rate = rate.add(BUTLER_FEE_PER_NIGHT);
        }
        return money(rate);
    }

    @Override
    public String getPricingNote() {
        String note = "Base rate Sunday-Thursday; +15% on Friday and Saturday nights.";
        return butlerService ? note + " Includes butler service (LKR 5,000 per night)." : note;
    }

    @Override
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, bedrooms + (bedrooms == 1 ? " separate bedroom" : " separate bedrooms"));
        if (butlerService) {
            features.add(1, "Personal butler service");
        }
        return features;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
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
