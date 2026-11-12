package com.hotel.reservation.controller.admin;

import com.hotel.reservation.controller.FormErrors;
import com.hotel.reservation.dto.CustomerForm;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.Customer;
import com.hotel.reservation.service.CustomerService;
import com.hotel.reservation.service.ReservationService;
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

/** Customer management CRUD for administrators. */
@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final CustomerService customerService;
    private final ReservationService reservationService;

    public AdminCustomerController(CustomerService customerService, ReservationService reservationService) {
        this.customerService = customerService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("q", q);
        model.addAttribute("totalCustomers", customerService.count());
        return "admin/customers";
    }

    /** Customer profile with their reservation history. */
    @GetMapping("/{id}")
    public String view(@PathVariable String id, Model model) {
        model.addAttribute("customer", customerService.findById(id));
        model.addAttribute("reservations", reservationService.findDetailsByCustomer(id));
        return "admin/customer-detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return showForm(model, new CustomerForm(), null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("customerForm") CustomerForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return showForm(model, form, null);
        }
        try {
            Customer customer = customerService.register(form);
            redirect.addFlashAttribute("success", customer.getFullName() + " was registered as " + customer.getId() + ".");
            return "redirect:/admin/customers";
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, null);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        return showForm(model, CustomerForm.fromCustomer(customerService.findById(id)), id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("customerForm") CustomerForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        customerService.findById(id);
        if (result.hasErrors()) {
            return showForm(model, form, id);
        }
        try {
            Customer customer = customerService.update(id, form);
            redirect.addFlashAttribute("success", customer.getFullName() + "'s details were updated.");
            return "redirect:/admin/customers/" + id;
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return showForm(model, form, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirect) {
        try {
            Customer customer = customerService.findById(id);
            customerService.delete(id);
            redirect.addFlashAttribute("success", customer.getFullName() + " was deleted.");
            return "redirect:/admin/customers";
        } catch (HotelException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/customers";
        }
    }

    private String showForm(Model model, CustomerForm form, String customerId) {
        model.addAttribute("customerForm", form);
        model.addAttribute("customerId", customerId);
        return "admin/customer-form";
    }
}
