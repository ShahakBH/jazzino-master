package com.yazino.bi.operations.view;

import jxl.format.CellFormat;
import jxl.write.DateTime;
import jxl.write.WritableSheet;
import jxl.write.WriteException;

/**
 * Class used to create Excel cells containing dates
 */
public final class DateTimeCellCreator implements ExcelCellCreator {

    private static final ExcelCellCreator INSTANCE = new DateTimeCellCreator();

    /**
     * No default constructor
     */
    private DateTimeCellCreator() {
    }

    public static ExcelCellCreator getInstance() {
        return INSTANCE;
    }

    @Override
    public void createCell(final WritableSheet sheet, final int column, final int row, final Object contents,
                           final CellFormat format) throws WriteException {
        final org.joda.time.DateTime date = (org.joda.time.DateTime) contents;
        if (date == null) {
            return;
        }
        sheet.addCell(new DateTime(column, row, date.toDate(), format));
    }

}
