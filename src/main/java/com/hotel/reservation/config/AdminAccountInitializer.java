package com.hotel.reservation.config;

import com.hotel.reservation.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Makes sure there is always at least one administrator.
 *
 * When the "admins" table is empty, one admin is created with the username and password
 * from the settings hotel.admin.username / hotel.admin.password (put them in the git-ignored
 * application-local.properties). If no password is configured, a random one is generated
 * and printed ONCE in the console. Existing admins are never changed.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AccountService accountService;
    private final String username;
    private final String password;

    public AdminAccountInitializer(AccountService accountService,
                                   @Value("${hotel.admin.username:admin}") String username,
                                   @Value("${hotel.admin.password:}") String password) {
        this.accountService = accountService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountService.countAdmins() > 0) {
            return;
        }
        String initialPassword = password;
        if (initialPassword == null || initialPassword.isBlank()) {
            byte[] random = new byte[6];
            new SecureRandom().nextBytes(random);
            initialPassword = "Adm-" + HexFormat.of().formatHex(random);
            log.warn("No hotel.admin.password configured. Generated admin password for '{}': {}", username, initialPassword);
        }
        accountService.createAdmin(username, "Hotel Administrator", initialPassword);
        log.info("Created the first administrator account '{}'.", username);
    }
}
