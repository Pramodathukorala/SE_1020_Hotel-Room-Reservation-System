package com.hotel.reservation.dto;

import com.hotel.reservation.model.Reservation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Form object used by the admin dashboard to create or edit a reservation for an existing customer. */
public class ReservationForm {

    @NotBlank(message = "Please choose a customer.")
    private String customerId;

    @NotBlank(message = "Please choose a room.")
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

    public static ReservationForm fromReservation(Reservation reservation) {
        ReservationForm form = new ReservationForm();
        form.customerId = reservation.getCustomerId();
        form.roomId = reservation.getRoomId();
        form.checkIn = reservation.getCheckIn();
        form.checkOut = reservation.getCheckOut();
        form.guests = reservation.getGuests();
        return form;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
}
