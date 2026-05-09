package com.tatalance.client;

import org.bson.Document;
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
class ClientIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        mongoTemplate.dropCollection("clients");
    }

    @Test
    void should_createAndListClient_when_validFirstNameLastName() {
        var request = Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "phone", "+1234"
        );

        var createResponse = restTemplate.postForEntity("/api/clients", request, Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).containsKeys("id", "firstName", "lastName", "createdAt");
        assertThat(createResponse.getBody().get("firstName")).isEqualTo("John");
        assertThat(createResponse.getBody().get("lastName")).isEqualTo("Doe");

        var listResponse = restTemplate.getForEntity("/api/clients", List.class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
    }

    @Test
    void should_return400_when_firstNameMissing() {
        var request = Map.of("lastName", "Doe", "phone", "+1234");
        var response = restTemplate.postForEntity("/api/clients", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return400_when_lastNameMissing() {
        var request = Map.of("firstName", "John", "phone", "+1234");
        var response = restTemplate.postForEntity("/api/clients", request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_migrateOldNameField_when_documentHasNameOnly() {
        // Insert a document with the old "name" field directly
        var oldDoc = new Document()
                .append("name", "Luciano Perez")
                .append("phone", "+5555");
        mongoTemplate.insert(oldDoc, "clients");

        // Run the migration manually (it already ran on startup, but the collection was cleaned)
        var migration = new ClientNameMigration(mongoTemplate);
        migration.run();

        // Verify the document was migrated
        var docs = mongoTemplate.findAll(Document.class, "clients");
        assertThat(docs).hasSize(1);

        var migrated = docs.get(0);
        assertThat(migrated.getString("firstName")).isEqualTo("Luciano");
        assertThat(migrated.getString("lastName")).isEqualTo("Perez");
        assertThat(migrated.containsKey("name")).isFalse();
    }

    @Test
    void should_migrateSingleWordName_when_noSpace() {
        var oldDoc = new Document()
                .append("name", "Madonna")
                .append("phone", "+1111");
        mongoTemplate.insert(oldDoc, "clients");

        new ClientNameMigration(mongoTemplate).run();

        var docs = mongoTemplate.findAll(Document.class, "clients");
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getString("firstName")).isEqualTo("Madonna");
        assertThat(docs.get(0).getString("lastName")).isEmpty();
    }
}
