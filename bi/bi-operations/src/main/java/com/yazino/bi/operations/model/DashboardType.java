package com.yazino.bi.operations.model;

/**
 * Type of the dashboard to display
 */
public enum DashboardType {
    OVERVIEW("Overview"), DETAILS("Details");

    private String readableName;

    /**
     * Constructs a type with the readable name
     *
     * @param readableName Name to display to the end user
     */
    private DashboardType(final String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }

    public void setReadableName(final String readableName) {
        this.readableName = readableName;
    }
}
