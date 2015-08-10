package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.model.DateDetailsLevel;

public class ManagementReportCommand extends CommandWithPlatformFormatAndDates {
    private DateDetailsLevel detailsLevel;

    public DateDetailsLevel getDetailsLevel() {
        return detailsLevel;
    }

    public void setDetailsLevel(final DateDetailsLevel detailsLevel) {
        this.detailsLevel = detailsLevel;
    }
}
