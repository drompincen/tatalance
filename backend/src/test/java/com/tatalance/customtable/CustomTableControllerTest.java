package com.tatalance.customtable;

import com.tatalance.SecurityConfig;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomTableController.class)
@Import(SecurityConfig.class)
@WithMockUser
class CustomTableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomTableRepository tableRepository;

    @MockBean
    private CustomTableRowRepository rowRepository;

    @MockBean
    private UserDetailsService userDetailsService;

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
        table.setColumns(new ArrayList<>(List.of(col1, col2)));
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

    // ── Column CRUD tests ──

    @Test
    void should_addColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Email","type":"STRING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(3)))
                .andExpect(jsonPath("$.columns[2].name").value("Email"));
    }

    @Test
    void should_return400_when_duplicateColumnName() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Name","type":"STRING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_addingColumnToNonexistentTable() throws Exception {
        when(tableRepository.findById("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tables/unknown/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","type":"STRING"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_renameColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/tables/tbl001/columns/Name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Full Name"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].name").value("Full Name"));
    }

    @Test
    void should_updateColumnLabels() throws Exception {
        var table = sampleTable();
        // Add a boolean column
        var boolCol = new ColumnDef();
        boolCol.setName("Active");
        boolCol.setType(ColumnType.BOOLEAN);
        table.getColumns().add(boolCol);
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/tables/tbl001/columns/Active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trueLabel":"Paid","falseLabel":"Unpaid"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[2].trueLabel").value("Paid"))
                .andExpect(jsonPath("$.columns[2].falseLabel").value("Unpaid"));
    }

    @Test
    void should_return404_when_updatingNonexistentColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(put("/api/tables/tbl001/columns/Missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_deleteColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(delete("/api/tables/tbl001/columns/Age"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(1)))
                .andExpect(jsonPath("$.columns[0].name").value("Name"));
    }

    @Test
    void should_return400_when_deletingLastColumn() throws Exception {
        var table = sampleTable();
        table.setColumns(new ArrayList<>(List.of(table.getColumns().get(0))));
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(delete("/api/tables/tbl001/columns/Name"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return404_when_deletingNonexistentColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(delete("/api/tables/tbl001/columns/Missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return400_when_blankTrueLabel() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Active","type":"BOOLEAN","trueLabel":"  ","falseLabel":"No"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_addLinkColumn() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.existsById("tbl002")).thenReturn(true);
        when(tableRepository.save(any(CustomTable.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vehicle","type":"LINK","linkedTableId":"tbl002"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns", hasSize(3)))
                .andExpect(jsonPath("$.columns[2].type").value("LINK"))
                .andExpect(jsonPath("$.columns[2].linkedTableId").value("tbl002"));
    }

    @Test
    void should_return400_when_linkMissingTableId() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Vehicle","type":"LINK"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_linkToSelf() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Self","type":"LINK","linkedTableId":"tbl001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_linkToNonexistentTable() throws Exception {
        var table = sampleTable();
        when(tableRepository.findById("tbl001")).thenReturn(Optional.of(table));
        when(tableRepository.existsById("missing")).thenReturn(false);

        mockMvc.perform(post("/api/tables/tbl001/columns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ref","type":"LINK","linkedTableId":"missing"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
