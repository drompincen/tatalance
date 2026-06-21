package com.tatalance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ValidationErrorTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void authenticate() {
        this.restTemplate = restTemplate.withBasicAuth("admin", "admin");
    }

    @Test
    void should_returnStructuredErrors_when_clientValidationFails() {
        var request = Map.of("phone", "bad");
        var response = restTemplate.postForEntity("/api/clients", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var body = response.getBody();
        assertThat(body).containsKey("errors");
        var errors = (List<Map<String, String>>) body.get("errors");
        assertThat(errors).isNotEmpty();

        var fields = errors.stream().map(e -> e.get("field")).toList();
        assertThat(fields).contains("firstName");

        var firstNameError = errors.stream()
                .filter(e -> "firstName".equals(e.get("field")))
                .findFirst().orElseThrow();
        assertThat(firstNameError.get("message")).isEqualTo("First name is required");
    }

    @Test
    void should_returnFriendlyPhoneError() {
        var request = Map.of("firstName", "Test", "lastName", "User", "phone", "+123");
        var response = restTemplate.postForEntity("/api/clients", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var errors = (List<Map<String, String>>) response.getBody().get("errors");
        var phoneError = errors.stream()
                .filter(e -> "phone".equals(e.get("field")))
                .findFirst().orElseThrow();
        assertThat(phoneError.get("message")).contains("+13055551234");
    }

    @Test
    void should_returnFriendlyErrors_forDriver() {
        var request = Map.of("firstName", "Test", "lastName", "Driver", "phone", "+15551234567");
        var response = restTemplate.postForEntity("/api/drivers", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var errors = (List<Map<String, String>>) response.getBody().get("errors");
        var fields = errors.stream().map(e -> e.get("field")).toList();
        assertThat(fields).contains("payoutType");
    }

    @Test
    void should_returnFriendlyErrors_forRide() { // updated post #93: scheduled/pickupDateTime not @NotNull enforced at create (optional per jobs MVP; only Ride location fields are)
        var request = Map.of("clientId", "x");
        var response = restTemplate.postForEntity("/api/rides", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var errors = (List<Map<String, String>>) response.getBody().get("errors");
        var fields = errors.stream().map(e -> e.get("field")).toList();
        assertThat(fields).contains("pickupLocation", "dropoffLocation");
    }
}
