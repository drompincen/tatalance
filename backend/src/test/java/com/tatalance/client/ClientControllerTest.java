package com.tatalance.client;

import com.tatalance.SecurityConfig;
import com.tatalance.activity.ActivityLogger;
import com.tatalance.ride.Job;
import com.tatalance.ride.RideRepository;
import com.tatalance.ride.RideStatus; // updated for Job base model refactor #93 (Category A)
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

@WebMvcTest(ClientController.class)
@Import(SecurityConfig.class)
@WithMockUser
class ClientControllerTest {

    private static final String TEST_USER_ID = "user123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRepository repository;

    @MockBean
    private RideRepository rideRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthHelper authHelper;

    @MockBean
    private ActivityLogger activityLogger;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn(TEST_USER_ID);
    }

    @Test
    void should_returnCreatedClient_when_validFirstNameAndLastName() throws Exception {
        var saved = new Client();
        saved.setId("abc123");
        saved.setFirstName("John");
        saved.setLastName("Doe");
        saved.setPhone("+12125551234");
        saved.setCreatedAt(Instant.now());

        when(repository.save(any(Client.class))).thenReturn(saved);

        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","phone":"+12125551234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.id").value("abc123"));
    }

    @Test
    void should_return400_when_firstNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Doe","phone":"+12125551234"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_lastNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"","phone":"+12125551234"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_phoneIsBlank() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","phone":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_phoneIsTooShort() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","phone":"+123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_phoneMissingPlus() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","phone":"12125551234"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_phoneHasLetters() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"John","lastName":"Doe","phone":"+1212555abc4"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_returnClientList_when_getClients() throws Exception {
        var client = new Client();
        client.setId("abc123");
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setPhone("+12125551234");
        client.setCreatedAt(Instant.now());

        when(repository.findByUserId(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(client)));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"));
    }

    @Test
    void should_getClientById() throws Exception {
        var client = new Client();
        client.setId("abc123");
        client.setFirstName("John");
        client.setLastName("Doe");
        client.setPhone("+12125551234");
        when(repository.findByIdAndUserId("abc123", TEST_USER_ID)).thenReturn(Optional.of(client));

        mockMvc.perform(get("/api/clients/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void should_return404_when_clientNotFound() throws Exception {
        when(repository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/clients/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_updateClient() throws Exception {
        var existing = new Client();
        existing.setId("abc123");
        existing.setFirstName("John");
        existing.setLastName("Doe");
        existing.setPhone("+12125551234");
        existing.setCreatedAt(Instant.now());

        when(repository.findByIdAndUserId("abc123", TEST_USER_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/clients/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","lastName":"Smith","phone":"+13055559999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.phone").value("+13055559999"));
    }

    @Test
    void should_return404_when_updatingNonexistentClient() throws Exception {
        when(repository.findByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/clients/unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Jane","lastName":"Smith","phone":"+13055559999"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_deleteClient() throws Exception {
        when(repository.existsByIdAndUserId("abc123", TEST_USER_ID)).thenReturn(true);
        when(rideRepository.findByUserIdAndClientIdAndStatusIn(eq(TEST_USER_ID), eq("abc123"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(delete("/api/clients/abc123"))
                .andExpect(status().isNoContent());
        verify(repository).deleteById("abc123");
    }

    @Test
    void should_return400_when_clientHasActiveRides() throws Exception {
        when(repository.existsByIdAndUserId("abc123", TEST_USER_ID)).thenReturn(true);
        var ride = new com.tatalance.ride.Ride();
        ride.setStatus(RideStatus.SCHEDULED);
        when(rideRepository.findByUserIdAndClientIdAndStatusIn(eq(TEST_USER_ID), eq("abc123"), any()))
                .thenReturn(List.of(ride));

        mockMvc.perform(delete("/api/clients/abc123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_deletingNonexistentClient() throws Exception {
        when(repository.existsByIdAndUserId("unknown", TEST_USER_ID)).thenReturn(false);

        mockMvc.perform(delete("/api/clients/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_createClient_withEmail() throws Exception {
        when(repository.save(any(Client.class))).thenAnswer(i -> {
            Client c = i.getArgument(0);
            c.setId("cliNew");
            return c;
        });
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"E\",\"lastName\":\"Mail\",\"phone\":\"+12125551234\",\"email\":\"e@ex.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("e@ex.com"));
    }
}
