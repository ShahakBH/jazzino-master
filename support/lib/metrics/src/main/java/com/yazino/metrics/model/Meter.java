package com.yazino.metrics.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import static java.lang.Math.max;
import static org.apache.commons.lang3.Validate.notNull;

public class Meter implements Metric<Meter> {
    private static final long serialVersionUID = 5077113436001533742L;

    private static final String METRIC_TYPE = "meter";

    private DateTime timestamp;
    private int count;
    private double oneMinuteRatePerSecond;
    private double fiveMinuteRatePerSecond;
    private double fifteenMinuteRatePerSecond;
    private double meanRatePerSecond;

    public Meter(final DateTime timestamp,
                 final int count,
                 final double oneMinuteRatePerSecond,
                 final double fiveMinuteRatePerSecond,
                 final double fifteenMinuteRatePerSecond,
                 final double meanRatePerSecond) {
        notNull(timestamp, "timestamp may not be null");

        this.timestamp = timestamp;
        this.count = count;
        this.oneMinuteRatePerSecond = oneMinuteRatePerSecond;
        this.fiveMinuteRatePerSecond = fiveMinuteRatePerSecond;
        this.fifteenMinuteRatePerSecond = fifteenMinuteRatePerSecond;
        this.meanRatePerSecond = meanRatePerSecond;
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public Meter add(final Meter metric) {
        if (metric == null) {
            return this;
        }

        return new Meter(new DateTime(max(timestamp.getMillis(), metric.timestamp.getMillis())),
                count + metric.count,
                oneMinuteRatePerSecond + metric.oneMinuteRatePerSecond,
                fiveMinuteRatePerSecond + metric.fiveMinuteRatePerSecond,
                fifteenMinuteRatePerSecond + metric.fifteenMinuteRatePerSecond,
                meanRatePerSecond + metric.meanRatePerSecond);
    }

    @Override
    public String getType() {
        return METRIC_TYPE;
    }

    public int getCount() {
        return count;
    }

    public double getOneMinuteRatePerSecond() {
        return oneMinuteRatePerSecond;
    }

    public double getFiveMinuteRatePerSecond() {
        return fiveMinuteRatePerSecond;
    }

    public double getFifteenMinuteRatePerSecond() {
        return fifteenMinuteRatePerSecond;
    }

    public double getMeanRatePerSecond() {
        return meanRatePerSecond;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Meter rhs = (Meter) obj;
        return new EqualsBuilder()
                .append(this.timestamp, rhs.timestamp)
                .append(this.count, rhs.count)
                .append(this.oneMinuteRatePerSecond, rhs.oneMinuteRatePerSecond)
                .append(this.fiveMinuteRatePerSecond, rhs.fiveMinuteRatePerSecond)
                .append(this.fifteenMinuteRatePerSecond, rhs.fifteenMinuteRatePerSecond)
                .append(this.meanRatePerSecond, rhs.meanRatePerSecond)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(timestamp)
                .append(count)
                .append(oneMinuteRatePerSecond)
                .append(fiveMinuteRatePerSecond)
                .append(fifteenMinuteRatePerSecond)
                .append(meanRatePerSecond)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("timestamp", timestamp)
                .append("count", count)
                .append("oneMinuteRatePerSecond", oneMinuteRatePerSecond)
                .append("fiveMinuteRatePerSecond", fiveMinuteRatePerSecond)
                .append("fifteenMinuteRatePerSecond", fifteenMinuteRatePerSecond)
                .append("meanRatePerSecond", meanRatePerSecond)
                .toString();
    }
}
