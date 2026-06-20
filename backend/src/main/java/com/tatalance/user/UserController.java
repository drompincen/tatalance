package com.tatalance.user;

import jakarta.servlet.http.HttpSession;
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
    public Map<String, Object> me(Authentication auth) {
        AppUser user = repository.findByUsername(auth.getName()).orElse(null);
        boolean googleLinked = user != null && user.getGoogleId() != null;
        return Map.of("username", auth.getName(), "googleLinked", googleLinked);
    }

    @PostMapping("/link-google")
    public ResponseEntity<?> linkGoogle(Authentication auth, HttpSession session) {
        session.setAttribute("linkUsername", auth.getName());
        return ResponseEntity.ok(Map.of("redirect", "/oauth2/authorization/google"));
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

        String securityQuestion = body.get("securityQuestion");
        String securityAnswer = body.get("securityAnswer");
        if (securityQuestion != null && !securityQuestion.isBlank()
                && securityAnswer != null && !securityAnswer.isBlank()) {
            user.setSecurityQuestion(securityQuestion.trim());
            user.setSecurityAnswer(securityAnswer.trim().toLowerCase());
        }

        repository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created — you can now sign in"));
    }

    @PostMapping("/forgot-password/question")
    public ResponseEntity<?> forgotPasswordQuestion(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }

        return repository.findByUsername(username.trim())
                .filter(u -> u.getSecurityQuestion() != null)
                .map(u -> ResponseEntity.ok(Map.of("question", u.getSecurityQuestion())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No security question found for this account")));
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> forgotPasswordReset(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String answer = body.get("securityAnswer");
        String newPassword = body.get("newPassword");

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        if (answer == null || answer.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Security answer is required"));
        }
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 4 characters"));
        }

        AppUser user = repository.findByUsername(username.trim()).orElse(null);
        if (user == null || user.getSecurityQuestion() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No security question found for this account"));
        }

        if (!user.getSecurityAnswer().equals(answer.trim().toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect answer"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset — you can now sign in"));
    }
}
