package com.yazino.bi.operations.util;

import jxl.Workbook;
import jxl.write.WritableWorkbook;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Implements the excel report helper interface as a static factory
 */
public final class ExcelReportHelperImpl implements ExcelReportHelper {

    private static final ExcelReportHelper INSTANCE = new ExcelReportHelperImpl();

    /**
     * This is a singleton having only one static instance
     */
    private ExcelReportHelperImpl() {
    }

    public static ExcelReportHelper getInstance() {
        return INSTANCE;
    }

    @Override
    public WritableWorkbook getWorkbookInstance(final ServletOutputStream outputStream) throws IOException {
        return Workbook.createWorkbook(outputStream);
    }

}
