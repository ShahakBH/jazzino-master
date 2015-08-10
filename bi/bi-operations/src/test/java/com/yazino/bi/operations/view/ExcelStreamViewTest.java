package com.yazino.bi.operations.view;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static com.yazino.bi.operations.controller.ReportConstants.REPORT_DATA_MODEL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.yazino.bi.operations.util.ExcelReportHelper;
import com.yazino.bi.operations.util.ReportColumnsExtractorHelper;
import com.yazino.bi.operations.util.ReportTestBean;

@SuppressWarnings("unused")
public class ExcelStreamViewTest {

    private Map<String, Object> modelData;
    private ExcelStreamView view;
    private ServletOutputStream outputStream;
    private HttpServletResponse response;
    private WritableWorkbook workbookMock;
    private WritableSheet worksheetMock;

    @Before
    public void createConfiguration() throws IOException {
        final ExcelReportHelper helper = mock(ExcelReportHelper.class);
        final Map<String, Object> atrMap = new HashMap<String, Object>();
        atrMap.put("helper", helper);

        view = new ExcelStreamView();
        view.setAttributesMap(atrMap);
        view.setUrl("activityReport");

        response = mock(HttpServletResponse.class);
        outputStream = mock(ServletOutputStream.class);
        given(response.getOutputStream()).willReturn(outputStream);

        workbookMock = mock(WritableWorkbook.class);
        given(helper.getWorkbookInstance(outputStream)).willReturn(workbookMock);
        worksheetMock = mock(WritableSheet.class);
        given(workbookMock.createSheet(anyString(), anyInt())).willReturn(worksheetMock);
    }

    @Test
    public void shouldReturnExcelStreamFromModel() throws Exception {
        // GIVEN a data model (empty in our case)
        final Map<String, Object> modelData = createEmptyModel();

        // WHEN asking the view to render the model contents
        view.renderMergedOutputModel(modelData, mock(HttpServletRequest.class), response);

        // THEN the response sets the correct MIME type
        verify(response).setContentType("application/vnd.ms-excel");
        verify(response).setHeader("Content-Disposition", "attachment; filename=activityReport.xls");

        // AND returns some Excel data as output stream
        verify(workbookMock).write();
        verify(workbookMock).close();
        verify(outputStream).close();
    }

    private Map<String, Object> createEmptyModel() {
        final Map<String, Object> modelData = new HashMap<String, Object>();
        final List<Object> emptyList = new ArrayList<Object>();
        modelData.put(REPORT_DATA_MODEL, emptyList);
        return modelData;
    }

    private Map<String, Object> createFilledModel() {
        final List<String> additionalFields = new ArrayList<String>();
        ReportTestBean.setHeaders(additionalFields);

        final Map<String, Object> modelData = new HashMap<String, Object>();
        final List<Object> list = new ArrayList<Object>();

        final ReportTestBean bean1 = new ReportTestBean();
        bean1.setStringField("str");
        bean1.setIntegerField(2);
        list.add(bean1);

        modelData.put(REPORT_DATA_MODEL, list);
        return modelData;
    }

    @Test
    public void shouldFillSheetWithTestData() throws Exception {
        // GIVEN a filled data model
        final Map<String, Object> modelData = createFilledModel();
        // Some static data is changed at this moment, so need to reset the extractors list
        ReportColumnsExtractorHelper.reset();

        // WHEN rendering the view
        view.renderMergedOutputModel(modelData, mock(HttpServletRequest.class), response);

        // THEN the data is correctly written to the data sheet
        verify(worksheetMock).addCell(argThat(cellMatches(0, 0, "Header")));
        verify(worksheetMock).addCell(argThat(cellMatches(1, 0, "Header NG")));
        verify(worksheetMock).addCell(argThat(cellMatches(2, 0, "H")));
        verify(worksheetMock).addCell(argThat(cellMatches(3, 0, "Header")));
        verify(worksheetMock).addCell(argThat(cellMatches(0, 1, "str")));
        verify(worksheetMock).addCell(argThat(cellMatches(2, 1, "2")));
        verify(worksheetMock).setColumnView(0, 10);
    }

    private ArgumentMatcher<WritableCell> cellMatches(final int x, final int y, final String contents) {
        return new ArgumentMatcher<WritableCell>() {
            @Override
            public boolean matches(final Object argument) {
                final WritableCell arg = (WritableCell) argument;
                System.out.println("Arg: " + arg.getColumn() + "x" + arg.getRow() + ": " + arg.getContents());
                if (arg.getColumn() == x && arg.getRow() == y && contents.equals(arg.getContents())) {
                    return true;
                }
                return false;
            }
        };
    }

    @Test
    public void shouldCloseOutputStreamOnError() throws Exception {
        // GIVEN a view with an output stream that fails on write tentative
        final Map<String, Object> modelData = createFilledModel();
        doThrow(new IOException()).when(workbookMock).write();

        // WHEN rendering the view
        view.renderMergedOutputModel(modelData, mock(HttpServletRequest.class), response);

        // THEN at least the close method on the output stream is called
        verify(outputStream).close();
    }
}