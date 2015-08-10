package com.yazino.platform.model.statistic;

import com.yazino.platform.playerstatistic.StatisticEvent;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public class ExperienceFactor implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(ExperienceFactor.class);
    private static final long serialVersionUID = -1601900896434564056L;

    private final String event;
    private final BigDecimal points;

    public ExperienceFactor(final String event,
                            final BigDecimal points) {
        this.event = event;
        this.points = points;
    }

    public BigDecimal calculateExperiencePoints(final Collection<StatisticEvent> events) {
        for (StatisticEvent statisticEvent : events) {
            if (statisticEvent.getEvent().equals(event)) {
                final BigDecimal multiplier = resolveMultiplier(statisticEvent);
                final BigDecimal result = points.multiply(multiplier);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Giving %s (%s x %s) for %s", result, points, multiplier, event));
                }
                return result;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Experience event %s not found", event));
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveMultiplier(final StatisticEvent statisticEvent) {
        BigDecimal multiplier = BigDecimal.ONE;
        final List<Object> parameter = statisticEvent.getParameters();
        if (parameter.size() > 0 && parameter.get(0) instanceof Integer) {
            multiplier = new BigDecimal((Integer) parameter.get(0));
        }
        return multiplier;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ExperienceFactor rhs = (ExperienceFactor) obj;
        return new EqualsBuilder()
                .append(event, rhs.event)
                .append(points, rhs.points)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(event)
                .append(points)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(event)
                .append(points)
                .toString();
    }
}
