package com.hotel.reservation.controller.admin;

import com.hotel.reservation.controller.FormErrors;
import com.hotel.reservation.dto.RoomForm;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Room;
import com.hotel.reservation.model.RoomType;
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

/** Room management CRUD for administrators. */
@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /** READ: list + search + filter by type. */
    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) RoomType type,
                       Model model) {
        model.addAttribute("rooms", roomService.search(q, type, null, null, null));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("totalRooms", roomService.count());
        return "admin/rooms";
    }

    /** CREATE (step 1): empty form. */
    @GetMapping("/new")
    public String newForm(Model model) {
        RoomForm form = new RoomForm();
        form.setType(RoomType.STANDARD);
        form.setCapacity(2);
        return showForm(model, form, null);
    }

    /** CREATE (step 2): validate and save. */
    @PostMapping
    public String create(@Valid @ModelAttribute("roomForm") RoomForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(model, form, null);
        }
        try {
            Room room = roomService.create(form);
            redirect.addFlashAttribute("success", "Room " + room.getRoomNumber() + " was added.");
            return "redirect:/admin/rooms";
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, null);
        }
    }

    /** UPDATE (step 1): form pre-filled with the current values. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        return showForm(model, RoomForm.fromRoom(roomService.findById(id)), id);
    }

    /** UPDATE (step 2): validate and save. */
    @PostMapping("/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("roomForm") RoomForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        roomService.findById(id);
        if (result.hasErrors()) {
            return showForm(model, form, id);
        }
        try {
            Room room = roomService.update(id, form);
            redirect.addFlashAttribute("success", "Room " + room.getRoomNumber() + " was updated.");
            return "redirect:/admin/rooms";
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, id);
        }
    }

    /** DELETE: refused (with a message) if the room has reservations. */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirect) {
        try {
            Room room = roomService.findById(id);
            roomService.delete(id);
            redirect.addFlashAttribute("success", "Room " + room.getRoomNumber() + " was deleted.");
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    private String showForm(Model model, RoomForm form, String roomId) {
        model.addAttribute("roomForm", form);
        model.addAttribute("roomId", roomId);
        model.addAttribute("roomTypes", RoomType.values());
        model.addAttribute("imageOptions", RoomImages.OPTIONS);
        return "admin/room-form";
    }
}
