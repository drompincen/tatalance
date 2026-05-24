package com.tatalance.invoice;

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
class InvoiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection("invoices");
        mongoTemplate.dropCollection("rides");
        mongoTemplate.dropCollection("clients");
        mongoTemplate.dropCollection("drivers");
    }

    private String createCompletedRide() {
        // Create client
        var client = Map.of("firstName", "Ana", "lastName", "Torres", "phone", "+17865551004");
        var clientResp = restTemplate.postForEntity("/api/clients", client, Map.class);
        var clientId = clientResp.getBody().get("id").toString();

        // Create driver
        var driver = Map.of("firstName", "Carlos", "lastName", "Mendez",
                "phone", "+13055551002", "payoutType", "PERCENTAGE", "payoutRate", 70);
        var driverResp = restTemplate.postForEntity("/api/drivers", driver, Map.class);
        var driverId = driverResp.getBody().get("id").toString();

        // Create ride
        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "MIA", "dropoffLocation", "FLL", "basePrice", 100);
        var rideResp = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = rideResp.getBody().get("id").toString();

        // Assign driver
        restTemplate.postForEntity("/api/rides/" + rideId + "/assign",
                Map.of("driverId", driverId), Map.class);

        // Complete ride
        restTemplate.postForEntity("/api/rides/" + rideId + "/complete",
                Map.of("actualStart", "2026-06-01T14:05:00Z", "actualEnd", "2026-06-01T15:10:00Z",
                        "tolls", 5.0, "parking", 10.0), Map.class);

        return rideId;
    }

    @Test
    void should_generateInvoice_fromCompletedRide() {
        var rideId = createCompletedRide();

        var response = restTemplate.postForEntity("/api/invoices",
                Map.of("rideId", rideId), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("invoiceNumber")).asString().startsWith("INV-2026-");
        assertThat(response.getBody().get("clientName")).isEqualTo("Ana Torres");
        assertThat(response.getBody().get("status")).isEqualTo("OUTSTANDING");
        assertThat(((Number) response.getBody().get("baseCharge")).doubleValue()).isEqualTo(100.0);
        assertThat(((Number) response.getBody().get("additionalCharges")).doubleValue()).isEqualTo(15.0);
        // tax = (100 + 15) * 0.08 = 9.20
        assertThat(((Number) response.getBody().get("tax")).doubleValue()).isEqualTo(9.20);
        // total = 115 + 9.20 = 124.20
        assertThat(((Number) response.getBody().get("total")).doubleValue()).isEqualTo(124.20);
    }

    @Test
    void should_listInvoices() {
        var rideId = createCompletedRide();
        restTemplate.postForEntity("/api/invoices", Map.of("rideId", rideId), Map.class);

        var response = restTemplate.getForEntity("/api/invoices", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void should_return400_when_rideNotCompleted() {
        // Create client + ride (SCHEDULED, not completed)
        var client = Map.of("firstName", "Test", "lastName", "User", "phone", "+15551110000");
        var clientResp = restTemplate.postForEntity("/api/clients", client, Map.class);
        var clientId = clientResp.getBody().get("id").toString();

        var ride = Map.of("clientId", clientId, "pickupDateTime", "2026-06-01T14:00:00Z",
                "pickupLocation", "A", "dropoffLocation", "B");
        var rideResp = restTemplate.postForEntity("/api/rides", ride, Map.class);
        var rideId = rideResp.getBody().get("id").toString();

        var response = restTemplate.postForEntity("/api/invoices",
                Map.of("rideId", rideId), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_getInvoiceById() {
        var rideId = createCompletedRide();
        var created = restTemplate.postForEntity("/api/invoices",
                Map.of("rideId", rideId), Map.class);
        var invoiceId = created.getBody().get("id").toString();

        var response = restTemplate.getForEntity("/api/invoices/" + invoiceId, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("clientName")).isEqualTo("Ana Torres");
    }
}
