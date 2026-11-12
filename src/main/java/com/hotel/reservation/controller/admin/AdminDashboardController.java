package com.hotel.reservation.controller.admin;

import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
import com.hotel.reservation.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Admin overview: statistics cards, upcoming stays and room status. */
@Controller
public class AdminDashboardController {

    private final RoomService roomService;
    private final CustomerService customerService;
    private final ReservationService reservationService;

    public AdminDashboardController(RoomService roomService, CustomerService customerService,
                                    ReservationService reservationService) {
        this.roomService = roomService;
        this.customerService = customerService;
        this.reservationService = reservationService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("stats", reservationService.getStatistics());
        model.addAttribute("upcoming", reservationService.findUpcoming(6));
        model.addAttribute("rooms", roomService.findAll());
        model.addAttribute("recentCustomers", customerService.findAll().stream().limit(5).toList());
        model.addAttribute("today", reservationService.today());
        return "admin/dashboard";
    }
}
