package com.tatalance.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import com.tatalance.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "testuser")
class UserControllerTest {

    private static final String USERNAME = "testuser";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository repository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserDetailsService userDetailsService;

    private AppUser sampleUser() {
        AppUser u = new AppUser();
        u.setUsername(USERNAME);
        u.setPassword("encoded-old");
        u.setRole("USER");
        u.setBusinessMode(BusinessMode.CHAUFFEUR);
        u.setDefaultHourlyRate(new BigDecimal("20.00"));
        return u;
    }

    @BeforeEach
    void setUp() {
        // default
    }

    @Test
    void me_returnsUserInfo_whenUserExists_noGoogle() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/me").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.googleLinked").value(false))
                .andExpect(jsonPath("$.googleOAuthEnabled").value(false)) // no client reg bean
                .andExpect(jsonPath("$.businessMode").value("CHAUFFEUR"));

        verify(repository).findByUsername(USERNAME);
    }

    @Test
    void me_returnsDefaults_whenUserNotFound() throws Exception {
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.businessMode").value("CHAUFFEUR"))
                .andExpect(jsonPath("$.defaultHourlyRate").value(20.00));
    }

    @Test
    void me_returnsGoogleLinkedTrue_whenGoogleIdSet() throws Exception {
        AppUser u = sampleUser();
        u.setGoogleId("g-123");
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));

        mockMvc.perform(get("/api/users/me").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    void updateSettings_businessModeValid_updatesAndReturns() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(repository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessMode\":\"FREELANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessMode").value("FREELANCE"));

        verify(repository).save(any(AppUser.class));
    }

    @Test
    void updateSettings_invalidBusinessMode_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));

        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessMode\":\"INVALID_MODE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid businessMode"));
    }

    @Test
    void updateSettings_negativeHourlyRate_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));

        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultHourlyRate\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hourly rate must be positive"));
    }

    @Test
    void updateSettings_validRate_updates() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultHourlyRate\": 45.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultHourlyRate").value(45.5));
    }

    @Test
    void changePassword_success() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("oldpass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-new");
        when(repository.save(any())).thenReturn(u);

        mockMvc.perform(post("/api/users/change-password").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"oldpass\",\"newPassword\":\"newpass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(repository).save(u);
    }

    @Test
    void changePassword_missingCurrent_returns400() throws Exception {
        mockMvc.perform(post("/api/users/change-password").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newpass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is required"));
    }

    @Test
    void changePassword_tooShortNew_returns400() throws Exception {
        mockMvc.perform(post("/api/users/change-password").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be at least 4 characters"));
    }

    @Test
    void changePassword_wrongCurrent_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        mockMvc.perform(post("/api/users/change-password").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void register_success() throws Exception {
        when(repository.existsByUsername("newu")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("enc");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newu\",\"password\":\"pass1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Account created — you can now sign in"));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(repository.existsByUsername("taken")).thenReturn(true);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"taken\",\"password\":\"pass1234\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withSecurityQuestion_savesIt() throws Exception {
        when(repository.existsByUsername("secu")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("e");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"secu\",\"password\":\"pass1234\",\"securityQuestion\":\"Pet?\",\"securityAnswer\":\"Fluffy\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void forgotPasswordQuestion_success() throws Exception {
        AppUser u = sampleUser();
        u.setSecurityQuestion("Your pet?");
        when(repository.findByUsername("secu")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/users/forgot-password/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"secu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Your pet?"));
    }

    @Test
    void forgotPasswordQuestion_noQuestion_returns404() throws Exception {
        AppUser u = sampleUser();
        u.setSecurityQuestion(null);
        when(repository.findByUsername("secu")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/users/forgot-password/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"secu\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void forgotPasswordReset_success() throws Exception {
        AppUser u = sampleUser();
        u.setSecurityQuestion("q");
        u.setSecurityAnswer("ans");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newp")).thenReturn("enc2");
        when(repository.save(any())).thenReturn(u);

        mockMvc.perform(post("/api/users/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"securityAnswer\":\"ans\",\"newPassword\":\"newp\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset — you can now sign in"));
    }

    @Test
    void forgotPasswordReset_wrongAnswer_returns400() throws Exception {
        AppUser u = sampleUser();
        u.setSecurityQuestion("q");
        u.setSecurityAnswer("correct");
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/users/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"securityAnswer\":\"wrong\",\"newPassword\":\"newp\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect answer"));
    }

    @Test
    void updateSettings_withBothModeAndRate() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessMode\":\"FREELANCE\",\"defaultHourlyRate\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessMode").value("FREELANCE"))
                .andExpect(jsonPath("$.defaultHourlyRate").value(99));
    }

    @Test
    void register_withoutSecurityQuestion_succeeds() throws Exception {
        when(repository.existsByUsername("plain")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("enc");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"plain\",\"password\":\"pass1234\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void me_withGoogleLinked_andEnabled() throws Exception {
        AppUser u = sampleUser();
        u.setGoogleId("g123");
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        // client reg is mocked in class? but to hit enabled true branch if present
        mockMvc.perform(get("/api/users/me").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleLinked").value(true));
    }

    @Test
    void updateSettings_zeroRate_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultHourlyRate\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hourly rate must be positive"));
    }

    @Test
    void forgotPasswordQuestion_noUser_returns404() throws Exception {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/users/forgot-password/question")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changePassword_newTooShort_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        mockMvc.perform(post("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"oldpass\",\"newPassword\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be at least 4 characters"));
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSettings_businessModeInvalid_returns400() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessMode\":\"BAD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid businessMode"));
    }

    @Test
    void changePassword_currentBlank_returns400() throws Exception {
        mockMvc.perform(post("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"newpass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is required"));
    }

    @Test
    void forgotPasswordReset_newShort_returns400() throws Exception {
        when(repository.findByUsername("u")).thenReturn(Optional.of(sampleUser()));
        mockMvc.perform(post("/api/users/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"securityAnswer\":\"ans\",\"newPassword\":\"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void me_defaultBusinessMode_whenNoUser() throws Exception {
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/users/me").with(user(USERNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessMode").value("CHAUFFEUR"))
                .andExpect(jsonPath("$.defaultHourlyRate").value(20.00));
    }

    @Test
    void forgotPasswordReset_noQuestion_returns404() throws Exception {
        AppUser u = sampleUser();
        u.setSecurityQuestion(null);
        when(repository.findByUsername("u")).thenReturn(Optional.of(u));
        mockMvc.perform(post("/api/users/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"u\",\"securityAnswer\":\"ans\",\"newPassword\":\"newp\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void register_duplicateUsername() throws Exception {
        when(repository.existsByUsername("dup")).thenReturn(true);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup\",\"password\":\"pass1234\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already taken"));
    }

    @Test
    void updateSettings_noChange() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(patch("/api/users/me/settings").with(user(USERNAME))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessMode").value("CHAUFFEUR"));
    }

    @Test
    void changePassword_success_updates() throws Exception {
        AppUser u = sampleUser();
        when(repository.findByUsername(USERNAME)).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new4")).thenReturn("encnew");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        mockMvc.perform(post("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old\",\"newPassword\":\"new4\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPasswordReset_noUser_returns404() throws Exception {
        when(repository.findByUsername("no")).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/users/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no\",\"securityAnswer\":\"ans\",\"newPassword\":\"newp\"}"))
                .andExpect(status().isNotFound());
    }
}
