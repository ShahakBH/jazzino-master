package com.yazino.bi.operations.util;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import jxl.write.WritableWorkbook;

/**
 * Holds the helper methods for the Excel report generator
 */
public interface ExcelReportHelper {
    /**
     * Creates an Excel workbook capable to write data to the appropriate stream
     *
     * @param outputStream Output stream to write to
     * @return A streamable Excel workbook
     * @throws IOException Thrown when the worksheet creation on the stream is impossible
     */
    WritableWorkbook getWorkbookInstance(ServletOutputStream outputStream) throws IOException;
}
