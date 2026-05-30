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

    @Test
    void should_listRidesByDriver_when_driverAssigned() throws Exception {
        // M3 (#33) — Driver queue endpoint. Returns rides where assignedDriverId
        // matches, sorted by pickupDateTime ascending.
        var ride = sampleRide();
        ride.setAssignedDriverId("drv001");
        when(rideRepository.findByAssignedDriverIdOrderByPickupDateTimeAsc("drv001"))
                .thenReturn(List.of(ride));

        mockMvc.perform(get("/api/drivers/drv001/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].assignedDriverId").value("drv001"))
                .andExpect(jsonPath("$[0].pickupLocation").value("Miami Airport"));
    }

    @Test
    void should_returnEmptyList_when_driverHasNoRides() throws Exception {
        when(rideRepository.findByAssignedDriverIdOrderByPickupDateTimeAsc("drv999"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/drivers/drv999/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void should_startRide_when_scheduled() throws Exception {
        // M4 (#34) — start endpoint, transitions to IN_PROGRESS + actualStart=now
        var ride = sampleRide();
        ride.setStatus(RideStatus.SCHEDULED);
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.actualStart").exists());
    }

    @Test
    void should_return404_when_startingMissingRide() throws Exception {
        when(rideRepository.findById("missing")).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/rides/missing/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return409_when_startingAlreadyCompletedRide() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/start"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_completeRide_andCalculateBillable() throws Exception {
        // M4 (#34) — complete endpoint, transitions to COMPLETED + billable = base + extras
        var ride = sampleRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setBasePrice(new BigDecimal("85.00"));
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tolls": 8.50, "parking": 12.00, "additionalCharges": 15.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.actualEnd").exists())
                .andExpect(jsonPath("$.billableAmount").value(120.50));
    }

    @Test
    void should_return409_when_completingScheduledRide() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.SCHEDULED);
        when(rideRepository.findById("ride001")).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }
}
