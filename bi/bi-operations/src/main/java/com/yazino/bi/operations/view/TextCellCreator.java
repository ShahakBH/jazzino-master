package com.yazino.bi.operations.view;

import jxl.format.CellFormat;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * Class used to create Excel cells containing text
 */
public final class TextCellCreator implements ExcelCellCreator {

    private static final ExcelCellCreator INSTANCE = new TextCellCreator();

    /**
     * No default constructor
     */
    private TextCellCreator() {
    }

    public static ExcelCellCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void createCell(final WritableSheet sheet, final int column, final int row, final Object contents,
                           final CellFormat format) throws WriteException {
        if (contents == null) {
            return;
        }
        sheet.addCell(new Label(column, row, contents.toString(), format));
    }

}
