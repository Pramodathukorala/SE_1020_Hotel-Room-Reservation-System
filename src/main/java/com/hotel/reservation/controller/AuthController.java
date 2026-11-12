package com.hotel.reservation.controller;

import com.hotel.reservation.dto.RegistrationForm;
import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.model.GuestAccount;
import com.hotel.reservation.security.CurrentUser;
import com.hotel.reservation.security.SessionUser;
import com.hotel.reservation.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Guest registration, guest sign-in/out and the separate administrator sign-in/out. */
@Controller
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ================================================================= guests

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form, BindingResult result,
                           HttpServletRequest request, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            GuestAccount account = accountService.registerGuest(form);
            SessionUser user = accountService.sessionFor(account);
            CurrentUser.signIn(request, user);
            redirect.addFlashAttribute("success", "Welcome, " + user.firstName() + "! Your account has been created.");
            return "redirect:" + account.getHomePage();
        } catch (HotelException e) {
            FormErrors.apply(result, e);
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String next, Model model) {
        model.addAttribute("next", safeNext(next, null));
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        @RequestParam(required = false) String next,
                        HttpServletRequest request, Model model, RedirectAttributes redirect) {
        try {
            SessionUser user = accountService.authenticateGuest(email, password);
            CurrentUser.signIn(request, user);
            redirect.addFlashAttribute("success", "Welcome back, " + user.firstName() + ".");
            return "redirect:" + safeNext(next, "/my-bookings");
        } catch (HotelException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("next", safeNext(next, null));
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirect) {
        CurrentUser.signOutGuest(session);
        redirect.addFlashAttribute("success", "You have been signed out.");
        return "redirect:/";
    }

    // ================================================================= admins

    @GetMapping("/admin/login")
    public String adminLoginForm(@RequestParam(required = false) String next, HttpSession session, Model model) {
        if (CurrentUser.admin(session) != null) {
            return "redirect:/admin";
        }
        model.addAttribute("next", safeNext(next, null));
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String username, @RequestParam String password,
                             @RequestParam(required = false) String next,
                             HttpServletRequest request, Model model) {
        try {
            SessionUser admin = accountService.authenticateAdmin(username, password);
            CurrentUser.signIn(request, admin);
            return "redirect:" + safeNext(next, "/admin");
        } catch (HotelException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("next", safeNext(next, null));
            return "admin/login";
        }
    }

    @PostMapping("/admin/logout")
    public String adminLogout(HttpSession session, RedirectAttributes redirect) {
        CurrentUser.signOutAdmin(session);
        redirect.addFlashAttribute("success", "You have been signed out of the admin console.");
        return "redirect:/admin/login";
    }

    /**
     * Only allows redirects to pages of THIS website (a path like "/my-bookings"),
     * never to another site ("//evil.com" or "https://..."), to prevent "open redirect" attacks.
     */
    static String safeNext(String next, String fallback) {
        if (next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")) {
            return next;
        }
        return fallback;
    }
}
