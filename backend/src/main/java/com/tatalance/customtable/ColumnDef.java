package com.tatalance.customtable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ColumnDef {
    @NotBlank
    private String name;
    @NotNull
    private ColumnType type;
    private String trueLabel;
    private String falseLabel;
    private String linkedTableId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ColumnType getType() { return type; }
    public void setType(ColumnType type) { this.type = type; }
    public String getTrueLabel() { return trueLabel; }
    public void setTrueLabel(String trueLabel) { this.trueLabel = trueLabel; }
    public String getFalseLabel() { return falseLabel; }
    public void setFalseLabel(String falseLabel) { this.falseLabel = falseLabel; }
    public String getLinkedTableId() { return linkedTableId; }
    public void setLinkedTableId(String linkedTableId) { this.linkedTableId = linkedTableId; }
}
