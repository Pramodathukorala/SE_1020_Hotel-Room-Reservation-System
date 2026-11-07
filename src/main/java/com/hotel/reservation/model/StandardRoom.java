package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class StandardRoom extends Room {
    public StandardRoom() {}
    public StandardRoom(String id, String number, int capacity, BigDecimal price, String description, boolean maintenance) {
        super(id, number, capacity, price, description, maintenance);
    }
    @Override
    public BigDecimal calculateNightlyRate() { return getBasePrice().multiply(new BigDecimal("1.00")).setScale(2,RoundingMode.HALF_UP); }
    @Override
    public List<String> getRoomFeatures() { return List.of("King bed","Rain shower","Garden view"); }
    @Override
    public String getCategory() { return "STANDARD"; }
}
