package com.tatalance.driver;

import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverRepository repository;

    @MockBean
    private RideRepository rideRepository;

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

    @Test
    void should_updateDriver() throws Exception {
        when(repository.findById("drv001")).thenReturn(Optional.of(sampleDriver()));
        when(repository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/drivers/drv001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Garcia","phone":"+13055551002",
                                 "vehicle":"2025 BMW 7 Series","payoutType":"FLAT","payoutRate":50}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Garcia"))
                .andExpect(jsonPath("$.vehicle").value("2025 BMW 7 Series"))
                .andExpect(jsonPath("$.payoutType").value("FLAT"))
                .andExpect(jsonPath("$.payoutRate").value(50));
    }

    @Test
    void should_return404_when_updatingNonexistentDriver() throws Exception {
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/drivers/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Carlos","lastName":"Garcia","phone":"+13055551002",
                                 "payoutType":"FLAT","payoutRate":50}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_deleteDriver() throws Exception {
        when(repository.existsById("drv001")).thenReturn(true);
        when(rideRepository.findByAssignedDriverIdAndStatusIn(eq("drv001"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(delete("/api/drivers/drv001"))
                .andExpect(status().isNoContent());
        verify(repository).deleteById("drv001");
    }

    @Test
    void should_return400_when_driverHasActiveRides() throws Exception {
        when(repository.existsById("drv001")).thenReturn(true);
        var ride = new com.tatalance.ride.Ride();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findByAssignedDriverIdAndStatusIn(eq("drv001"), any()))
                .thenReturn(List.of(ride));

        mockMvc.perform(delete("/api/drivers/drv001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_deletingNonexistentDriver() throws Exception {
        when(repository.existsById("unknown")).thenReturn(false);

        mockMvc.perform(delete("/api/drivers/unknown"))
                .andExpect(status().isNotFound());
    }
}
