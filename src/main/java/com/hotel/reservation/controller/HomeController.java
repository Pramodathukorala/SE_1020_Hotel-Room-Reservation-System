package com.hotel.reservation.controller;

import com.hotel.reservation.model.RoomType;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/** Public landing page. */
@Controller
public class HomeController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    public HomeController(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = reservationService.today();
        model.addAttribute("featuredRooms", roomService.findFeatured());
        model.addAttribute("roomCount", roomService.count());
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("defaultCheckIn", today.plusDays(1));
        model.addAttribute("defaultCheckOut", today.plusDays(3));
        model.addAttribute("today", today);
        return "home";
    }
}
