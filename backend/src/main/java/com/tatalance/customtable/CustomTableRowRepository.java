package com.tatalance.customtable;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CustomTableRowRepository extends MongoRepository<CustomTableRow, String> {
    List<CustomTableRow> findByTableId(String tableId);
    void deleteByTableId(String tableId);
    Optional<CustomTableRow> findByIdAndUserId(String id, String userId);
}
