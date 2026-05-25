package com.tatalance;

import com.tatalance.client.ClientRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.ride.RideRepository;
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

    @Test
    void should_haveDynamicDbBadgeElement() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"db-badge\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/info")));
    }
}
