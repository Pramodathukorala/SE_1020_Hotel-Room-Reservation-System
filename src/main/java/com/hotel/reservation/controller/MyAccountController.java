package com.hotel.reservation.controller;

import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.dto.ReservationDetails;
import com.hotel.reservation.dto.ReservationForm;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.Role;
import com.hotel.reservation.security.CurrentUser;
import com.hotel.reservation.security.SessionUser;
import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.util.Formats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Pages for a signed-in guest: all of their bookings (view, edit, cancel, delete)
 * and their profile. AuthInterceptor guarantees a guest is signed in before these run,
 * and every reservation method checks that the booking belongs to this guest.
 */
@Controller
public class MyAccountController {

    private final ReservationService reservationService;
    private final CustomerService customerService;
    private final Formats formats;

    public MyAccountController(ReservationService reservationService, CustomerService customerService, Formats formats) {
        this.reservationService = reservationService;
        this.customerService = customerService;
        this.formats = formats;
    }

    /**
     * Runs BEFORE the request is bound and validated (for "/my-bookings/{id}" requests):
     * the guest and the room of the booking are filled in from the server side, so the form
     * only needs the fields a guest may change (dates and number of guests).
     */
    @ModelAttribute("reservationForm")
    public ReservationForm reservationFormForBooking(@PathVariable(name = "id", required = false) String id,
                                                     HttpSession session) {
        ReservationForm form = new ReservationForm();
        SessionUser guest = CurrentUser.guest(session);
        if (id != null && guest != null) {
            Reservation reservation = reservationService.getDetailsForCustomer(id, guest.accountId()).reservation();
            form.setCustomerId(guest.accountId());
            form.setRoomId(reservation.getRoomId());
        }
        return form;
    }

    // ============================================================ bookings

    @GetMapping("/my-bookings")
    public String myBookings(HttpSession session, Model model) {
        String customerId = guestId(session);
        List<ReservationDetails> bookings = reservationService.findDetailsByCustomer(customerId);
        model.addAttribute("bookings", bookings);
        model.addAttribute("activeCount", bookings.stream().filter(d -> d.reservation().isActive()).count());
        model.addAttribute("customer", customerService.findById(customerId));
        model.addAttribute("today", reservationService.today());
        return "my-bookings";
    }

    @GetMapping("/my-bookings/{id}")
    public String viewBooking(@PathVariable String id, HttpSession session, Model model) {
        model.addAttribute("details", reservationService.getDetailsForCustomer(id, guestId(session)));
        model.addAttribute("today", reservationService.today());
        return "my-booking-detail";
    }

    @GetMapping("/my-bookings/{id}/edit")
    public String editForm(@PathVariable String id, HttpSession session, Model model, RedirectAttributes redirect) {
        ReservationDetails details = reservationService.getDetailsForCustomer(id, guestId(session));
        if (!details.reservation().isActive()) {
            redirect.addFlashAttribute("error", "Cancelled bookings cannot be changed.");
            return "redirect:/my-bookings/" + id;
        }
        return showEditForm(model, details, ReservationForm.fromReservation(details.reservation()));
    }

    /** UPDATE: new dates / number of guests; availability is re-checked and the total recalculated. */
    @PostMapping("/my-bookings/{id}")
    public String updateBooking(@PathVariable String id, @Valid @ModelAttribute("reservationForm") ReservationForm form,
                                BindingResult result, HttpSession session, Model model, RedirectAttributes redirect) {
        String customerId = guestId(session);
        ReservationDetails details = reservationService.getDetailsForCustomer(id, customerId);
        form.setCustomerId(customerId);
        form.setRoomId(details.reservation().getRoomId());
        if (result.hasErrors()) {
            return showEditForm(model, details, form);
        }
        try {
            Reservation updated = reservationService.updateForCustomer(id, customerId, form);
            redirect.addFlashAttribute("success", "Booking " + id + " was updated. New total: "
                    + formats.money(updated.getTotalPrice()) + ".");
            return "redirect:/my-bookings/" + id;
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showEditForm(model, details, form);
        }
    }

    @PostMapping("/my-bookings/{id}/cancel")
    public String cancelBooking(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        try {
            reservationService.cancelForCustomer(id, guestId(session));
            redirect.addFlashAttribute("success", "Booking " + id + " was cancelled. The room has been released.");
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    /** DELETE: removes a cancelled booking from the guest's list. */
    @PostMapping("/my-bookings/{id}/delete")
    public String deleteBooking(@PathVariable String id, HttpSession session, RedirectAttributes redirect) {
        try {
            reservationService.deleteForCustomer(id, guestId(session));
            redirect.addFlashAttribute("success", "Booking " + id + " was deleted.");
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    // ============================================================= profile

    @GetMapping("/account/profile")
    public String profileForm(HttpSession session, Model model) {
        model.addAttribute("customerForm", CustomerForm.fromCustomer(customerService.findById(guestId(session))));
        return "account-profile";
    }

    @PostMapping("/account/profile")
    public String updateProfile(@Valid @ModelAttribute("customerForm") CustomerForm form, BindingResult result,
                                HttpServletRequest request, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "account-profile";
        }
        try {
            String customerId = guestId(request.getSession());
            var customer = customerService.update(customerId, form);
            // keep the name shown in the navigation bar up to date
            request.getSession().setAttribute(CurrentUser.GUEST_KEY,
                    new SessionUser(customerId, customer.getFullName(), Role.GUEST));
            redirect.addFlashAttribute("success", "Your details were updated.");
            return "redirect:/my-bookings";
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return "account-profile";
        }
    }

    // ============================================================= helpers

    private String showEditForm(Model model, ReservationDetails details, ReservationForm form) {
        model.addAttribute("details", details);
        model.addAttribute("reservationForm", form);
        model.addAttribute("today", reservationService.today());
        if (form.getCheckIn() != null && form.getCheckOut() != null) {
            model.addAttribute("quote", reservationService.quote(details.reservation().getRoomId(), form.getCheckIn(),
                    form.getCheckOut(), form.getGuests() == null ? 1 : form.getGuests(), details.reservation().getId()));
        }
        return "my-booking-edit";
    }

    /** The signed-in guest's customer ID (AuthInterceptor has already checked that someone is signed in). */
    private static String guestId(HttpSession session) {
        return CurrentUser.guest(session).accountId();
    }
}
