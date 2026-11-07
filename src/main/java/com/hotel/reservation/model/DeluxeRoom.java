package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DeluxeRoom extends Room {
    public DeluxeRoom() {}
    public DeluxeRoom(String id, String number, int capacity, BigDecimal price, String description, boolean maintenance) {
        super(id, number, capacity, price, description, maintenance);
    }
    @Override
    public BigDecimal calculateNightlyRate() { return getBasePrice().multiply(new BigDecimal("1.20")).setScale(2,RoundingMode.HALF_UP); }
    @Override
    public List<String> getRoomFeatures() { return List.of("King bed","Private balcony","Ocean view"); }
    @Override
    public String getCategory() { return "DELUXE"; }
}
