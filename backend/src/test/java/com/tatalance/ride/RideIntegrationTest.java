package com.tatalance.ride;

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
class RideIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection("rides");
        mongoTemplate.dropCollection("clients");
        this.restTemplate = restTemplate.withBasicAuth("admin", "admin");
    }

    private String createClient() {
        var client = Map.of(
                "firstName", "Ana",
                "lastName", "Torres",
                "phone", "+17865551004"
        );
        var response = restTemplate.postForEntity("/api/clients", client, Map.class);
        return response.getBody().get("id").toString();
    }

    @Test
    void should_createAndListRide() {
        var clientId = createClient();

        var ride = Map.of(
                "clientId", clientId,
                "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "Miami Airport",
                "dropoffLocation", "South Beach Hotel",
                "basePrice", 85
        );

        var createResponse = restTemplate.postForEntity("/api/rides", ride, Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).containsKeys("id", "clientName", "status", "createdAt");
        assertThat(createResponse.getBody().get("clientName")).isEqualTo("Ana Torres");
        assertThat(createResponse.getBody().get("status")).isEqualTo("SCHEDULED");

        var listResponse = restTemplate.getForEntity("/api/rides", List.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
    }

    @Test
    void should_getRideById() {
        var clientId = createClient();
        var ride = Map.of(
                "clientId", clientId,
                "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA",
                "dropoffLocation", "FLL"
        );
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var id = created.getBody().get("id").toString();

        var response = restTemplate.getForEntity("/api/rides/" + id, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("pickupLocation")).isEqualTo("MIA");
    }

    @Test
    void should_return404_when_rideNotFound() {
        var response = restTemplate.getForEntity("/api/rides/nonexistent", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void should_listRidesByClient() {
        var clientId = createClient();
        var ride = Map.of(
                "clientId", clientId,
                "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "Downtown",
                "dropoffLocation", "Airport"
        );
        restTemplate.postForEntity("/api/rides", ride, Map.class);

        var response = restTemplate.getForEntity("/api/clients/" + clientId + "/rides", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void should_return400_when_clientNotFound() {
        var ride = Map.of(
                "clientId", "nonexistent",
                "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA",
                "dropoffLocation", "FLL"
        );
        var response = restTemplate.postForEntity("/api/rides", ride, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_listRidesByDriver_sortedByPickupTime() {
        // M3 (#33) — driver queue endpoint
        var clientId = createClient();

        // Two rides, one earlier, one later. Both assigned to the same driver.
        var laterId = createRide(clientId, "2026-08-01T18:00:00Z", "Bayfront", "Wynwood", "drv-q-test");
        var earlierId = createRide(clientId, "2026-08-01T08:00:00Z", "MIA", "Downtown", "drv-q-test");
        createRide(clientId, "2026-08-01T10:00:00Z", "Brickell", "South Beach", "other-driver");

        var response = restTemplate.getForEntity("/api/drivers/drv-q-test/rides", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(((Map) response.getBody().get(0)).get("id")).isEqualTo(earlierId);
        assertThat(((Map) response.getBody().get(1)).get("id")).isEqualTo(laterId);
    }

    @Test
    void should_returnEmptyList_when_driverHasNoRides() {
        var response = restTemplate.getForEntity("/api/drivers/no-such-driver/rides", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    /**
     * Helper that posts a ride and back-fills assignedDriverId via Mongo, since
     * the assignment endpoint is not yet built (Story 4 still pending).
     */
    private String createRide(String clientId, String pickupTime, String from, String to, String driverId) {
        var ride = Map.of(
                "clientId", clientId,
                "pickupDateTime", pickupTime,
                "pickupLocation", from,
                "dropoffLocation", to
        );
        var resp = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = resp.getBody().get("id").toString();
        if (driverId != null) {
            mongoTemplate.updateFirst(
                    org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria.where("_id").is(rideId)),
                    org.springframework.data.mongodb.core.query.Update.update("assignedDriverId", driverId),
                    "rides");
        }
        return rideId;
    }
}
