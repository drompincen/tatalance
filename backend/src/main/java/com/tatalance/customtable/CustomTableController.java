package com.tatalance.customtable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "Custom Tables", description = "User-defined tables")
@RestController
@RequestMapping("/api/tables")
public class CustomTableController {

    private final CustomTableRepository tableRepository;
    private final CustomTableRowRepository rowRepository;

    public CustomTableController(CustomTableRepository tableRepository, CustomTableRowRepository rowRepository) {
        this.tableRepository = tableRepository;
        this.rowRepository = rowRepository;
    }

    @Operation(summary = "Create a custom table")
    @ApiResponse(responseCode = "201", description = "Table created")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomTable createTable(@Valid @RequestBody CustomTable table) {
        table.setCreatedAt(Instant.now());
        return tableRepository.save(table);
    }

    @Operation(summary = "List all custom tables")
    @GetMapping
    public List<CustomTable> listTables() {
        return tableRepository.findAll();
    }

    @Operation(summary = "Get a custom table by id")
    @ApiResponse(responseCode = "404", description = "Table not found")
    @GetMapping("/{id}")
    public CustomTable getTable(@PathVariable String id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    }

    @Operation(summary = "Rename a custom table")
    @ApiResponse(responseCode = "200", description = "Table renamed")
    @ApiResponse(responseCode = "404", description = "Table not found")
    @PutMapping("/{id}")
    public CustomTable renameTable(@PathVariable String id, @RequestBody Map<String, String> body) {
        CustomTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        table.setName(name);
        return tableRepository.save(table);
    }

    @Operation(summary = "Delete a custom table and all its rows")
    @ApiResponse(responseCode = "204", description = "Table deleted")
    @ApiResponse(responseCode = "404", description = "Table not found")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTable(@PathVariable String id) {
        if (!tableRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found");
        }
        rowRepository.deleteByTableId(id);
        tableRepository.deleteById(id);
    }

    @Operation(summary = "Add a column to a custom table")
    @ApiResponse(responseCode = "200", description = "Column added")
    @ApiResponse(responseCode = "404", description = "Table not found")
    @ApiResponse(responseCode = "400", description = "Invalid column or duplicate name")
    @PostMapping("/{id}/columns")
    public CustomTable addColumn(@PathVariable String id, @Valid @RequestBody ColumnDef column) {
        validateLabels(column);
        CustomTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        if (column.getType() == ColumnType.LINK) {
            if (column.getLinkedTableId() == null || column.getLinkedTableId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "linkedTableId is required for LINK columns");
            }
            if (column.getLinkedTableId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot link a table to itself");
            }
            if (!tableRepository.existsById(column.getLinkedTableId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked table not found");
            }
        }
        boolean duplicate = table.getColumns().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(column.getName()));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Column name already exists");
        }
        List<ColumnDef> cols = new ArrayList<>(table.getColumns());
        cols.add(column);
        table.setColumns(cols);
        return tableRepository.save(table);
    }

    private void validateLabels(ColumnDef column) {
        if (column.getTrueLabel() != null && column.getTrueLabel().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "trueLabel must not be blank");
        }
        if (column.getFalseLabel() != null && column.getFalseLabel().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "falseLabel must not be blank");
        }
    }

    @Operation(summary = "Update a column (rename or change labels)")
    @ApiResponse(responseCode = "200", description = "Column updated")
    @ApiResponse(responseCode = "404", description = "Table or column not found")
    @PutMapping("/{id}/columns/{columnName}")
    public CustomTable updateColumn(@PathVariable String id, @PathVariable String columnName,
                                    @RequestBody Map<String, String> body) {
        CustomTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        ColumnDef col = table.getColumns().stream()
                .filter(c -> c.getName().equals(columnName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Column not found"));

        String newName = body.get("name");
        if (newName != null && !newName.isBlank() && !newName.equals(columnName)) {
            boolean duplicate = table.getColumns().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(newName) && !c.getName().equals(columnName));
            if (duplicate) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Column name already exists");
            }
            col.setName(newName);
        }
        if (body.containsKey("trueLabel")) col.setTrueLabel(body.get("trueLabel"));
        if (body.containsKey("falseLabel")) col.setFalseLabel(body.get("falseLabel"));
        return tableRepository.save(table);
    }

    @Operation(summary = "Delete a column from a custom table")
    @ApiResponse(responseCode = "200", description = "Column deleted")
    @ApiResponse(responseCode = "404", description = "Table or column not found")
    @ApiResponse(responseCode = "400", description = "Cannot delete last column")
    @DeleteMapping("/{id}/columns/{columnName}")
    public CustomTable deleteColumn(@PathVariable String id, @PathVariable String columnName) {
        CustomTable table = tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        if (table.getColumns().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last column");
        }
        List<ColumnDef> cols = new ArrayList<>(table.getColumns());
        boolean removed = cols.removeIf(c -> c.getName().equals(columnName));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Column not found");
        }
        table.setColumns(cols);
        return tableRepository.save(table);
    }

    @Operation(summary = "Add a row to a custom table")
    @ApiResponse(responseCode = "201", description = "Row added")
    @ApiResponse(responseCode = "404", description = "Table not found")
    @PostMapping("/{id}/rows")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomTableRow addRow(@PathVariable String id, @RequestBody Map<String, Object> data) {
        if (!tableRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found");
        }
        CustomTableRow row = new CustomTableRow();
        row.setTableId(id);
        row.setData(data);
        row.setCreatedAt(Instant.now());
        return rowRepository.save(row);
    }

    @Operation(summary = "List rows in a custom table")
    @ApiResponse(responseCode = "200", description = "Rows listed")
    @GetMapping("/{id}/rows")
    public List<CustomTableRow> listRows(@PathVariable String id) {
        return rowRepository.findByTableId(id);
    }

    @Operation(summary = "Update a row")
    @ApiResponse(responseCode = "200", description = "Row updated")
    @ApiResponse(responseCode = "404", description = "Row not found")
    @PutMapping("/{id}/rows/{rowId}")
    public CustomTableRow updateRow(@PathVariable String id, @PathVariable String rowId,
                                    @RequestBody Map<String, Object> data) {
        CustomTableRow row = rowRepository.findById(rowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found"));
        if (!row.getTableId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found in this table");
        }
        row.setData(data);
        return rowRepository.save(row);
    }

    @Operation(summary = "Delete a row")
    @ApiResponse(responseCode = "204", description = "Row deleted")
    @ApiResponse(responseCode = "404", description = "Row not found")
    @DeleteMapping("/{id}/rows/{rowId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRow(@PathVariable String id, @PathVariable String rowId) {
        CustomTableRow row = rowRepository.findById(rowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found"));
        if (!row.getTableId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Row not found in this table");
        }
        rowRepository.deleteById(rowId);
    }
}
