package com.yazino.bi.operations.view;

import static com.yazino.bi.operations.controller.ReportConstants.REPORT_DATA_MODEL;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import org.springframework.web.servlet.view.AbstractUrlBasedView;

import com.yazino.bi.operations.util.ExcelReportHelper;
import com.yazino.bi.operations.util.ReportColumnsExtractorHelper;
import com.yazino.bi.operations.util.ReportFieldInformation;

/**
 * View rendering a report as an Excel workbook
 */
public class ExcelStreamView extends AbstractUrlBasedView {

    @SuppressWarnings("unchecked")
    @Override
    protected void renderMergedOutputModel(@SuppressWarnings("rawtypes") final Map model,
            final HttpServletRequest request,
            final HttpServletResponse response) throws Exception {
        final ExcelReportHelper exportHelper = (ExcelReportHelper) getAttributesMap().get("helper");

        final List<Object> reportData = (List<Object>) model.get(REPORT_DATA_MODEL);

        final String url = getUrl();
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=" + url + ".xls");

        final ServletOutputStream out = response.getOutputStream();
        try {
            final WritableWorkbook workbook = exportHelper.getWorkbookInstance(out);
            final WritableSheet sheet = workbook.createSheet("Report", 0);

            fillReportWorkbook(sheet, reportData);

            workbook.write();
            workbook.close();
        } catch (final Throwable t) {
            t.printStackTrace();
        } finally {
            out.close();
        }
    }

    /**
     * Fills the report workbook with the data provided in the beans list
     *
     * @param sheet      Worksheet to write to
     * @param reportData List of report beans to display
     * @throws WriteException Thrown when there is a write problem in the excel sheet
     */
    private void fillReportWorkbook(final WritableSheet sheet, final List<Object> reportData)
            throws WriteException {
        if (reportData.size() == 0) {
            return;
        }
        final Object firstLine = reportData.get(0);
        final ReportColumnsExtractorHelper reportExtractor =
                ReportColumnsExtractorHelper.forClass(firstLine.getClass());

        displayHeader(sheet, reportExtractor);
        int lineNumber = 1;
        for (final Object reportLine : reportData) {
            displayReportLine(sheet, reportLine, reportExtractor, lineNumber++);
        }
    }

    /**
     * Display an individual line of the report based on the bean's metadata
     *
     * @param sheet           Worksheet to write to
     * @param reportLine      Bean representing one line of the report
     * @param reportExtractor Helper used to extract field values from the bean
     * @param lineNumber      Ordinal number of the line
     * @throws WriteException Thrown when there is a write problem in the excel sheet
     */
    private void displayReportLine(final WritableSheet sheet,
            final Object reportLine,
            final ReportColumnsExtractorHelper reportExtractor,
            final int lineNumber) throws WriteException {
        int pos = 0;
        for (final ReportFieldInformation fieldInfo : reportExtractor.getFields(reportLine).values()) {
            fieldInfo
                    .getFormat()
                    .getCellCreator()
                    .createCell(sheet, pos++, lineNumber, fieldInfo.getFieldValue(),
                            fieldInfo.getFormat().getExcelCellFormat());
        }
    }

    /**
     * Display the report header based on the object's data
     *
     * @param sheet           Worksheet to write to
     * @param reportExtractor Helper used to extract field values from the beans
     * @throws WriteException Thrown when there is a write problem in the excel sheet
     */
    private void displayHeader(final WritableSheet sheet, final ReportColumnsExtractorHelper reportExtractor)
            throws WriteException {
        int pos = 0;
        for (final ReportFieldInformation fieldInfo : reportExtractor.getFieldsList()) {
            if (fieldInfo.getColumnWidth() != -1) {
                sheet.setColumnView(pos, fieldInfo.getColumnWidth());
            }

            TextCellCreator.getInstance().createCell(sheet, pos++, 0, fieldInfo.getFieldHeader(),
                    fieldInfo.getFormat().getExcelHeaderFormat());
        }
    }
}
