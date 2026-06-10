package com.tatalance.client;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends MongoRepository<Client, String> {
    List<Client> findByUserId(String userId);
    Optional<Client> findByIdAndUserId(String id, String userId);
    boolean existsByIdAndUserId(String id, String userId);
    boolean existsByUserIdAndPhone(String userId, String phone);
    boolean existsByUserIdAndPhoneAndIdNot(String userId, String phone, String id);
}
