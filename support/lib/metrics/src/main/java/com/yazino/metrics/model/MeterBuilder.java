package com.yazino.metrics.model;

import org.joda.time.DateTime;

public final class MeterBuilder {

    private DateTime timestamp;
    private int count;
    private double oneMinuteRatePerSecond;
    private double fiveMinuteRatePerSecond;
    private double fifteenMinuteRatePerSecond;
    private double meanRatePerSecond;

    private MeterBuilder() {

    }

    public static MeterBuilder newMeter() {
        return new MeterBuilder();
    }

    public MeterBuilder withTimestamp(final DateTime newTimestamp) {
        this.timestamp = newTimestamp;
        return this;
    }

    public MeterBuilder withCount(final int newCount) {
        this.count = newCount;
        return this;
    }

    public MeterBuilder withOneMinuteRatePerSecond(final double newOneMinuteRatePerSecond) {
        this.oneMinuteRatePerSecond = newOneMinuteRatePerSecond;
        return this;
    }

    public MeterBuilder withFiveMinuteRatePerSecond(final double newFiveMinuteRatePerSecond) {
        this.fiveMinuteRatePerSecond = newFiveMinuteRatePerSecond;
        return this;
    }

    public MeterBuilder withFifteenMinuteRatePerSecond(final double newFifteenMinuteRatePerSecond) {
        this.fifteenMinuteRatePerSecond = newFifteenMinuteRatePerSecond;
        return this;
    }

    public MeterBuilder withMeanRatePerSecond(final double newMeanRatePerSecond) {
        this.meanRatePerSecond = newMeanRatePerSecond;
        return this;
    }

    public Meter build() {
        return new Meter(timestamp, count, oneMinuteRatePerSecond, fiveMinuteRatePerSecond,
                fifteenMinuteRatePerSecond, meanRatePerSecond);
    }

}
