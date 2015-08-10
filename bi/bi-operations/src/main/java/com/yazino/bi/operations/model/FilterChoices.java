package com.yazino.bi.operations.model;

/**
 * Possible selection criteria for payment reports
 */
public enum FilterChoices {
    NONE("None"), TIME("Time"), PAYMENT_METHOD("Payment method"), PACKAGE("Package"), COUNTRY("Country"), GAME("Game");

    private final String readableName;

    /**
     * Sets up a filter choice
     *
     * @param readableName Human-readable name
     */
    private FilterChoices(final String readableName) {
        this.readableName = readableName;
    }

    public String getReadableName() {
        return readableName;
    }
}
