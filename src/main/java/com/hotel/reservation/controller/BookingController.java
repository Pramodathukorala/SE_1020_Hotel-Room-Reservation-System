package com.hotel.reservation.controller;

import com.hotel.reservation.dto.BookingForm;
import com.hotel.reservation.dto.ReservationDetails;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Guest booking workflow: booking form -> reservation -> confirmation,
 * plus "Manage booking" where a guest can look up or cancel their own reservation.
 */
@Controller
public class BookingController {

    private static final String SESSION_BOOKINGS = "myBookings";

    private final RoomService roomService;
    private final ReservationService reservationService;

    public BookingController(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    @GetMapping("/book")
    public String bookingForm(@RequestParam String roomId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                              @RequestParam(required = false) Integer guests,
                              Model model) {
        roomService.findById(roomId);   // 404 if the room does not exist
        BookingForm form = new BookingForm();
        form.setRoomId(roomId);
        form.setCheckIn(checkIn);
        form.setCheckOut(checkOut);
        form.setGuests(guests == null ? 1 : guests);
        model.addAttribute("bookingForm", form);
        return showForm(form, model);
    }

    @PostMapping("/book")
    public String submitBooking(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult result,
                                Model model, HttpSession session) {
        roomService.findById(form.getRoomId());
        if (result.hasErrors()) {
            return showForm(form, model);
        }
        try {
            Reservation reservation = reservationService.book(form);
            rememberBooking(session, reservation.getId());
            return "redirect:/booking/confirmation/" + reservation.getId();
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(form, model);
        }
    }

    /** "Update price" button: re-calculates the summary for new dates without validating guest details. */
    @PostMapping("/book/quote")
    public String refreshQuote(@ModelAttribute("bookingForm") BookingForm form, Model model) {
        return showForm(form, model);
    }

    /** Only the browser session that made the booking may open the confirmation page directly. */
    @GetMapping("/booking/confirmation/{id}")
    public String confirmation(@PathVariable String id, HttpSession session, Model model,
                               RedirectAttributes redirect) {
        if (!myBookings(session).contains(id)) {
            redirect.addFlashAttribute("info", "Please enter your booking reference and email to view your booking.");
            return "redirect:/manage-booking";
        }
        model.addAttribute("details", reservationService.getDetails(id));
        return "confirmation";
    }

    // --------------------------------------------------------- manage booking

    @GetMapping("/manage-booking")
    public String manageBookingForm() {
        return "manage-booking";
    }

    @PostMapping("/manage-booking")
    public String findBooking(@RequestParam String reference, @RequestParam String email, Model model) {
        model.addAttribute("reference", reference);
        model.addAttribute("email", email);
        try {
            model.addAttribute("details", reservationService.findForGuest(reference, email));
        } catch (HotelException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "manage-booking";
    }

    @PostMapping("/manage-booking/cancel")
    public String cancelBooking(@RequestParam String reference, @RequestParam String email, Model model) {
        model.addAttribute("reference", reference);
        model.addAttribute("email", email);
        try {
            ReservationDetails details = reservationService.findForGuest(reference, email);
            reservationService.cancel(details.reservation().getId());
            model.addAttribute("details", reservationService.getDetails(details.reservation().getId()));
            model.addAttribute("success", "Your reservation has been cancelled. The room has been released.");
        } catch (HotelException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "manage-booking";
    }

    // ---------------------------------------------------------------- helpers

    private String showForm(BookingForm form, Model model) {
        int guests = form.getGuests() == null ? 1 : form.getGuests();
        model.addAttribute("room", roomService.findById(form.getRoomId()));
        model.addAttribute("quote", reservationService.quote(form.getRoomId(), form.getCheckIn(), form.getCheckOut(), guests));
        model.addAttribute("today", reservationService.today());
        return "booking";
    }

    @SuppressWarnings("unchecked")
    private static Set<String> myBookings(HttpSession session) {
        Object stored = session.getAttribute(SESSION_BOOKINGS);
        return stored instanceof Set<?> set ? (Set<String>) set : Set.of();
    }

    private static void rememberBooking(HttpSession session, String reservationId) {
        Set<String> bookings = new HashSet<>(myBookings(session));
        bookings.add(reservationId);
        session.setAttribute(SESSION_BOOKINGS, bookings);
    }
}
