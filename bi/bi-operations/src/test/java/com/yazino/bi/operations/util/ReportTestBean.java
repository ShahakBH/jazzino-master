package com.yazino.bi.operations.util;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import java.util.ArrayList;
import java.util.List;

import com.yazino.bi.operations.view.ReportField;

/**
 * Bean used to test annotated report info
 */
public class ReportTestBean {
    @ReportField(header = "Header", position = 0, columnWidth = 10)
    private String stringField;

    private String nonAnnotatedField;

    @ReportField(header = "Header", position = 4)
    private String stringField2;

    @SuppressWarnings("unused")
    @ReportField(header = "Header NG", position = 1)
    private String fieldWithNoGetter;

    @ReportField(header = "H", position = 2, format = INTEGER)
    private Integer integerField;

    @ReportField(position = 3, multipleColumnsSource = "headers")
    private List<String> multipleField;

    private static List<String> headers = new ArrayList<String>();

    public static List<String> getHeaders() {
        return headers;
    }

    public static void setHeaders(final List<String> headers) {
        ReportTestBean.headers = headers;
    }

    public String getStringField2() {
        return stringField2;
    }

    public void setStringField2(final String stringField2) {
        this.stringField2 = stringField2;
    }

    public Integer getIntegerField() {
        return integerField;
    }

    public void setIntegerField(final Integer integerField) {
        this.integerField = integerField;
    }

    public String getStringField() {
        return stringField;
    }

    public void setStringField(final String stringField) {
        this.stringField = stringField;
    }

    public String getNonAnnotatedField() {
        return nonAnnotatedField;
    }

    public void setNonAnnotatedField(final String nonAnnotatedField) {
        this.nonAnnotatedField = nonAnnotatedField;
    }

    public List<String> getMultipleField() {
        return multipleField;
    }

    public void setMultipleField(final List<String> multipleField) {
        this.multipleField = multipleField;
    }
}
