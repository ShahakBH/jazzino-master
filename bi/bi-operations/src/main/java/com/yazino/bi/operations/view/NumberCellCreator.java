package com.yazino.bi.operations.view;

import jxl.format.CellFormat;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * Class used to create Excel cells containing text
 */
public final class NumberCellCreator implements ExcelCellCreator {

    private static final ExcelCellCreator INSTANCE = new NumberCellCreator();

    /**
     * No default constructor
     */
    private NumberCellCreator() {
    }

    public static ExcelCellCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void createCell(final WritableSheet sheet, final int column, final int row, final Object contents,
                           final CellFormat format) throws WriteException {
        final java.lang.Number number = (java.lang.Number) contents;
        if (number == null) {
            return;
        }
        sheet.addCell(new Number(column, row, number.doubleValue(), format));
    }

}
