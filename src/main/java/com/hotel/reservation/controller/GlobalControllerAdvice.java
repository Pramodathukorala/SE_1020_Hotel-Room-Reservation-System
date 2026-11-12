package com.hotel.reservation.controller;

import com.hotel.reservation.exception.ResourceNotFoundException;
import com.hotel.reservation.exception.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @ExceptionHandler(StorageException.class)
    public String handleStorage(StorageException e, Model model, HttpServletResponse response) {
        log.error("Storage failure", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return errorPage(model, 500, "Our records are temporarily unavailable",
                "The data file could not be read or written. Please check the data folder and try again.");
    }

    private static String errorPage(Model model, int status, String title, String message) {
        model.addAttribute("status", status);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "error";
    }
}
