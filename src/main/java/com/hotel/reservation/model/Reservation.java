package com.hotel.reservation.model;

import java.time.LocalDate;
import java.math.BigDecimal;

/** A booking with stable room/guest references and a snapshot of its quoted price. */
public class Reservation {
    private String id;
    private String roomId;
    private String customerId;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;
    private int guests;
    private BigDecimal total;
    private boolean cancelled;

    public Reservation() {}

    public Reservation(String id, String roomId, String customerId, LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal total, boolean cancelled) {
        this.id = id;
        this.roomId = roomId;
        this.customerId = customerId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.total = total;
        this.cancelled = cancelled;
    }

    public String getId() { return id; }
    public void setId(String value) { id = value; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String value) { roomId = value; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String value) { customerId = value; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate value) { checkIn = value; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate value) { checkOut = value; }

    public int getGuests() { return guests; }
    public void setGuests(int value) { guests = value; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal value) { total = value; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean value) { cancelled = value; }
}
