package com.tatalance.profile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends MongoRepository<Profile, String> {
    List<Profile> findByUserId(String userId);
    Page<Profile> findByUserId(String userId, Pageable pageable);
    Optional<Profile> findByIdAndUserId(String id, String userId);
    boolean existsByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
