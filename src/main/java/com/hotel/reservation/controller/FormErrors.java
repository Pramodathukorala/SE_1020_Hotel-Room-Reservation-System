package com.hotel.reservation.controller;

import com.hotel.reservation.exception.HotelException;
import com.hotel.reservation.exception.ValidationException;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.validation.BindingResult;

/**
 * Copies a business-rule error from the service layer into Spring's BindingResult,
 * so it is displayed beside the matching form field (or at the top of the form).
 */
public final class FormErrors {

    private FormErrors() {
    }

    public static void apply(BindingResult result, HotelException error, String fieldPrefix) {
        String field = error instanceof ValidationException validation ? validation.getField() : null;
        if (field != null && result.getTarget() != null) {
            String path = fieldPrefix + field;
            if (new BeanWrapperImpl(result.getTarget()).isReadableProperty(path)) {
                result.rejectValue(path, "business", error.getMessage());
                return;
            }
        }
        result.reject("business", error.getMessage());
    }

    public static void apply(BindingResult result, HotelException error) {
        apply(result, error, "");
    }
}
