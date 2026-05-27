package com.tatalance.customtable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "custom_tables")
public class CustomTable {
    @Id
    private String id;
    @NotBlank
    private String name;
    @NotEmpty
    @Valid
    private List<ColumnDef> columns;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<ColumnDef> getColumns() { return columns; }
    public void setColumns(List<ColumnDef> columns) { this.columns = columns; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
