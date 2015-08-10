package com.yazino.bi.operations.view;

import jxl.format.CellFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * Creates a cell in the Excel worksheet
 */
public interface ExcelCellCreator {
    /**
     * Creates a cell in a given position
     *
     * @param sheet    Excel sheet to create the cell in
     * @param column   Zero-based column number
     * @param row      Zero-based row number
     * @param contents Cell's contents
     * @param format   Cell's format
     * @throws WriteException Throws when it's not possible to write to the cell
     */
    void createCell(WritableSheet sheet, int column, int row, Object contents, CellFormat format) throws WriteException;

    CellFormat DEFAULT_FORMAT = new WritableCellFormat();
}
