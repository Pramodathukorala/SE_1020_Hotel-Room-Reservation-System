package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * INHERITANCE: a DeluxeRoom IS-A Room with a view and an optional balcony.
 * Pricing rule: base price, plus a 10% surcharge on Friday and Saturday nights.
 */
public class DeluxeRoom extends Room {

    public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.10");

    private String view;        // e.g. "Garden", "Ocean"
    private boolean balcony;

    public DeluxeRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                      int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                      String view, boolean balcony) {
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setView(view);
        this.balcony = balcony;
    }

    @Override
    public RoomType getType() {
        return RoomType.DELUXE;
    }

    @Override
    public BigDecimal calculateNightlyRate(LocalDate night) {
        if (isWeekendNight(night)) {
            return money(getBasePrice().multiply(WEEKEND_MULTIPLIER));
        }
        return getBasePrice();
    }

    @Override
    public String getPricingNote() {
        return "Base rate Sunday-Thursday; +10% on Friday and Saturday nights.";
    }

    @Override
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, view + " view");
        if (balcony) {
            features.add(1, "Private balcony");
        }
        return features;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = requireText(view, "View", 40);
    }

    public boolean hasBalcony() {
        return balcony;
    }

    public void setBalcony(boolean balcony) {
        this.balcony = balcony;
    }
}
