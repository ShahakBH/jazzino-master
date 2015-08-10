package com.yazino.bi.operations.model;

import java.util.ArrayList;
import java.util.List;

public class TableModel {

    private List<ColumnModel> columns;
    private List<RowModel> rows;

    public TableModel() {
        this.columns = new ArrayList<ColumnModel>();
        this.rows = new ArrayList<RowModel>();
    }

    public List<ColumnModel> getColumns() {
        return columns;
    }

    public TableModel addColumn(final ColumnModel column) {
        this.columns.add(column);
        return this;
    }

    public void addRow(final Object... cellData) {
        final RowModel row = new RowModel(this);
        for (int i = 0; i < cellData.length; i++) {
            row.addCell(new CellModel(cellData[i], columns.get(i).getFormatter()));
        }
        rows.add(row);
    }

    public List<RowModel> getTableRows() {
        return rows;
    }

    public boolean containsRows() {
        return !rows.isEmpty();
    }
}
