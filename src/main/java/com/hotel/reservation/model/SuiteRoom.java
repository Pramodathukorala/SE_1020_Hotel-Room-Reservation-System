package com.hotel.reservation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SuiteRoom extends Room {
    public SuiteRoom() {}
    public SuiteRoom(String id, String number, int capacity, BigDecimal price, String description, boolean maintenance) {
        super(id, number, capacity, price, description, maintenance);
    }
    @Override
    public BigDecimal calculateNightlyRate() { return getBasePrice().multiply(new BigDecimal("1.50")).setScale(2,RoundingMode.HALF_UP); }
    @Override
    public List<String> getRoomFeatures() { return List.of("Separate living room","Panoramic ocean view","Soaking tub"); }
    @Override
    public String getCategory() { return "SUITE"; }
}
