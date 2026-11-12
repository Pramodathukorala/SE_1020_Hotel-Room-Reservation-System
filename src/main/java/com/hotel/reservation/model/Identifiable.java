package com.hotel.reservation.model;

/**
 * OOP - ABSTRACTION (interface): "anything that has an ID".
 * Room, Customer, Reservation and UserAccount all implement it.
 * The generic repository (AbstractJdbcRepository) only needs to know an object has an ID,
 * so ONE piece of code can store every type.
 */
public interface Identifiable {

    String getId();
}
