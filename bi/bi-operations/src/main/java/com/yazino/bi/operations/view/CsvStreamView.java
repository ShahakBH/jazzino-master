package com.yazino.bi.operations.view;

import static com.yazino.bi.operations.controller.ReportConstants.REPORT_DATA_MODEL;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractUrlBasedView;

import com.yazino.bi.operations.util.ReportColumnsExtractorHelper;
import com.yazino.bi.operations.util.ReportFieldInformation;
import com.yazino.bi.operations.util.WriterFactory;

public class CsvStreamView extends AbstractUrlBasedView {

    private WriterFactory writerFactory;

    @Override
    protected void renderMergedOutputModel(final Map<String, Object> model,
            final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        writerFactory = (WriterFactory) getAttributesMap().get("helper");

        @SuppressWarnings("unchecked")
        final List<Object> reportData = (List<Object>) model.get(REPORT_DATA_MODEL);

        final String url = getUrl();
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=" + url + ".csv");

        final PrintWriter out = response.getWriter();
        try {
            fillReportCsv(out, reportData);
        } catch (final Throwable t) {
            t.printStackTrace();
        } finally {
            out.close();
        }
    }

    private void fillReportCsv(final PrintWriter out, final List<Object> reportData) throws IOException {
        if (reportData.size() == 0) {
            return;
        }
        final BufferedWriter writer = writerFactory.getWriter(out);

        final Object firstLine = reportData.get(0);
        final ReportColumnsExtractorHelper reportExtractor =
                ReportColumnsExtractorHelper.forClass(firstLine.getClass());

        boolean isFirst = true;
        StringBuilder cell = new StringBuilder();
        for (final ReportFieldInformation fieldInfo : reportExtractor.getFieldsList()) {
            if (isFirst) {
                isFirst = false;
            } else {
                cell.append(",");
            }
            cell.append(quotedString(fieldInfo.getFieldHeader()));
        }
        writer.write(cell.append("\n").toString());

        for (final Object reportLine : reportData) {
            cell = new StringBuilder();
            isFirst = true;
            for (final ReportFieldInformation fieldInfo : reportExtractor.getFields(reportLine).values()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    cell.append(",");
                }
                cell.append(quotedString(fieldInfo.getFieldValue()));
            }
            writer.write(cell.append("\n").toString());
        }

        writer.flush();
    }

    private String quotedString(final Object obj) {
        final StringBuilder str = new StringBuilder("\"");
        if (obj != null) {
            str.append(obj.toString());
        }
        str.append("\"");
        return str.toString();
    }

}
