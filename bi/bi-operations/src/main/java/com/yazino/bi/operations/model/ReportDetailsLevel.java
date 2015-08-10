package com.yazino.bi.operations.model;

/**
 * Possible details levels for reports
 */
public enum ReportDetailsLevel {
    DAILY("by day", true), WEEKLY("by week", true), MONTHLY("by month", true), PAYMENT_METHOD_GROUP(
            "by payment method", false), PACKAGE_GROUP("by package", false), PACKAGE_CHIPS_GROUP("by package+chips",
            false), COUNTRY_GROUP("by country", false), COUNTRY_PACKAGE_GROUP("by country and package", false),
    GAME_GROUP("by game", false);

    private final String readableName;
    private final boolean dateSelection;

    /**
     * Creates one enum instance with the given readable name
     *
     * @param readableName  User-readable name
     * @param dateSelection True if this details level concerns dates
     */
    private ReportDetailsLevel(final String readableName, final boolean dateSelection) {
        this.readableName = readableName;
        this.dateSelection = dateSelection;
    }

    public String getReadableName() {
        return readableName;
    }

    public boolean isDateSelection() {
        return dateSelection;
    }
}
