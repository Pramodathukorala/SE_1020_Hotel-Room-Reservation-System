package com.hotel.reservation.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatting helpers used from the Thymeleaf templates as ${@fmt.money(price)}.
 * Keeping formatting in one place gives every page the same currency and date style.
 */
@Component("fmt")
public class Formats {

    public static final String CURRENCY = "LKR";

    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    public String money(BigDecimal amount) {
        return CURRENCY + " " + amount(amount);
    }

    /** "18500" -> "18,500.00" */
    public String amount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return format.format(amount);
    }

    /** "18500" -> "18,500" (for large headline prices). */
    public String wholeAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return format.format(amount);
    }

    public String date(LocalDate date) {
        return date == null ? "" : date.format(LONG_DATE);
    }

    public String shortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE);
    }

    public String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME);
    }

    public String nights(long nights) {
        return nights + (nights == 1 ? " night" : " nights");
    }
}
