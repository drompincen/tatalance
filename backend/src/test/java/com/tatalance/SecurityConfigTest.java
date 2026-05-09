package com.tatalance;

import com.tatalance.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientRepository repository;

    @Test
    void should_return401_when_apiRequestUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void should_redirectToLogin_when_browserRequestUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/clients").accept(MediaType.TEXT_HTML))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void should_returnOk_when_loginPageAccessedUnauthenticated() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void should_returnOk_when_authenticated() throws Exception {
        when(repository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/clients"))
            .andExpect(status().isOk());
    }
}
