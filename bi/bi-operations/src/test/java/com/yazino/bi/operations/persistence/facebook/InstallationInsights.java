package com.yazino.bi.operations.persistence.facebook;

import com.restfb.Facebook;

public class InstallationInsights {
    @Facebook("end_time")
    private String endTime;

    @Facebook("value")
    private Long value;

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(final String endTime) {
        this.endTime = endTime;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(final Long value) {
        this.value = value;
    }
}
