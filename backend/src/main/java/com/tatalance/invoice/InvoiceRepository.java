package com.tatalance.invoice;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    long count();
    List<Invoice> findByUserId(String userId);
    Optional<Invoice> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
}
