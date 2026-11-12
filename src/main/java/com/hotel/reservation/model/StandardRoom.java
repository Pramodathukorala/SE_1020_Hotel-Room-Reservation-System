package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A Standard room.
 *
 * OOP - INHERITANCE: "extends Room" means a StandardRoom IS-A Room.
 *       It gets all of Room's fields and methods, and adds one field: bedType.
 * Pricing rule: the same price every night.
 */
public class StandardRoom extends Room {   // OOP - INHERITANCE

    // OOP - ENCAPSULATION: extra field only Standard rooms have, kept private
    private String bedType;   // e.g. "Queen" or "Twin"

    public StandardRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                        int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                        String bedType) {
        // OOP - INHERITANCE: super(...) calls the parent (Room) constructor to set the shared fields
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setBedType(bedType);
    }

    @Override   // OOP - POLYMORPHISM: our own version of Room's abstract method
    public RoomType getType() {
        return RoomType.STANDARD;
    }

    @Override   // OOP - POLYMORPHISM: Standard rooms cost the same every night
    public BigDecimal calculateNightlyRate(LocalDate night) {
        return getBasePrice();
    }

    @Override   // OOP - POLYMORPHISM
    public String getPricingNote() {
        return "Flat rate every night of the week.";
    }

    @Override   // OOP - POLYMORPHISM: reuse the parent's list (super) and add the bed type
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, bedType + " bed");
        return features;
    }

    // OOP - ENCAPSULATION: getter and checked setter
    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = requireText(bedType, "Bed type", 30);   // requireText is inherited from Room
    }
}
