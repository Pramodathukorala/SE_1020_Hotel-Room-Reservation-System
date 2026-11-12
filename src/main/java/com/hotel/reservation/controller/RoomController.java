package com.hotel.reservation.controller;

import com.hotel.reservation.exception.ValidationException;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Public room catalogue: listing with filters, and the room details page with a live price check. */
@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    public RoomController(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public String listRooms(@RequestParam(required = false) String q,
                            @RequestParam(required = false) RoomType type,
                            @RequestParam(required = false) BigDecimal maxPrice,
                            @RequestParam(required = false) Integer guests,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                            @RequestParam(required = false) String sort,
                            Model model) {
        // guests out of the valid range simply means "no guest filter"
        Integer guestFilter = (guests != null && guests >= 1 && guests <= Room.MAX_CAPACITY) ? guests : null;
        List<Room> rooms = roomService.search(q, type, maxPrice, guestFilter, sort).stream()
                .filter(Room::isBookable)
                .toList();

        boolean datesSearched = false;
        if (checkIn != null || checkOut != null) {
            try {
                if (checkIn != null && checkIn.isBefore(reservationService.today())) {
                    throw new ValidationException("Check-in date cannot be in the past.");
                }
                rooms = reservationService.findAvailableRooms(rooms, checkIn, checkOut, guestFilter);
                datesSearched = true;
            } catch (ValidationException e) {
                model.addAttribute("dateError", e.getMessage());
            }
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("guests", guestFilter);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("sort", sort);
        model.addAttribute("datesSearched", datesSearched);
        model.addAttribute("today", reservationService.today());
        return "rooms";
    }

    @GetMapping("/{id}")
    public String roomDetails(@PathVariable String id,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                              @RequestParam(required = false) Integer guests,
                              Model model) {
        Room room = roomService.findById(id);
        int guestCount = guests == null ? Math.min(2, room.getCapacity()) : guests;
        if (checkIn != null || checkOut != null) {
            model.addAttribute("quote", reservationService.quote(id, checkIn, checkOut, guestCount));
        }
        model.addAttribute("room", room);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guestCount);
        model.addAttribute("today", reservationService.today());
        model.addAttribute("otherRooms", roomService.findAll().stream()
                .filter(r -> !r.getId().equals(id) && r.isBookable())
                .filter(r -> r.getType() == room.getType() || r.getCapacity() >= room.getCapacity())
                .limit(3)
                .toList());
        return "room-detail";
    }
}
