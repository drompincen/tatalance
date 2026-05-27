package com.tatalance.driver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DriverIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection("drivers");
    }

    @Test
    void should_createAndListDriver() {
        var request = Map.of(
                "firstName", "Carlos",
                "lastName", "Mendez",
                "phone", "+13055551002",
                "vehicle", "2024 Mercedes S-Class",
                "payoutType", "PERCENTAGE",
                "payoutRate", 70
        );

        var createResponse = restTemplate.postForEntity("/api/drivers", request, Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).containsKeys("id", "firstName", "lastName", "createdAt", "availability");
        assertThat(createResponse.getBody().get("firstName")).isEqualTo("Carlos");
        assertThat(createResponse.getBody().get("availability")).isEqualTo("AVAILABLE");

        var listResponse = restTemplate.getForEntity("/api/drivers", List.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
    }

    @Test
    void should_getDriverById() {
        var request = Map.of(
                "firstName", "Mike",
                "lastName", "Johnson",
                "phone", "+19545551003",
                "payoutType", "FLAT",
                "payoutRate", 35
        );
        var created = restTemplate.postForEntity("/api/drivers", request, Map.class);
        var id = created.getBody().get("id").toString();

        var response = restTemplate.getForEntity("/api/drivers/" + id, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("firstName")).isEqualTo("Mike");
    }

    @Test
    void should_return404_when_driverNotFound() {
        var response = restTemplate.getForEntity("/api/drivers/nonexistent", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_updateAvailability() {
        var request = Map.of(
                "firstName", "Ana",
                "lastName", "Torres",
                "phone", "+17865551004",
                "payoutType", "PERCENTAGE",
                "payoutRate", 65
        );
        var created = restTemplate.postForEntity("/api/drivers", request, Map.class);
        var id = created.getBody().get("id").toString();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var patchBody = new HttpEntity<>(Map.of("availability", "OFF_DUTY"), headers);

        var patchResponse = restTemplate.exchange(
                "/api/drivers/" + id + "/availability",
                HttpMethod.PATCH, patchBody, Map.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody().get("availability")).isEqualTo("OFF_DUTY");

        // Verify persistence
        var getResponse = restTemplate.getForEntity("/api/drivers/" + id, Map.class);
        assertThat(getResponse.getBody().get("availability")).isEqualTo("OFF_DUTY");
    }

    @Test
    void should_return400_when_firstNameMissing() {
        var request = Map.of("lastName", "Mendez", "phone", "+13055551002", "payoutType", "PERCENTAGE", "payoutRate", 70);
        var response = restTemplate.postForEntity("/api/drivers", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_updateDriver() {
        var request = Map.of("firstName", "Carlos", "lastName", "Mendez", "phone", "+13055551002",
                "payoutType", "PERCENTAGE", "payoutRate", 70);
        var created = restTemplate.postForEntity("/api/drivers", request, Map.class);
        var id = (String) created.getBody().get("id");

        var update = Map.of("firstName", "Carlos", "lastName", "Garcia", "phone", "+13055551002",
                "vehicle", "2025 BMW 7", "payoutType", "FLAT", "payoutRate", 50);
        var response = restTemplate.exchange("/api/drivers/" + id, HttpMethod.PUT,
                new HttpEntity<>(update), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("lastName")).isEqualTo("Garcia");
        assertThat(response.getBody().get("vehicle")).isEqualTo("2025 BMW 7");
    }

    @Test
    void should_deleteDriver() {
        var request = Map.of("firstName", "Carlos", "lastName", "Mendez", "phone", "+13055551002",
                "payoutType", "PERCENTAGE", "payoutRate", 70);
        var created = restTemplate.postForEntity("/api/drivers", request, Map.class);
        var id = (String) created.getBody().get("id");

        var response = restTemplate.exchange("/api/drivers/" + id, HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var list = restTemplate.getForEntity("/api/drivers", List.class);
        assertThat(list.getBody()).isEmpty();
    }

    @Test
    void should_return404_when_deletingNonexistentDriver() {
        var response = restTemplate.exchange("/api/drivers/nonexistent", HttpMethod.DELETE, null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
