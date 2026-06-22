package com.tatalance.customtable;

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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomTableIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        this.restTemplate = restTemplate.withBasicAuth("admin", "admin");
        try {
            mongoTemplate.dropCollection("custom_tables");
            mongoTemplate.dropCollection("custom_table_rows");
        } catch (Exception e) {
            assumeTrue(false, "Skipping integration test - Mongo not available: " + e.getMessage());
        }
    }

    private String createTable() {
        var table = Map.of(
                "name", "Contacts",
                "columns", List.of(
                        Map.of("name", "Name", "type", "STRING"),
                        Map.of("name", "Age", "type", "INT"),
                        Map.of("name", "Active", "type", "BOOLEAN")
                )
        );
        var response = restTemplate.postForEntity("/api/tables", table, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("id");
    }

    @Test
    void should_createAndListTable() {
        var tableId = createTable();
        assertThat(tableId).isNotNull();

        var list = restTemplate.getForEntity("/api/tables", Map.class);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) list.getBody().get("content")).hasSize(1);
    }

    @Test
    void should_renameTable() {
        var tableId = createTable();

        var response = restTemplate.exchange("/api/tables/" + tableId, HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "My Contacts")), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("name")).isEqualTo("My Contacts");
    }

    @Test
    void should_addAndListRows() {
        var tableId = createTable();

        var row = Map.of("Name", "John", "Age", 30, "Active", true);
        var addResponse = restTemplate.postForEntity("/api/tables/" + tableId + "/rows", row, Map.class);
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var listResponse = restTemplate.getForEntity("/api/tables/" + tableId + "/rows", Map.class);
        assertThat((List<?>) listResponse.getBody().get("content")).hasSize(1);
    }

    @Test
    void should_updateRow() {
        var tableId = createTable();
        var row = Map.of("Name", "John", "Age", 30);
        var created = restTemplate.postForEntity("/api/tables/" + tableId + "/rows", row, Map.class);
        var rowId = (String) created.getBody().get("id");

        var update = Map.of("Name", "Jane", "Age", 25);
        var response = restTemplate.exchange("/api/tables/" + tableId + "/rows/" + rowId,
                HttpMethod.PUT, new HttpEntity<>(update), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map) response.getBody().get("data")).get("Name")).isEqualTo("Jane");
    }

    @Test
    void should_deleteRow() {
        var tableId = createTable();
        var row = Map.of("Name", "John");
        var created = restTemplate.postForEntity("/api/tables/" + tableId + "/rows", row, Map.class);
        var rowId = (String) created.getBody().get("id");

        var response = restTemplate.exchange("/api/tables/" + tableId + "/rows/" + rowId,
                HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var list = restTemplate.getForEntity("/api/tables/" + tableId + "/rows", Map.class);
        assertThat((List<?>) list.getBody().get("content")).isEmpty();
    }

    @Test
    void should_deleteTableAndRows() {
        var tableId = createTable();
        restTemplate.postForEntity("/api/tables/" + tableId + "/rows",
                Map.of("Name", "John"), Map.class);

        var response = restTemplate.exchange("/api/tables/" + tableId,
                HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var tables = restTemplate.getForEntity("/api/tables", Map.class);
        assertThat((List<?>) tables.getBody().get("content")).isEmpty();
    }

    @Test
    void should_addColumn() {
        var tableId = createTable();

        var col = Map.of("name", "Email", "type", "STRING");
        var response = restTemplate.postForEntity("/api/tables/" + tableId + "/columns", col, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List) response.getBody().get("columns")).hasSize(4);
    }

    @Test
    void should_renameColumn() {
        var tableId = createTable();

        var update = Map.of("name", "Full Name");
        var response = restTemplate.exchange("/api/tables/" + tableId + "/columns/Name",
                HttpMethod.PUT, new HttpEntity<>(update), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cols = (List<Map>) response.getBody().get("columns");
        assertThat(cols.get(0).get("name")).isEqualTo("Full Name");
    }

    @Test
    void should_deleteColumn() {
        var tableId = createTable();

        var response = restTemplate.exchange("/api/tables/" + tableId + "/columns/Age",
                HttpMethod.DELETE, null, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List) response.getBody().get("columns")).hasSize(2);
    }

    @Test
    void should_addColumnWithLabels() {
        var tableId = createTable();

        var col = Map.of("name", "Paid", "type", "BOOLEAN", "trueLabel", "Paid", "falseLabel", "Unpaid");
        var response = restTemplate.postForEntity("/api/tables/" + tableId + "/columns", col, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cols = (List<Map>) response.getBody().get("columns");
        var lastCol = cols.get(cols.size() - 1);
        assertThat(lastCol.get("trueLabel")).isEqualTo("Paid");
        assertThat(lastCol.get("falseLabel")).isEqualTo("Unpaid");
    }

    @Test
    void should_addLinkColumn() {
        var tableId = createTable();
        // Create a second table to link to
        var table2 = Map.of("name", "Vehicles",
                "columns", List.of(Map.of("name", "Make", "type", "STRING")));
        var t2Response = restTemplate.postForEntity("/api/tables", table2, Map.class);
        var table2Id = (String) t2Response.getBody().get("id");

        var col = Map.of("name", "Vehicle", "type", "LINK", "linkedTableId", table2Id);
        var response = restTemplate.postForEntity("/api/tables/" + tableId + "/columns", col, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var cols = (List<Map>) response.getBody().get("columns");
        var lastCol = cols.get(cols.size() - 1);
        assertThat(lastCol.get("type")).isEqualTo("LINK");
        assertThat(lastCol.get("linkedTableId")).isEqualTo(table2Id);
    }
}
