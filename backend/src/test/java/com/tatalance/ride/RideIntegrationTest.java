package com.tatalance.ride;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
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
        mongoTemplate.dropCollection("drivers");
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

    private String createDriver() {
        var driver = Map.of(
                "firstName", "Carlos",
                "lastName", "Mendez",
                "phone", "+13055551002",
                "payoutType", "PERCENTAGE",
                "payoutRate", 70
        );
        var response = restTemplate.postForEntity("/api/drivers", driver, Map.class);
        return response.getBody().get("id").toString();
    }

    @Test
    void should_assignDriverToRide() {
        var clientId = createClient();
        var driverId = createDriver();

        var ride = Map.of(
                "clientId", clientId,
                "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA",
                "dropoffLocation", "FLL"
        );
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        var assignResponse = restTemplate.postForEntity(
                "/api/rides/" + rideId + "/assign",
                Map.of("driverId", driverId), Map.class);
        assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assignResponse.getBody().get("status")).isEqualTo("ASSIGNED");
        assertThat(assignResponse.getBody().get("assignedDriverId")).isEqualTo(driverId);
        assertThat(assignResponse.getBody().get("assignedDriverName")).isEqualTo("Carlos Mendez");

        // Verify driver is now ON_TRIP
        var driverResponse = restTemplate.getForEntity("/api/drivers/" + driverId, Map.class);
        assertThat(driverResponse.getBody().get("availability")).isEqualTo("ON_TRIP");
    }

    @Test
    void should_return400_when_driverNotAvailable() {
        var clientId = createClient();
        var driverId = createDriver();

        // Create and assign first ride to make driver ON_TRIP
        var ride1 = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "A", "dropoffLocation", "B");
        var created1 = restTemplate.postForEntity("/api/rides", ride1, Map.class);
        restTemplate.postForEntity("/api/rides/" + created1.getBody().get("id") + "/assign",
                Map.of("driverId", driverId), Map.class);

        // Try to assign same driver to second ride
        var ride2 = Map.of("clientId", clientId, "pickupDateTime", "2026-06-02T14:00:00Z",
                "pickupLocation", "C", "dropoffLocation", "D");
        var created2 = restTemplate.postForEntity("/api/rides", ride2, Map.class);
        var assignResponse = restTemplate.postForEntity(
                "/api/rides/" + created2.getBody().get("id") + "/assign",
                Map.of("driverId", driverId), Map.class);
        assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_startThenCompleteRide_andCalculateBillable() {
        var clientId = createClient();
        var driverId = createDriver();

        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL", "basePrice", 85);
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        // Assign → Start → Complete (complete requires IN_PROGRESS)
        restTemplate.postForEntity("/api/rides/" + rideId + "/assign",
                Map.of("driverId", driverId), Map.class);
        restTemplate.postForEntity("/api/rides/" + rideId + "/start", null, Map.class);

        var completeBody = Map.of(
                "tolls", 5.50,
                "parking", 10.00
        );
        var completeResponse = restTemplate.postForEntity(
                "/api/rides/" + rideId + "/complete", completeBody, Map.class);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResponse.getBody().get("status")).isEqualTo("COMPLETED");
        // billableAmount = basePrice(85) + tolls(5.50) + parking(10.00) = 100.50
        assertThat(((Number) completeResponse.getBody().get("billableAmount")).doubleValue()).isEqualTo(100.50);
    }

    @Test
    void should_return409_when_completingScheduledRide() {
        var clientId = createClient();
        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL");
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        var response = restTemplate.postForEntity("/api/rides/" + rideId + "/complete", Map.of(), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_reassignDriver_beforeRideStarts() {
        var clientId = createClient();
        var driver1Id = createDriver();

        // Create second driver
        var driver2 = Map.of("firstName", "Mike", "lastName", "Johnson",
                "phone", "+19545551003", "payoutType", "FLAT", "payoutRate", 35);
        var driver2Response = restTemplate.postForEntity("/api/drivers", driver2, Map.class);
        var driver2Id = driver2Response.getBody().get("id").toString();

        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL");
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        // Assign first driver
        restTemplate.postForEntity("/api/rides/" + rideId + "/assign",
                Map.of("driverId", driver1Id), Map.class);

        // Reassign to second driver — the first driver should be freed when reassigned
        var reassignResponse = restTemplate.postForEntity(
                "/api/rides/" + rideId + "/assign",
                Map.of("driverId", driver2Id), Map.class);
        assertThat(reassignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reassignResponse.getBody().get("assignedDriverName")).isEqualTo("Mike Johnson");
    }

    @Test
    void should_updateScheduledRide() {
        var clientId = createClient();
        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL", "basePrice", 85);
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        var update = Map.of("clientId", clientId, "pickupDateTime", "2026-06-02T10:00:00Z",
                "pickupLocation", "Brickell", "dropoffLocation", "Wynwood", "basePrice", 60);
        var response = restTemplate.exchange("/api/rides/" + rideId, HttpMethod.PUT,
                new HttpEntity<>(update), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("pickupLocation")).isEqualTo("Brickell");
        assertThat(response.getBody().get("dropoffLocation")).isEqualTo("Wynwood");
    }

    @Test
    void should_cancelScheduledRide() {
        var clientId = createClient();
        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL");
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        var response = restTemplate.postForEntity("/api/rides/" + rideId + "/cancel", null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("CANCELLED");
    }

    @Test
    void should_cancelAssignedRide_and_freeDriver() {
        var clientId = createClient();
        var driverId = createDriver();

        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL");
        var created = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = created.getBody().get("id").toString();

        restTemplate.postForEntity("/api/rides/" + rideId + "/assign",
                Map.of("driverId", driverId), Map.class);

        var cancelResponse = restTemplate.postForEntity("/api/rides/" + rideId + "/cancel", null, Map.class);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().get("status")).isEqualTo("CANCELLED");

        // Verify driver is back to AVAILABLE (cancel frees the driver)
        var driverResponse = restTemplate.getForEntity("/api/drivers/" + driverId, Map.class);
        assertThat(driverResponse.getBody().get("availability")).isEqualTo("AVAILABLE");
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
     * Helper that posts a ride and back-fills assignedDriverId via Mongo to set up
     * driver-queue scenarios without going through the assignment flow.
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
