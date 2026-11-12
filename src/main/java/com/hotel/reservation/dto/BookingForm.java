package com.hotel.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Form object for the public booking page: the stay details plus the guest's
 * contact details. COMPOSITION: the guest part re-uses CustomerForm and its validation.
 */
public class BookingForm {

    @NotBlank(message = "Room is required.")
    private String roomId;

    @NotNull(message = "Check-in date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Check-out date is required.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @NotNull(message = "Number of guests is required.")
    @Min(value = 1, message = "At least 1 guest is required.")
    @Max(value = 10, message = "At most 10 guests.")
    private Integer guests = 1;

    @Valid
    private CustomerForm customer = new CustomerForm();

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public CustomerForm getCustomer() { return customer; }
    public void setCustomer(CustomerForm customer) { this.customer = customer; }
}
