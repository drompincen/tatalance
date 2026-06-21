package com.tatalance.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(1)
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            if (repository.count() == 0) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole("USER");
                admin.setCreatedAt(Instant.now());
                repository.save(admin);
                log.info("Seeded default admin user");
            }
        } catch (Exception e) {
            log.warn("UserSeeder skipped (Mongo not available or error): {}", e.getMessage());
        }
    }
}
