package com.tatalance;

import com.tatalance.activity.ActivityLogRepository;
import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.ClientRepository;
import com.tatalance.customtable.CustomTableRepository;
import com.tatalance.customtable.CustomTableRowRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.RideRepository; // updated during Category A Job model refactor (Issue #93)
import com.tatalance.ride.TimerService;
import com.tatalance.profile.ProfileRepository;
import com.tatalance.user.AppUserRepository;
import com.tatalance.user.AuthHelper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SecurityConfig.class, TimerService.class})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientRepository repository;

    // @WebMvcTest with no controller arg scans ALL controllers — Driver and Ride
    // need their repos mocked too or the context fails to load.
    @MockBean
    DriverRepository driverRepository;

    @MockBean
    RideRepository rideRepository;

    @MockBean
    CustomTableRepository customTableRepository;

    @MockBean
    CustomTableRowRepository customTableRowRepository;

    @MockBean
    InvoiceRepository invoiceRepository;

    @MockBean
    UserDetailsService userDetailsService;

    @MockBean
    AppUserRepository appUserRepository;

    @MockBean
    AuthHelper authHelper;

    @MockBean
    ProfileRepository profileRepository;

    @MockBean
    TimerService timerService;

    @MockBean
    ActivityLogger activityLogger;

    @MockBean
    ActivityLogRepository activityLogRepository;

    @Test
    void should_return401_when_apiRequestUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_returnOk_when_infoRequestUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.googleOAuthEnabled").value(false));
    }

    @Test
    void should_permitRegisterPage_withoutAuth() throws Exception {
        mockMvc.perform(get("/register.html"))
            .andExpect(status().isOk());
    }

    @Test
    void should_permitForgotPasswordPage_withoutAuth() throws Exception {
        mockMvc.perform(get("/forgot-password.html"))
            .andExpect(status().isOk());
    }

    @Test
    void should_redirectToCustomLoginPage_when_browserRequestUnauthenticated() throws Exception {
        // SecurityConfig.formLogin().loginPage("/login.html") — the mobile-friendly
        // login page (issue #35) replaced Spring Security's default /login form.
        mockMvc.perform(get("/api/clients").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login.html"));
    }

    @Test
    @WithMockUser
    void should_returnOk_when_authenticated() throws Exception {
        when(authHelper.getCurrentUserId()).thenReturn("testuser");
        when(repository.findByUserId(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk());
    }
}
