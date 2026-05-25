package com.tatalance.driver;

import com.tatalance.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
@Import(SecurityConfig.class)
@WithMockUser
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverRepository repository;

    private Driver sampleDriver() {
        var driver = new Driver();
        driver.setId("drv001");
        driver.setFirstName("Carlos");
        driver.setLastName("Mendez");
        driver.setPhone("+13055551002");
        driver.setEmail("carlos@example.com");
        driver.setVehicle("2024 Mercedes S-Class");
        driver.setPayoutType(PayoutType.PERCENTAGE);
        driver.setPayoutRate(new BigDecimal("70"));
        driver.setAvailability(Availability.AVAILABLE);
        driver.setActive(true);
        driver.setCreatedAt(Instant.now());
        return driver;
    }

    @Test
    void should_returnCreatedDriver_when_validInput() throws Exception {
        when(repository.save(any(Driver.class))).thenReturn(sampleDriver());

        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Mendez","phone":"+13055551002",
                                 "vehicle":"2024 Mercedes S-Class","payoutType":"PERCENTAGE","payoutRate":70}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Carlos"))
                .andExpect(jsonPath("$.lastName").value("Mendez"))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.id").value("drv001"));
    }

    @Test
    void should_return400_when_firstNameBlank() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Mendez","phone":"+13055551002",
                                 "payoutType":"PERCENTAGE","payoutRate":70}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_lastNameBlank() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"","phone":"+13055551002",
                                 "payoutType":"PERCENTAGE","payoutRate":70}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_phoneInvalid() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Mendez","phone":"+123",
                                 "payoutType":"PERCENTAGE","payoutRate":70}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_payoutTypeMissing() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Mendez","phone":"+13055551002",
                                 "payoutRate":70}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_payoutRateMissing() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Mendez","phone":"+13055551002",
                                 "payoutType":"PERCENTAGE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnDriverList() throws Exception {
        when(repository.findAll()).thenReturn(List.of(sampleDriver()));

        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Carlos"));
    }

    @Test
    void should_returnDriver_when_idExists() throws Exception {
        when(repository.findById("drv001")).thenReturn(Optional.of(sampleDriver()));

        mockMvc.perform(get("/api/drivers/drv001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Carlos"));
    }

    @Test
    void should_return404_when_idNotFound() throws Exception {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/drivers/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_updateAvailability() throws Exception {
        var driver = sampleDriver();
        when(repository.findById("drv001")).thenReturn(Optional.of(driver));
        when(repository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/drivers/drv001/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availability":"OFF_DUTY"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("OFF_DUTY"));
    }

    @Test
    void should_return400_when_invalidAvailability() throws Exception {
        when(repository.findById("drv001")).thenReturn(Optional.of(sampleDriver()));

        mockMvc.perform(patch("/api/drivers/drv001/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"availability":"SLEEPING"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
