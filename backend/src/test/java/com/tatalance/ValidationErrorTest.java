package com.tatalance;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.client.ClientRepository;
import com.tatalance.driver.DriverRepository;
import com.tatalance.ride.RideRepository;
import com.tatalance.user.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {com.tatalance.client.ClientController.class, com.tatalance.driver.DriverController.class, com.tatalance.ride.RideController.class})
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
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("firstName")))
                .andExpect(jsonPath("$.errors[?(@.field=='firstName')].message").value("First name is required"));
    }

    @Test
    void should_returnFriendlyPhoneError() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"phone\":\"+123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='phone')].message[0]", containsString("+13055551234")));
    }

    @Test
    void should_returnFriendlyErrors_forDriver() throws Exception {
        mockMvc.perform(post("/api/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"Driver\",\"phone\":\"+15551234567\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("payoutType")));
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
}
