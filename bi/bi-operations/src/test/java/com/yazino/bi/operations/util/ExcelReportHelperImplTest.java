package com.yazino.bi.operations.util;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.junit.Test;

public class ExcelReportHelperImplTest {
    @Test
    public void shouldReturnWorkbookInstance() throws IOException, WriteException {
        // GIVEN an output stream
        final ServletOutputStream os = mock(ServletOutputStream.class);

        // WHEN getting an instance of the helper and using it
        final WritableWorkbook workbook = ExcelReportHelperImpl.getInstance().getWorkbookInstance(os);
        final WritableSheet sheet = workbook.createSheet("Test", 0);
        sheet.addCell(new Label(0, 0, "Test"));

        workbook.write();
        workbook.close();

        // THEN we have an observable action on the stream
        verify(os, atLeastOnce()).write(any(byte[].class));
    }
}
