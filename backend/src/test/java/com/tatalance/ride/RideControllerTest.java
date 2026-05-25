package com.tatalance.ride;

import com.tatalance.SecurityConfig;
import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import(SecurityConfig.class)
@WithMockUser
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideRepository rideRepository;

    @MockBean
    private ClientRepository clientRepository;

    private Client sampleClient() {
        var client = new Client();
        client.setId("cli001");
        client.setFirstName("Ana");
        client.setLastName("Torres");
        client.setPhone("+17865551004");
        client.setCreatedAt(Instant.now());
        return client;
    }

    private Ride sampleRide() {
        var ride = new Ride();
        ride.setId("ride001");
        ride.setClientId("cli001");
        ride.setClientName("Ana Torres");
        ride.setPickupDateTime(Instant.parse("2026-06-01T14:00:00Z"));
        ride.setPickupLocation("Miami Airport");
        ride.setDropoffLocation("South Beach Hotel");
        ride.setBasePrice(new BigDecimal("85.00"));
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setCreatedAt(Instant.now());
        return ride;
    }

    @Test
    void should_createRide_when_validInput() throws Exception {
        when(clientRepository.findById("cli001")).thenReturn(Optional.of(sampleClient()));
        when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide());

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2026-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel",
                                 "basePrice":85.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Ana Torres"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.pickupLocation").value("Miami Airport"));
    }

    @Test
    void should_return400_when_clientIdMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pickupDateTime":"2026-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_pickupLocationMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2026-06-01T14:00:00Z",
                                 "dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_dropoffLocationMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2026-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_pickupDateTimeMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_clientNotFound() throws Exception {
        when(clientRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"unknown","pickupDateTime":"2026-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listAllRides() throws Exception {
        when(rideRepository.findAll()).thenReturn(List.of(sampleRide()));

        mockMvc.perform(get("/api/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clientName").value("Ana Torres"));
    }

    @Test
    void should_getRideById() throws Exception {
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(sampleRide()));

        mockMvc.perform(get("/api/rides/ride001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupLocation").value("Miami Airport"));
    }

    @Test
    void should_return404_when_rideNotFound() throws Exception {
        when(rideRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rides/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_listRidesByClient() throws Exception {
        when(rideRepository.findByClientId("cli001")).thenReturn(List.of(sampleRide()));

        mockMvc.perform(get("/api/clients/cli001/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clientId").value("cli001"));
    }
}
