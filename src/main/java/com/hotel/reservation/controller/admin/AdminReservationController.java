package com.hotel.reservation.controller.admin;

import com.hotel.reservation.controller.FormErrors;
import com.hotel.reservation.dto.ReservationForm;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Reservation;
import com.hotel.reservation.model.ReservationStatus;
import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Reservation management for administrators: create, view, search, edit, cancel and delete. */
@Controller
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final RoomService roomService;
    private final CustomerService customerService;

    public AdminReservationController(ReservationService reservationService, RoomService roomService,
                                      CustomerService customerService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
        this.customerService = customerService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ReservationStatus status,
                       Model model) {
        model.addAttribute("reservations", reservationService.search(q, status));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", ReservationStatus.values());
        model.addAttribute("today", reservationService.today());
        return "admin/reservations";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model model) {
        model.addAttribute("details", reservationService.getDetails(id));
        model.addAttribute("today", reservationService.today());
        return "admin/reservation-detail";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) String customerId,
                          @RequestParam(required = false) String roomId, Model model) {
        ReservationForm form = new ReservationForm();
        form.setCustomerId(customerId);
        form.setRoomId(roomId);
        form.setCheckIn(reservationService.today().plusDays(1));
        form.setCheckOut(reservationService.today().plusDays(2));
        return showForm(model, form, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("reservationForm") ReservationForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(model, form, null);
        }
        try {
            Reservation reservation = reservationService.create(form);
            redirect.addFlashAttribute("success", "Reservation " + reservation.getId() + " was created.");
            return "redirect:/admin/reservations/" + reservation.getId();
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model, RedirectAttributes redirect) {
        Reservation reservation = reservationService.findById(id);
        if (!reservation.isActive()) {
            redirect.addFlashAttribute("error", "Cancelled reservations cannot be edited.");
            return "redirect:/admin/reservations/" + id;
        }
        return showForm(model, ReservationForm.fromReservation(reservation), id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("reservationForm") ReservationForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        Reservation existing = reservationService.findById(id);
        form.setCustomerId(existing.getCustomerId());   // the guest of a reservation cannot be changed
        if (result.hasErrors()) {
            return showForm(model, form, id);
        }
        try {
            Reservation reservation = reservationService.update(id, form);
            redirect.addFlashAttribute("success", "Reservation " + reservation.getId()
                    + " was updated. New total recalculated.");
            return "redirect:/admin/reservations/" + id;
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, id);
        }
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable String id, RedirectAttributes redirect) {
        try {
            reservationService.cancel(id);
            redirect.addFlashAttribute("success", "Reservation " + id + " was cancelled and the room released.");
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservations/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirect) {
        try {
            reservationService.delete(id);
            redirect.addFlashAttribute("success", "Reservation " + id + " was permanently deleted.");
            return "redirect:/admin/reservations";
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/reservations/" + id;
        }
    }

    private String showForm(Model model, ReservationForm form, String reservationId) {
        model.addAttribute("reservationForm", form);
        model.addAttribute("reservationId", reservationId);
        model.addAttribute("customers", customerService.findAll());
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("today", reservationService.today());
        if (form.getRoomId() != null && !form.getRoomId().isBlank() && form.getCheckIn() != null && form.getCheckOut() != null) {
            try {
                model.addAttribute("quote", reservationService.quote(form.getRoomId(), form.getCheckIn(),
                        form.getCheckOut(), form.getGuests() == null ? 1 : form.getGuests(), reservationId));
            } catch (HotelException ignored) {
                // unknown room: the form error explains it
            }
        }
        return "admin/reservation-form";
    }
}
