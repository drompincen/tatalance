package com.tatalance.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRepository repository;

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

        when(repository.findAll()).thenReturn(List.of(client));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }
}
