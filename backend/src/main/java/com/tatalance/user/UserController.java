package com.tatalance.user;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.*;

import com.tatalance.invoice.Invoice;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.invoice.TaxRateResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final InvoiceRepository invoiceRepository;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    public UserController(AppUserRepository repository, PasswordEncoder passwordEncoder,
                          InvoiceRepository invoiceRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        AppUser user = repository.findByUsername(auth.getName()).orElse(null);
        boolean googleLinked = user != null && user.getGoogleId() != null;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("username", auth.getName());
        out.put("googleLinked", googleLinked);
        out.put("googleOAuthEnabled", clientRegistrationRepository != null);
        if (user != null) {
            out.put("businessMode", user.getBusinessMode() != null ? user.getBusinessMode().name() : BusinessMode.CHAUFFEUR.name());
            out.put("defaultHourlyRate", user.getDefaultHourlyRate());
            out.put("defaultTaxRate", user.getDefaultTaxRate());
            out.put("defaultTaxRatePercent", taxRatePercent(user.getDefaultTaxRate()));
            out.put("venmoHandle", user.getVenmoHandle());
        } else {
            out.put("businessMode", BusinessMode.CHAUFFEUR.name());
            out.put("defaultHourlyRate", new BigDecimal("20.00"));
            out.put("defaultTaxRate", null);
            out.put("defaultTaxRatePercent", null);
            out.put("venmoHandle", null);
        }
        return out;
    }

    @PatchMapping("/me/settings")
    public ResponseEntity<?> updateSettings(Authentication auth, @RequestBody Map<String, Object> body) {
        AppUser user = repository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (body.containsKey("businessMode")) {
            try {
                BusinessMode mode = BusinessMode.valueOf(body.get("businessMode").toString().toUpperCase());
                user.setBusinessMode(mode);
                if (user.getDefaultTaxRate() == null && mode == BusinessMode.FREELANCE) {
                    user.setDefaultTaxRate(BigDecimal.ZERO);
                }
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid businessMode"));
            }
        }
        if (body.containsKey("defaultTaxRatePercent")) {
            Object raw = body.get("defaultTaxRatePercent");
            if (raw == null) {
                user.setDefaultTaxRate(null);
            } else if (raw instanceof Number n) {
                try {
                    user.setDefaultTaxRate(TaxRateResolver.fromPercent(n));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "defaultTaxRatePercent must be a number"));
            }
        } else if (body.containsKey("defaultTaxRate")) {
            Object raw = body.get("defaultTaxRate");
            if (raw == null) {
                user.setDefaultTaxRate(null);
            } else if (raw instanceof Number n) {
                user.setDefaultTaxRate(TaxRateResolver.clamp(new BigDecimal(n.toString())));
            }
        }
        if (body.containsKey("defaultHourlyRate")) {
            Object rate = body.get("defaultHourlyRate");
            if (rate instanceof Number n) {
                if (n.doubleValue() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Hourly rate must be positive"));
                }
                user.setDefaultHourlyRate(new BigDecimal(n.toString()));
            }
        }
        if (body.containsKey("venmoHandle")) {
            Object raw = body.get("venmoHandle");
            if (raw == null || raw.toString().isBlank()) {
                user.setVenmoHandle(null);
            } else {
                String handle = raw.toString().trim();
                if (!handle.startsWith("@")) {
                    handle = "@" + handle;
                }
                user.setVenmoHandle(handle);
            }
            syncInvoiceVenmoHandles(user);
        }
        repository.save(user);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("businessMode", user.getBusinessMode().name());
        out.put("defaultHourlyRate", user.getDefaultHourlyRate());
        out.put("defaultTaxRate", user.getDefaultTaxRate());
        out.put("defaultTaxRatePercent", taxRatePercent(user.getDefaultTaxRate()));
        out.put("venmoHandle", user.getVenmoHandle() != null ? user.getVenmoHandle() : "");
        return ResponseEntity.ok(out);
    }

    private void syncInvoiceVenmoHandles(AppUser user) {
        if (user.getId() == null) {
            return;
        }
        List<Invoice> invoices = invoiceRepository.findByUserId(user.getId());
        if (invoices.isEmpty()) {
            return;
        }
        String handle = user.getVenmoHandle();
        for (Invoice invoice : invoices) {
            invoice.setVenmoHandle(handle);
        }
        invoiceRepository.saveAll(invoices);
    }

    private static BigDecimal taxRatePercent(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        return rate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    @PostMapping("/link-google")
    public ResponseEntity<?> linkGoogle(Authentication auth, HttpSession session) {
        if (clientRegistrationRepository == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Google sign-in is not configured on this server (GOOGLE_CLIENT_ID missing)"));
        }
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
