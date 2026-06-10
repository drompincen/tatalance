package com.tatalance.driver;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends MongoRepository<Driver, String> {
    List<Driver> findByAvailability(Availability availability);
    List<Driver> findByUserId(String userId);
    Optional<Driver> findByIdAndUserId(String id, String userId);
    boolean existsByIdAndUserId(String id, String userId);
}
