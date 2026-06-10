package com.tatalance.user;

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
@Order(2) // runs after UserSeeder (default order)
public class UserIdMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserIdMigration.class);

    private final AppUserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public UserIdMigration(AppUserRepository userRepository, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        AppUser admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            log.warn("No admin user found — skipping userId migration");
            return;
        }

        String adminId = admin.getId();
        String[] collections = {"clients", "drivers", "rides", "invoices", "custom_tables", "custom_table_rows"};

        for (String collection : collections) {
            Query query = new Query(Criteria.where("userId").is(null));
            Update update = new Update().set("userId", adminId);
            var result = mongoTemplate.updateMulti(query, update, collection);
            if (result.getModifiedCount() > 0) {
                log.info("Migrated {} documents in '{}' to userId={}", result.getModifiedCount(), collection, adminId);
            }
        }
    }
}
