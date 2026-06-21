package com.tatalance.ride;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Base repository for unified Job model (Rides + Service/Freelance Jobs).
 * Category A refactor (Issue #93).
 *
 * @Document(collection="jobs") on Job.
 * RideRepository extends for concrete Ride queries (filtered).
 * Direct use for creating/saving base Jobs (SERVICE) for developer jobs.
 */
public interface JobRepository extends MongoRepository<Job, String> {
    // Common finders intentionally not declared here to avoid covariant return clashes with RideRepository.
    // Ride-specific overrides live in RideRepository with proper @Query + type filters.
    // For general job queries (future /jobs tab) add here or use @Query methods returning Job.
}
