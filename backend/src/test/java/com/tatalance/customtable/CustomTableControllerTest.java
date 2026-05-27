package com.tatalance.customtable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomTableController.class)
class CustomTableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomTableRepository tableRepository;

    @MockBean
    private CustomTableRowRepository rowRepository;

    private CustomTable sampleTable() {
        var table = new CustomTable();
        table.setId("tbl001");
        table.setName("Contacts");
        var col1 = new ColumnDef();
        col1.setName("Name");
        col1.setType(ColumnType.STRING);
        var col2 = new ColumnDef();
        col2.setName("Age");
        col2.setType(ColumnType.INT);
        table.setColumns(List.of(col1, col2));
        table.setCreatedAt(Instant.now());
        return table;
    }

    @Test
    void should_createTable() throws Exception {
        when(tableRepository.save(any(CustomTable.class))).thenReturn(sampleTable());

        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Contacts","columns":[{"name":"Name","type":"STRING"},{"name":"Age","type":"INT"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Contacts"))
                .andExpect(jsonPath("$.columns", hasSize(2)));
    }

    @Test
    void should_return400_when_nameBlank() throws Exception {
        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","columns":[{"name":"X","type":"STRING"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_columnsEmpty() throws Exception {
        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","columns":[]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_listTables() throws Exception {
        when(tableRepository.findAll()).thenReturn(List.of(sampleTable()));

        mockMvc.perform(get("/api/tables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Contacts"));
    }

    @Test
    void should_getTableById() throws Exception {
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(sampleTable()));

        mockMvc.perform(get("/api/tables/tbl001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Contacts"));
    }

    @Test
    void should_return404_when_tableNotFound() throws Exception {
        when(tableRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tables/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_renameTable() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/tables/tbl001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Contacts"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Contacts"));
    }

    @Test
    void should_deleteTable() throws Exception {
        when(tableRepository.existsById("tbl001")).thenReturn(true);

        mockMvc.perform(delete("/api/tables/tbl001"))
                .andExpect(status().isNoContent());
        verify(rowRepository).deleteByTableId("tbl001");
        verify(tableRepository).deleteById("tbl001");
    }

    @Test
    void should_return404_when_deletingNonexistentTable() throws Exception {
        when(tableRepository.existsById("unknown")).thenReturn(false);

        mockMvc.perform(delete("/api/tables/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_addRow() throws Exception {
        when(tableRepository.existsById("tbl001")).thenReturn(true);
        when(rowRepository.save(any(CustomTableRow.class))).thenAnswer(inv -> {
            CustomTableRow r = inv.getArgument(0);
            r.setId("row001");
            return r;
        });

        mockMvc.perform(post("/api/tables/tbl001/rows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"Name":"John","Age":30}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableId").value("tbl001"))
                .andExpect(jsonPath("$.data.Name").value("John"))
                .andExpect(jsonPath("$.data.Age").value(30));
    }

    @Test
    void should_return404_when_addingRowToNonexistentTable() throws Exception {
        when(tableRepository.existsById("unknown")).thenReturn(false);

        mockMvc.perform(post("/api/tables/unknown/rows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"Name":"John"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_listRows() throws Exception {
        var row = new CustomTableRow();
        row.setId("row001");
        row.setTableId("tbl001");
        row.setData(Map.of("Name", "John", "Age", 30));
        when(rowRepository.findByTableId("tbl001")).thenReturn(List.of(row));

        mockMvc.perform(get("/api/tables/tbl001/rows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].data.Name").value("John"));
    }

    @Test
    void should_updateRow() throws Exception {
        var row = new CustomTableRow();
        row.setId("row001");
        row.setTableId("tbl001");
        row.setData(Map.of("Name", "John"));
        when(rowRepository.findById("row001")).thenReturn(Optional.of(row));
        when(rowRepository.save(any(CustomTableRow.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/tables/tbl001/rows/row001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"Name":"Jane","Age":25}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.Name").value("Jane"))
                .andExpect(jsonPath("$.data.Age").value(25));
    }

    @Test
    void should_deleteRow() throws Exception {
        var row = new CustomTableRow();
        row.setId("row001");
        row.setTableId("tbl001");
        when(rowRepository.findById("row001")).thenReturn(Optional.of(row));

        mockMvc.perform(delete("/api/tables/tbl001/rows/row001"))
                .andExpect(status().isNoContent());
        verify(rowRepository).deleteById("row001");
    }

    @Test
    void should_return404_when_rowNotInTable() throws Exception {
        var row = new CustomTableRow();
        row.setId("row001");
        row.setTableId("other-table");
        when(rowRepository.findById("row001")).thenReturn(Optional.of(row));

        mockMvc.perform(delete("/api/tables/tbl001/rows/row001"))
                .andExpect(status().isNotFound());
    }
}
