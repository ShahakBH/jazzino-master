package com.yazino.bi.operations.view;

import java.util.List;

import com.yazino.bi.operations.view.reportbeans.AdTrackingReportBean;

public class AdTrackingReportResultsHolder {
    private String errorMessage = "";

    private List<AdTrackingReportBean> reportBeans;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<AdTrackingReportBean> getReportBeans() {
        return reportBeans;
    }

    public void setReportBeans(final List<AdTrackingReportBean> reportBeans) {
        this.reportBeans = reportBeans;
    }
}
