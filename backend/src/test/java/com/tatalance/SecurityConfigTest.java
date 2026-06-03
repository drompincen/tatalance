package com.tatalance;

import com.tatalance.client.ClientRepository;
import com.tatalance.customtable.CustomTableRepository;
import com.tatalance.customtable.CustomTableRowRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.invoice.InvoiceRepository;
import com.tatalance.ride.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
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

    @Test
    void should_return401_when_apiRequestUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isUnauthorized());
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
        when(repository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk());
    }
}
