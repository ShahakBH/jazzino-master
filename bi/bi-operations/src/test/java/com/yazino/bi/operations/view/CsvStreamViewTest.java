package com.yazino.bi.operations.view;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static com.yazino.bi.operations.controller.ReportConstants.REPORT_DATA_MODEL;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.yazino.bi.operations.util.ReportColumnsExtractorHelper;
import com.yazino.bi.operations.util.ReportTestBean;
import com.yazino.bi.operations.util.WriterFactory;

@RunWith(MockitoJUnitRunner.class)
public class CsvStreamViewTest {
    private CsvStreamView underTest;

    @Mock
    private HttpServletResponse response;

    @Mock
    private PrintWriter sourceWriter;

    @Mock
    private BufferedWriter writer;

    @Mock
    private WriterFactory writerFactory;

    @Before
    public void init() throws IOException {
        final Map<String, Object> atrMap = new HashMap<String, Object>();
        atrMap.put("helper", writerFactory);

        underTest = new CsvStreamView();
        underTest.setAttributesMap(atrMap);
        underTest.setUrl("activityReport");

        given(response.getWriter()).willReturn(sourceWriter);
        given(writerFactory.getWriter(sourceWriter)).willReturn(writer);
    }

    @Test
    public void shouldLoadEmptyModel() throws Exception {
        // GIVEN a data model (empty in our case)
        final Map<String, Object> modelData = createEmptyModel();

        // WHEN asking the view to render the model contents
        underTest.renderMergedOutputModel(modelData, mock(HttpServletRequest.class), response);

        // THEN the response sets the correct MIME type
        verify(response).setContentType("text/csv");
        verify(response).setHeader("Content-Disposition", "attachment; filename=activityReport.csv");

        // AND returns an empty closed stream
        verify(sourceWriter).close();
    }

    private Map<String, Object> createEmptyModel() {
        final Map<String, Object> modelData = new HashMap<String, Object>();
        final List<Object> emptyList = new ArrayList<Object>();
        modelData.put(REPORT_DATA_MODEL, emptyList);
        return modelData;
    }

    @Test
    public void shouldLoadFilledModel() throws Exception {
        // GIVEN a filled data model
        final Map<String, Object> modelData = createFilledModel();
     // Some static data is changed at this moment, so need to reset the extractors list
        ReportColumnsExtractorHelper.reset();

        // WHEN asking the view to render the model contents
        underTest.renderMergedOutputModel(modelData, mock(HttpServletRequest.class), response);

        // THEN the resulting CSV contains the expected data
        verify(writer).write("\"Header\",\"Header NG\",\"H\",\"Header\"\n");
        verify(writer).write("\"str\",\"\",\"2\",\"\"\n");
        verify(writer).flush();
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
}
