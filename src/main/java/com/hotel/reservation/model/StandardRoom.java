package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * INHERITANCE: a StandardRoom IS-A Room.
 * Pricing rule: a flat rate - every night costs exactly the base price.
 */
public class StandardRoom extends Room {

    private String bedType;   // e.g. "Queen" or "Twin"

    public StandardRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                        int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                        String bedType) {
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setBedType(bedType);
    }

    @Override
    public RoomType getType() {
        return RoomType.STANDARD;
    }

    @Override
    public BigDecimal calculateNightlyRate(LocalDate night) {
        return getBasePrice();
    }

    @Override
    public String getPricingNote() {
        return "Flat rate every night of the week.";
    }

    @Override
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, bedType + " bed");
        return features;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = requireText(bedType, "Bed type", 30);
    }
}
