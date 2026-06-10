package com.tatalance.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserController(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication auth) {
        return Map.of("username", auth.getName());
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is required"));
        }
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 4 characters"));
        }

        AppUser user = repository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        if (password == null || password.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 4 characters"));
        }
        if (repository.existsByUsername(username.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username already taken"));
        }

        AppUser user = new AppUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setCreatedAt(Instant.now());
        repository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created — you can now sign in"));
    }
}
