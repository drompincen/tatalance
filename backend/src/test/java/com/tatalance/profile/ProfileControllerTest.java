package com.tatalance.profile;

import com.tatalance.activity.ActivityLogger;
import com.tatalance.user.AuthHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    private static final String TEST_USER_ID = "user123";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfileRepository profileRepository;

    @MockBean
    private AuthHelper authHelper;

    @MockBean
    private ActivityLogger activityLogger;

    @BeforeEach
    void setUp() {
        when(authHelper.getCurrentUserId()).thenReturn(TEST_USER_ID);
    }

    private Profile sampleProfile() {
        Profile p = new Profile();
        p.setId("prof001");
        p.setUserId(TEST_USER_ID);
        p.setType(ProfileType.DRIVER);
        p.setName("Main Taxi");
        p.setCreatedAt(Instant.now());
        return p;
    }

    @Test
    void listProfiles_shouldReturnPage() throws Exception {
        when(profileRepository.findByUserId(eq(TEST_USER_ID), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(sampleProfile())));

        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk());

        verify(profileRepository).findByUserId(eq(TEST_USER_ID), any());
    }

    @Test
    void getById_shouldReturnProfile_whenOwned() throws Exception {
        Profile p = sampleProfile();
        when(profileRepository.findByIdAndUserId("prof001", TEST_USER_ID)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/profiles/prof001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prof001"));

        verify(profileRepository).findByIdAndUserId("prof001", TEST_USER_ID);
    }

    @Test
    void create_shouldSaveAndReturn201() throws Exception {
        when(profileRepository.save(any(Profile.class))).thenReturn(sampleProfile());

        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DRIVER\",\"name\":\"Main Taxi\"}"))
                .andExpect(status().isCreated());

        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void update_shouldModifyAndReturn200() throws Exception {
        Profile existing = sampleProfile();
        when(profileRepository.findByIdAndUserId("prof001", TEST_USER_ID)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Profile.class))).thenReturn(existing);

        mockMvc.perform(put("/api/profiles/prof001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ENGINEER\",\"name\":\"Updated\"}"))
                .andExpect(status().isOk());

        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void delete_shouldRemove() throws Exception {
        Profile p = sampleProfile();
        when(profileRepository.findByIdAndUserId("prof001", TEST_USER_ID)).thenReturn(Optional.of(p));

        mockMvc.perform(delete("/api/profiles/prof001"))
                .andExpect(status().isNoContent());

        verify(profileRepository).delete(p);
    }
}
