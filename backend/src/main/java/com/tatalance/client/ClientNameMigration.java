package com.tatalance.client;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ClientNameMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientNameMigration.class);

    private final MongoTemplate mongoTemplate;

    public ClientNameMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        var query = new Query(Criteria.where("name").exists(true)
                .and("firstName").exists(false));

        var docs = mongoTemplate.find(query, Document.class, "clients");

        if (docs.isEmpty()) {
            log.info("ClientNameMigration: no documents to migrate");
            return;
        }

        int count = 0;
        for (var doc : docs) {
            String name = doc.getString("name");
            String firstName;
            String lastName;

            int spaceIdx = name != null ? name.indexOf(' ') : -1;
            if (spaceIdx > 0) {
                firstName = name.substring(0, spaceIdx);
                lastName = name.substring(spaceIdx + 1);
            } else {
                firstName = name != null ? name : "";
                lastName = "";
            }

            var updateQuery = new Query(Criteria.where("_id").is(doc.getObjectId("_id")));
            var update = new Update()
                    .set("firstName", firstName)
                    .set("lastName", lastName)
                    .unset("name");
            mongoTemplate.updateFirst(updateQuery, update, "clients");
            count++;
        }

        log.info("ClientNameMigration: migrated {} document(s)", count);
    }
}
