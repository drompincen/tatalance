package com.tatalance.ride;

import com.tatalance.SecurityConfig;
import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.Client;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.Availability;
import com.tatalance.driver.Driver;
import com.tatalance.driver.DriverRepository;
import com.tatalance.profile.ProfileRepository;
import com.tatalance.user.AuthHelper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
@Import(SecurityConfig.class)
@WithMockUser
class RideControllerTest {

    private static final String TEST_USER_ID = "user123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideRepository rideRepository; // JobRepository base in #93; RideRepository extends for ride queries + type filter

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private DriverRepository driverRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthHelper authHelper;

    @MockBean
    private ActivityLogger activityLogger;

    @MockBean
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn(TEST_USER_ID);
    }

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
        ride.setPickupDateTime(Instant.parse("2028-06-01T14:00:00Z"));
        ride.setPickupLocation("Miami Airport");
        ride.setDropoffLocation("South Beach Hotel");
        ride.setBasePrice(new BigDecimal("85.00"));
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setCreatedAt(Instant.now());
        return ride;
    }

    private Driver sampleDriver() {
        var driver = new Driver();
        driver.setId("drv001");
        driver.setFirstName("Carlos");
        driver.setLastName("Mendez");
        driver.setPhone("+13055551002");
        driver.setAvailability(Availability.AVAILABLE);
        driver.setActive(true);
        return driver;
    }

    @Test
    void should_createRide_when_validInput() throws Exception {
        when(clientRepository.findByIdAndUserId("cli001", TEST_USER_ID)).thenReturn(Optional.of(sampleClient()));
        when(rideRepository.save(any(Ride.class))).thenReturn(sampleRide());

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-01T14:00:00Z",
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
                                {"pickupDateTime":"2028-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_pickupLocationMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-01T14:00:00Z",
                                 "dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_dropoffLocationMissing() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-01T14:00:00Z",
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
        when(clientRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"unknown","pickupDateTime":"2028-06-01T14:00:00Z",
                                 "pickupLocation":"Miami Airport","dropoffLocation":"South Beach Hotel"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listAllRides() throws Exception {
        when(rideRepository.findByUserId(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleRide())));

        mockMvc.perform(get("/api/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].clientName").value("Ana Torres"));
    }

    @Test
    void should_getRideById() throws Exception {
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(sampleRide()));

        mockMvc.perform(get("/api/rides/ride001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupLocation").value("Miami Airport"));
    }

    @Test
    void should_return404_when_rideNotFound() throws Exception {
        when(rideRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

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
    void should_assignDriver_when_available() throws Exception {
        var ride = sampleRide();
        var driver = sampleDriver();
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(driverRepository.findByIdAndUserId("drv001", TEST_USER_ID)).thenReturn(Optional.of(driver));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"driverId":"drv001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedDriverId").value("drv001"))
                .andExpect(jsonPath("$.assignedDriverName").value("Carlos Mendez"));
    }

    @Test
    void should_return400_when_driverNotAvailable() throws Exception {
        var ride = sampleRide();
        var driver = sampleDriver();
        driver.setAvailability(Availability.ON_TRIP);
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(driverRepository.findByIdAndUserId("drv001", TEST_USER_ID)).thenReturn(Optional.of(driver));

        mockMvc.perform(post("/api/rides/ride001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"driverId":"drv001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_driverIdMissing() throws Exception {
        var ride = sampleRide();
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_rideNotFound_forAssign() throws Exception {
        when(rideRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rides/unknown/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"driverId":"drv001"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return400_when_driverNotFound() throws Exception {
        var ride = sampleRide();
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(driverRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rides/ride001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"driverId":"unknown"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listAvailableDrivers() throws Exception {
        var driver = sampleDriver();
        when(driverRepository.findByAvailability(Availability.AVAILABLE)).thenReturn(List.of(driver));

        mockMvc.perform(get("/api/drivers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("Carlos"));
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
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.actualStart").exists());
    }

    @Test
    void should_return404_when_startingMissingRide() throws Exception {
        when(rideRepository.findByIdAndUserId("missing", TEST_USER_ID)).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/rides/missing/start"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return409_when_startingAlreadyCompletedRide() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/start"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_return409_when_startingAlreadyInProgressRide() throws Exception {
        // M4 (#34) — double-start guard
        var ride = sampleRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/start"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_completeRide_andCalculateBillable() throws Exception {
        // M4 (#34) — complete endpoint, transitions to COMPLETED + billable = base + extras
        var ride = sampleRide();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setBasePrice(new BigDecimal("85.00"));
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
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
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void should_updateScheduledRide() throws Exception {
        var ride = sampleRide();
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(clientRepository.findByIdAndUserId("cli001", TEST_USER_ID)).thenReturn(Optional.of(sampleClient()));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/rides/ride001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-02T10:00:00Z",
                                 "pickupLocation":"Brickell","dropoffLocation":"Wynwood",
                                 "basePrice":60.00,"notes":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pickupLocation").value("Brickell"))
                .andExpect(jsonPath("$.dropoffLocation").value("Wynwood"))
                .andExpect(jsonPath("$.notes").value("Updated"));
    }

    @Test
    void should_return400_when_updatingAssignedRide() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.ASSIGNED);
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(put("/api/rides/ride001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-02T10:00:00Z",
                                 "pickupLocation":"Brickell","dropoffLocation":"Wynwood"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_updatingNonexistentRide() throws Exception {
        when(rideRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/rides/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-02T10:00:00Z",
                                 "pickupLocation":"Brickell","dropoffLocation":"Wynwood"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_cancelScheduledRide() throws Exception {
        var ride = sampleRide();
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void should_cancelAssignedRide_and_freeDriver() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedDriverId("drv001");
        var driver = sampleDriver();
        driver.setAvailability(Availability.ON_TRIP);

        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverRepository.findByIdAndUserId("drv001", TEST_USER_ID)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/rides/ride001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void should_return400_when_cancellingCompletedRide() throws Exception {
        var ride = sampleRide();
        ride.setStatus(RideStatus.COMPLETED);
        when(rideRepository.findByIdAndUserId("ride001", TEST_USER_ID)).thenReturn(Optional.of(ride));

        mockMvc.perform(post("/api/rides/ride001/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_cancellingNonexistentRide() throws Exception {
        when(rideRepository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/rides/unknown/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_createServiceJob_forFreelance_Issue93() throws Exception {
        // Category D: direct /jobs endpoint for SERVICE jobs (freelance/developer) — Issue #93
        when(clientRepository.findByIdAndUserId("cli001", TEST_USER_ID)).thenReturn(Optional.of(sampleClient()));
        when(rideRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0);
            j.setId("job001");
            return j;
        });

        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":"cli001","pickupDateTime":"2028-06-10T10:00:00Z",
                                 "pickupLocation":"Landing page dev","dropoffLocation":"Freelance Job",
                                 "pricingMode":"HOURLY","hourlyRate":20,"notes":"EST:3|scope"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("job001"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.clientName").value("Ana Torres"))
                .andExpect(jsonPath("$.hourlyRate").value(20));
    }
}
