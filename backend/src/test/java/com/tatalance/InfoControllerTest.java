package com.tatalance;

import com.tatalance.client.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(SecurityConfig.class)
@WithMockUser
class InfoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ClientRepository repository;

    @Test
    void should_returnEmbedded_when_noDbTypeConfigured() throws Exception {
        mockMvc.perform(get("/api/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dbType").value("embedded"));
    }
}
