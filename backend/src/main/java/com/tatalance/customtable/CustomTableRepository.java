package com.tatalance.customtable;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomTableRepository extends MongoRepository<CustomTable, String> {
}
