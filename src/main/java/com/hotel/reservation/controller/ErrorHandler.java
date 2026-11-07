package com.hotel.reservation.controller;

import com.hotel.reservation.exception.HotelException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class ErrorHandler {
    @ExceptionHandler({HotelException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String invalid(Exception error, Model model, HttpServletResponse response) {
        response.setStatus(400);
        model.addAttribute("message",error instanceof HotelException ? error.getMessage() : "Some required values are missing or invalid. Check the form and try again.");
        return "error";
    }
}
