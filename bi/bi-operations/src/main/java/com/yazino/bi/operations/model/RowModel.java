package com.yazino.bi.operations.model;

import java.util.ArrayList;
import java.util.List;

public class RowModel {

    private TableModel table;
    private List<CellModel> cells;

    public RowModel(final TableModel table) {
        this.table = table;
        this.cells = new ArrayList<CellModel>();
    }

    public List<CellModel> getCells() {
        return cells;
    }

    public void addCell(final CellModel cell) {
        this.cells.add(cell);

    }
}
