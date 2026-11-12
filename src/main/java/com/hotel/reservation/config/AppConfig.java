package com.hotel.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

/** Application-wide beans. */
@Configuration
public class AppConfig {

    /**
     * The system clock is provided as a bean instead of calling LocalDate.now() directly,
     * so automated tests can replace it with a fixed date.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * BCrypt turns a password into a salted, slow, one-way hash. The same password gives a
     * different hash every time, and the original password cannot be recovered from it.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
