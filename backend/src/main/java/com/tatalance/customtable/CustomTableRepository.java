package com.tatalance.customtable;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomTableRepository extends MongoRepository<CustomTable, String> {
    List<CustomTable> findByUserId(String userId);
    Optional<CustomTable> findByIdAndUserId(String id, String userId);
    boolean existsByIdAndUserId(String id, String userId);
}
