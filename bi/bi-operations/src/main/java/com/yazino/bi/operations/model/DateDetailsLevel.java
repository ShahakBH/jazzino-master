package com.yazino.bi.operations.model;

public enum DateDetailsLevel {
    DAILY("By Day"), WEEKLY("By Week"), MONTHLY("By Month"), TOTALS("Totals");

    private final String readableName;

    private DateDetailsLevel(final String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }
}
