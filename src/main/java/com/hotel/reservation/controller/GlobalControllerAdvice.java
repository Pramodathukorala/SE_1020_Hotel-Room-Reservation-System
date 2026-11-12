package com.hotel.reservation.controller;

import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.security.CurrentUser;
import com.hotel.reservation.security.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Applies to every controller:
 * - exposes the current URL so the navigation can highlight the active page;
 * - turns exceptions into friendly error pages instead of stack traces.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /** The signed-in guest (or null), used by the navigation bar. */
    @ModelAttribute("currentGuest")
    public SessionUser currentGuest(HttpServletRequest request) {
        return CurrentUser.guest(request.getSession(false));
    }

    /** The signed-in administrator (or null), used by the admin sidebar. */
    @ModelAttribute("currentAdmin")
    public SessionUser currentAdmin(HttpServletRequest request) {
        return CurrentUser.admin(request.getSession(false));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException e, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return errorPage(model, 404, "We couldn't find that", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleBadParameter(MethodArgumentTypeMismatchException e, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return errorPage(model, 400, "That request didn't look right",
                "The value '" + e.getValue() + "' is not valid for " + e.getName() + ".");
    }

    /**
     * A database constraint (unique key, foreign key, check) rejected a change.
     * The services check these rules first, so this is only a safety net.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleConstraint(DataIntegrityViolationException e, Model model, HttpServletResponse response) {
        log.warn("Database constraint violation: {}", e.getMostSpecificCause().getMessage());
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        return errorPage(model, 409, "That change conflicts with existing records",
                "The database rejected the change because it would break a rule (for example a duplicate value "
                        + "or a record that is still in use). Please go back and try again.");
    }

    /** Any other database problem, e.g. MySQL is not running. */
    @ExceptionHandler(DataAccessException.class)
    public String handleDatabase(DataAccessException e, Model model, HttpServletResponse response) {
        log.error("Database failure", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return errorPage(model, 500, "Our records are temporarily unavailable",
                "The database could not be reached. Please check that MySQL is running and try again.");
    }

    private static String errorPage(Model model, int status, String title, String message) {
        model.addAttribute("status", status);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "error";
    }
}
