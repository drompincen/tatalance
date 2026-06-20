package com.tatalance.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    long count();
    List<Invoice> findByUserId(String userId);
    Page<Invoice> findByUserId(String userId, Pageable pageable);
    Optional<Invoice> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);
    List<Invoice> findByUserIdAndStatus(String userId, InvoiceStatus status);
}
