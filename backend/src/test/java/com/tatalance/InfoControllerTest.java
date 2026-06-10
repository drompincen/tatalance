package com.tatalance;

import com.tatalance.client.ClientRepository;
import com.tatalance.customtable.CustomTableRepository;
import com.tatalance.customtable.CustomTableRowRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.RideRepository;
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
class InfoControllerTest {

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

    @Test
    void should_returnEmbedded_when_noDbTypeConfigured() throws Exception {
        mockMvc.perform(get("/api/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dbType").value("embedded"));
    }
}
