package com.yazino.bi.operations.controller;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.yazino.bi.operations.util.DataFormatHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The name is self-explaining...
 */
public abstract class ControllerWithDateBinderAndFormatterAndReportFormat  {
    /**
     * Fills the possible months range selection
     *
     * @return Map containing the selection
     */
    @ModelAttribute("reportFormats")
    public Map<String, String> getReportFormats() {
        final Map<String, String> formats = new LinkedHashMap<String, String>();
        formats.put("do", "Web Page");
        formats.put("xls", "Excel Grid");
        formats.put("csv", "CSV File");

        return formats;
    }

    @ModelAttribute("formatter")
    public DataFormatHelper getFormatter() {
        return DataFormatHelper.getInstance();
    }

    /**
     * Specify how the dates are treated
     *
     * @param binder Binder to get up
     */
    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
