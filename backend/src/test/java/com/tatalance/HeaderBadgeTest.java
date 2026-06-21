package com.tatalance;

import com.tatalance.activity.ActivityLogRepository;
import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.ClientRepository;
import com.tatalance.customtable.CustomTableRepository;
import com.tatalance.customtable.CustomTableRowRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.JobRepository;
import com.tatalance.ride.RideRepository; // #93: RideRepository extends JobRepository now (jobs collection)
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
@WithMockUser
class HeaderBadgeTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientRepository repository;

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
    ActivityLogger activityLogger;

    @MockBean
    ActivityLogRepository activityLogRepository;

    @MockBean
    ProfileRepository profileRepository;

    @Test
    void should_haveDynamicDbBadgeElement() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"db-badge\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/info")));
    }
}
