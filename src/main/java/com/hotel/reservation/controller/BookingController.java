package com.hotel.reservation.controller;

import com.hotel.reservation.dto.BookingForm;
import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.dto.ReservationDetails;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.security.CurrentUser;
import com.hotel.reservation.security.SessionUser;
import com.hotel.reservation.service.CustomerService;
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
 * Handles the guest booking pages:
 *   1. /book                     - show the booking form
 *   2. POST /book                - save the booking
 *   3. /booking/confirmation/... - show the "thank you" page
 *   4. /manage-booking           - find or cancel a booking with reference + email
 *
 * A controller only receives the web request and chooses the page to show.
 * All booking rules are in ReservationService (separation of concerns).
 */
@Controller
public class BookingController {

    // name used to remember this browser's bookings in the session
    private static final String SESSION_BOOKINGS = "myBookings";

    // OOP - ENCAPSULATION: the services this controller needs, kept private and final
    private final RoomService roomService;
    private final ReservationService reservationService;
    private final CustomerService customerService;

    // Constructor injection: Spring creates the services and passes them in here
    public BookingController(RoomService roomService, ReservationService reservationService,
                             CustomerService customerService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
        this.customerService = customerService;
    }

    /** STEP 1 - show the booking form for a room (dates and guests come from the URL). */
    @GetMapping("/book")
    public String bookingForm(@RequestParam String roomId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                              @RequestParam(required = false) Integer guests,
                              HttpSession session, Model model) {
        roomService.findById(roomId);   // shows a 404 page if the room does not exist

        // fill the form with the values from the URL
        BookingForm form = new BookingForm();
        form.setRoomId(roomId);
        form.setCheckIn(checkIn);
        form.setCheckOut(checkOut);
        form.setGuests(guests == null ? 1 : guests);   // default: 1 guest

        useSignedInGuest(form, session);   // signed in? use the guest's saved details
        model.addAttribute("bookingForm", form);
        return showForm(form, model);
    }

    /** STEP 2 - the guest pressed "Confirm reservation". */
    @PostMapping("/book")
    public String submitBooking(@Valid @ModelAttribute("bookingForm") BookingForm form, BindingResult result,
                                Model model, HttpSession session) {
        // @Valid checks the form (empty fields, email format...); problems are put in "result"
        roomService.findById(form.getRoomId());
        boolean signedIn = useSignedInGuest(form, session);

        // if the form has mistakes, show it again with the error messages
        if (hasRelevantErrors(result, signedIn)) {
            return showForm(form, model);
        }
        try {
            // the service checks the dates, capacity and double booking, then saves to MySQL
            Reservation reservation = reservationService.book(form);
            rememberBooking(session, reservation.getId());
            return "redirect:/booking/confirmation/" + reservation.getId();   // go to the thank-you page
        } catch (HotelException e) {
            // OOP - POLYMORPHISM: this one catch handles every child of HotelException
            // (ValidationException, BookingConflictException, ...)
            FormErrors.apply(result, e);   // show the error message on the form
            return showForm(form, model);
        }
    }

    /** "Update price" button: recalculates the total for new dates (guest details are not checked). */
    @PostMapping("/book/quote")
    public String refreshQuote(@ModelAttribute("bookingForm") BookingForm form, HttpSession session, Model model) {
        useSignedInGuest(form, session);
        return showForm(form, model);
    }

    /** STEP 3 - the confirmation page. Only the browser that made the booking can open it. */
    @GetMapping("/booking/confirmation/{id}")
    public String confirmation(@PathVariable String id, HttpSession session, Model model,
                               RedirectAttributes redirect) {
        // someone else's booking reference? send them to "Manage booking" instead
        if (!myBookings(session).contains(id)) {
            redirect.addFlashAttribute("info", "Please enter your booking reference and email to view your booking.");
            return "redirect:/manage-booking";
        }
        model.addAttribute("details", reservationService.getDetails(id));
        return "confirmation";
    }

    // ===================================================== manage booking

    /** Shows the "Manage booking" form (reference + email). */
    @GetMapping("/manage-booking")
    public String manageBookingForm() {
        return "manage-booking";
    }

    /** Finds a booking by reference + email. */
    @PostMapping("/manage-booking")
    public String findBooking(@RequestParam String reference, @RequestParam String email, Model model) {
        model.addAttribute("reference", reference);   // keep the typed values in the form
        model.addAttribute("email", email);
        try {
            model.addAttribute("details", reservationService.findForGuest(reference, email));
        } catch (HotelException e) {
            model.addAttribute("lookupError", e.getMessage());   // e.g. "No booking matches..."
        }
        return "manage-booking";
    }

    /** Cancels a booking found by reference + email. */
    @PostMapping("/manage-booking/cancel")
    public String cancelBooking(@RequestParam String reference, @RequestParam String email, Model model) {
        model.addAttribute("reference", reference);
        model.addAttribute("email", email);
        try {
            ReservationDetails details = reservationService.findForGuest(reference, email);   // check it is theirs
            reservationService.cancel(details.reservation().getId());                       // cancel it
            model.addAttribute("details", reservationService.getDetails(details.reservation().getId()));
            model.addAttribute("lookupSuccess", "Your reservation has been cancelled. The room has been released.");
        } catch (HotelException e) {
            model.addAttribute("lookupError", e.getMessage());
        }
        return "manage-booking";
    }

    // ============================================================ helpers

    /**
     * If a guest is signed in, put their saved profile into the form.
     * This way nobody can book in another person's name. Returns true if signed in.
     */
    private boolean useSignedInGuest(BookingForm form, HttpSession session) {
        SessionUser guest = CurrentUser.guest(session);
        if (guest != null) {
            form.setCustomer(CustomerForm.fromCustomer(customerService.findById(guest.accountId())));
            return true;
        }
        return false;
    }

    /** For a signed-in guest the "customer.*" fields were replaced, so errors in them are ignored. */
    private static boolean hasRelevantErrors(BindingResult result, boolean signedIn) {
        if (!signedIn) {
            return result.hasErrors();
        }
        return result.hasGlobalErrors()
                || result.getFieldErrors().stream().anyMatch(error -> !error.getField().startsWith("customer."));
    }

    /** Prepares the data the booking page needs (room, price, today's date) and opens booking.html. */
    private String showForm(BookingForm form, Model model) {
        int guests = form.getGuests() == null ? 1 : form.getGuests();
        model.addAttribute("room", roomService.findById(form.getRoomId()));
        // price + availability check for the chosen dates
        model.addAttribute("quote", reservationService.quote(form.getRoomId(), form.getCheckIn(), form.getCheckOut(), guests));
        model.addAttribute("today", reservationService.today());
        return "booking";   // = templates/booking.html
    }

    /** The booking references made in this browser session. */
    @SuppressWarnings("unchecked")
    private static Set<String> myBookings(HttpSession session) {
        Object stored = session.getAttribute(SESSION_BOOKINGS);
        return stored instanceof Set<?> set ? (Set<String>) set : Set.of();
    }

    /** Remembers a new booking reference in the session (so the confirmation page can be opened). */
    private static void rememberBooking(HttpSession session, String reservationId) {
        Set<String> bookings = new HashSet<>(myBookings(session));
        bookings.add(reservationId);
        session.setAttribute(SESSION_BOOKINGS, bookings);
    }
}
