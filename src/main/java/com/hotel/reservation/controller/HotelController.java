package com.hotel.reservation.controller;

import com.hotel.reservation.model.*;
import com.hotel.reservation.service.HotelService;
import com.hotel.reservation.exception.HotelException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class HotelController {
    private final HotelService hotel;
    public HotelController(HotelService hotel) { this.hotel = hotel; }
    @ModelAttribute public void common(Model model) { model.addAttribute("hotel", hotel); model.addAttribute("today", LocalDate.now()); }
    @GetMapping("/") public String home(Model model) { model.addAttribute("rooms", hotel.rooms().stream().limit(3).toList()); return "home"; }
    @GetMapping("/rooms") public String rooms(@RequestParam(defaultValue="") String q, @RequestParam(defaultValue="") String category,
            @RequestParam(required=false) LocalDate in, @RequestParam(required=false) LocalDate out, Model model) {
        if ((in == null) != (out == null)) throw new HotelException("Enter both arrival and departure dates.");
        if (in != null && (in.isBefore(LocalDate.now()) || !out.isAfter(in))) throw new HotelException("Choose future dates with departure after arrival.");
        model.addAttribute("rooms", hotel.rooms().stream().filter(r -> (r.getNumber()+r.getDescription()+r.getCategory()).toLowerCase().contains(q.toLowerCase()))
                .filter(r -> category.isBlank() || r.getCategory().equals(category)).filter(r -> in == null || hotel.available(r.getId(), in, out, null)).toList());
        model.addAttribute("q", q); model.addAttribute("category", category); model.addAttribute("in", in); model.addAttribute("out", out); return "rooms";
    }
    @GetMapping("/rooms/{id}") public String detail(@PathVariable String id, Model model) { model.addAttribute("room", hotel.room(id)); return "room"; }
    @GetMapping("/customers/register") public String register(Model model) { model.addAttribute("customer", new Customer()); return "customer-form"; }
    @GetMapping("/admin/customers/{id}/edit") public String editCustomer(@PathVariable String id, Model model) { model.addAttribute("customer", hotel.customer(id)); return "customer-form"; }
    @GetMapping("/admin/customers/{id}") public String profile(@PathVariable String id, Model model) { model.addAttribute("customer", hotel.customer(id)); model.addAttribute("bookings", hotel.reservations().stream().filter(r -> r.getCustomerId().equals(id)).toList()); return "profile"; }
    @PostMapping("/customers") public String saveCustomer(@RequestParam(required=false) String id, @RequestParam String name, @RequestParam String email, @RequestParam String phone, RedirectAttributes flash) {
        Customer customer = hotel.saveCustomer(id,name,email,phone); flash.addFlashAttribute("success", "Guest profile saved. You can now select this guest when booking."); return "redirect:/admin/customers/"+customer.getId();
    }
    @GetMapping("/book") public String booking(@RequestParam(required=false) String roomId, @RequestParam(required=false) String id, Model model) {
        Reservation reservation = id == null ? new Reservation() : hotel.reservation(id);
        if (id == null) { reservation.setRoomId(roomId); reservation.setGuests(1); }
        model.addAttribute("booking", reservation); model.addAttribute("rooms", hotel.rooms()); model.addAttribute("customers", hotel.customers()); return "booking";
    }
    @PostMapping("/book") public String book(@RequestParam(required=false) String id, @RequestParam String roomId, @RequestParam String customerId,
            @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut, @RequestParam int guests) {
        return "redirect:/confirmation/"+hotel.book(id,roomId,customerId,checkIn,checkOut,guests).getId();
    }
    @GetMapping("/confirmation/{id}") public String confirmation(@PathVariable String id, Model model) { model.addAttribute("booking",hotel.reservation(id)); return "confirmation"; }
    @GetMapping("/reservations") public String reservations(@RequestParam(defaultValue="") String q, Model model) {
        model.addAttribute("bookings",hotel.reservations().stream().filter(r -> (r.getId()+hotel.customer(r.getCustomerId()).getName()+hotel.room(r.getRoomId()).getNumber()).toLowerCase().contains(q.toLowerCase())).toList()); model.addAttribute("q",q); return "reservations";
    }
    @PostMapping("/reservations/{id}/cancel") public String cancel(@PathVariable String id, RedirectAttributes flash) { hotel.cancel(id); flash.addFlashAttribute("success","Reservation cancelled. The room is available again."); return "redirect:/reservations"; }
    @PostMapping("/reservations/{id}/delete") public String deleteBooking(@PathVariable String id, RedirectAttributes flash) { hotel.deleteReservation(id); flash.addFlashAttribute("success","Cancelled reservation deleted."); return "redirect:/reservations"; }
    @GetMapping("/admin") public String dashboard(Model model) {
        model.addAttribute("revenue",hotel.reservations().stream().filter(r -> !r.isCancelled()).map(Reservation::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add)); return "dashboard";
    }
    @GetMapping("/admin/rooms") public String adminRooms(Model model) { model.addAttribute("rooms",hotel.rooms()); return "room-management"; }
    @GetMapping("/admin/rooms/new") public String newRoom(Model model) { model.addAttribute("room",new StandardRoom()); return "room-form"; }
    @GetMapping("/admin/rooms/{id}/edit") public String editRoom(@PathVariable String id, Model model) { model.addAttribute("room",hotel.room(id)); return "room-form"; }
    @PostMapping("/admin/rooms") public String saveRoom(@RequestParam(required=false) String id, @RequestParam String category, @RequestParam String number,
            @RequestParam int capacity, @RequestParam BigDecimal basePrice, @RequestParam String description, @RequestParam(defaultValue="false") boolean maintenance, RedirectAttributes flash) {
        hotel.saveRoom(id,category,number,capacity,basePrice,description,maintenance); flash.addFlashAttribute("success","Room saved."); return "redirect:/admin/rooms";
    }
    @PostMapping("/admin/rooms/{id}/delete") public String deleteRoom(@PathVariable String id, RedirectAttributes flash) { hotel.deleteRoom(id); flash.addFlashAttribute("success","Room deleted."); return "redirect:/admin/rooms"; }
    @GetMapping("/admin/customers") public String customers(@RequestParam(defaultValue="") String q, Model model) {
        model.addAttribute("customers",hotel.customers().stream().filter(c -> (c.getName()+c.getEmail()+c.getPhone()).toLowerCase().contains(q.toLowerCase())).toList()); model.addAttribute("q",q); return "customers";
    }
    @PostMapping("/admin/customers/{id}/delete") public String deleteCustomer(@PathVariable String id, RedirectAttributes flash) { hotel.deleteCustomer(id); flash.addFlashAttribute("success","Guest deleted."); return "redirect:/admin/customers"; }
}
