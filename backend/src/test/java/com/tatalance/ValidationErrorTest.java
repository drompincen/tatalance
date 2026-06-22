package com.tatalance;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.ride.RideRepository;
import com.tatalance.user.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

@WebMvcTest(controllers = {com.tatalance.client.ClientController.class, com.tatalance.driver.DriverController.class, com.tatalance.ride.RideController.class})
@AutoConfigureMockMvc(addFilters = false)
class ValidationErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private DriverRepository driverRepository;

    @MockBean
    private RideRepository rideRepository;

    @MockBean
    private AuthHelper authHelper;

    @MockBean
    private ActivityLogger activityLogger;

    @MockBean
    private com.tatalance.ride.TimerService timerService;

    @MockBean
    private com.tatalance.profile.ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn("test-user");
    }

    @Test
    void should_returnStructuredErrors_when_clientValidationFails() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[*].field", hasItems("firstName", "lastName", "phone")));
    }

    @Test
    void should_returnFriendlyPhoneError() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"phone\":\"+123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phone')].message").exists());
    }

    @Test
    void should_returnFriendlyErrors_forDriver() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"Driver\",\"phone\":\"+15551234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("payoutType", "payoutRate")));
    }

    @Test
    void should_returnFriendlyErrors_forRide() throws Exception {
        // updated post #93: scheduled/pickupDateTime not @NotNull enforced at create (optional per jobs MVP; only Ride location fields are)
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("pickupLocation", "dropoffLocation")));
    }

    @Test
    void should_handleResponseStatusException() throws Exception {
        // e.g. past date or bad profile in ride create
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"cli1\",\"pickupDateTime\":\"2020-01-01T00:00:00Z\",\"pickupLocation\":\"x\",\"dropoffLocation\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("past")));
    }

    @Test
    void should_handleUnreadableForPayoutType() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"T\",\"lastName\":\"D\",\"phone\":\"+1234567890\",\"payoutType\":\"BAD\",\"payoutRate\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("Payout type must be one of")));
    }

    @Test
    void should_handleUnreadableForAvailability() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"T\",\"lastName\":\"D\",\"phone\":\"+1234567890\",\"payoutType\":\"PERCENTAGE\",\"payoutRate\":10,\"availability\":\"BAD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("Availability must be one of")));
    }

    @Test
    void should_returnError_forMissingLastName() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"phone\":\"+1234567890\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("lastName")));
    }

    @Test
    void should_returnError_forMissingPickupLocationInRide() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"cli1\",\"dropoffLocation\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("pickupLocation")));
    }

    @Test
    void should_returnError_forMissingClientIdInRide() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"x\",\"dropoffLocation\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("Client not found")));
    }

    @Test
    void should_returnError_forAssignDriverIdMissing() throws Exception {
        when(rideRepository.findByIdAndUserId(eq("ride001"), anyString())).thenReturn(Optional.of(new com.tatalance.ride.Ride()));
        mockMvc.perform(post("/api/rides/ride001/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("driverId is required")));
    }

    @Test
    void should_returnError_forDriverCreateMissingPayoutRate() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"C\",\"lastName\":\"D\",\"phone\":\"+13055551002\",\"payoutType\":\"FLAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("payoutRate")));
    }

    @Test
    void should_returnError_forDriverCreateMissingPayoutType() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"C\",\"lastName\":\"D\",\"phone\":\"+13055551002\",\"payoutRate\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("payoutType")));
    }

    @Test
    void should_handleResponseStatus_forRidePastDate() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"cli1\",\"pickupDateTime\":\"2020-01-01T00:00:00Z\",\"pickupLocation\":\"x\",\"dropoffLocation\":\"y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("past")));
    }

    @Test
    void should_returnError_forDriverMissingPhone() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"C\",\"lastName\":\"D\",\"payoutType\":\"FLAT\",\"payoutRate\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("phone")));
    }

    @Test
    void should_returnError_forClientMissingFirstName() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"D\",\"phone\":\"+13055551002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("firstName")));
    }
}
