package com.tatalance.customtable;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CustomTableRowRepository extends MongoRepository<CustomTableRow, String> {
    List<CustomTableRow> findByTableId(String tableId);
    void deleteByTableId(String tableId);
}
