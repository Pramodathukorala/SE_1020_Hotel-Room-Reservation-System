package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A Deluxe room.
 *
 * OOP - INHERITANCE: "extends Room" means a DeluxeRoom IS-A Room.
 *       It adds two fields of its own: view and balcony.
 * Pricing rule: base price, plus 10% on Friday and Saturday nights.
 */
public class DeluxeRoom extends Room {   // OOP - INHERITANCE

    public static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.10");   // +10%

    // OOP - ENCAPSULATION: extra fields only Deluxe rooms have, kept private
    private String view;        // e.g. "Garden", "Ocean"
    private boolean balcony;

    public DeluxeRoom(String id, String roomNumber, String name, String description, BigDecimal basePrice,
                      int capacity, List<String> amenities, String imageUrl, boolean underMaintenance,
                      String view, boolean balcony) {
        // OOP - INHERITANCE: call the parent (Room) constructor first
        super(id, roomNumber, name, description, basePrice, capacity, amenities, imageUrl, underMaintenance);
        setView(view);
        this.balcony = balcony;
    }

    @Override   // OOP - POLYMORPHISM
    public RoomType getType() {
        return RoomType.DELUXE;
    }

    @Override   // OOP - POLYMORPHISM: a different pricing rule from StandardRoom
    public BigDecimal calculateNightlyRate(LocalDate night) {
        if (isWeekendNight(night)) {                                  // Friday or Saturday?
            return money(getBasePrice().multiply(WEEKEND_MULTIPLIER)); // add 10%
        }
        return getBasePrice();                                         // normal price
    }

    @Override   // OOP - POLYMORPHISM
    public String getPricingNote() {
        return "Base rate Sunday-Thursday; +10% on Friday and Saturday nights.";
    }

    @Override   // OOP - POLYMORPHISM: parent's features + view + balcony
    public List<String> getRoomFeatures() {
        List<String> features = super.getRoomFeatures();
        features.add(0, view + " view");
        if (balcony) {
            features.add(1, "Private balcony");
        }
        return features;
    }

    // OOP - ENCAPSULATION: getters and setters
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
