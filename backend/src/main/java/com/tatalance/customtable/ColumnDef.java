package com.tatalance.customtable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ColumnDef {
    @NotBlank
    private String name;
    @NotNull
    private ColumnType type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ColumnType getType() { return type; }
    public void setType(ColumnType type) { this.type = type; }
}
