package com.hotel.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
