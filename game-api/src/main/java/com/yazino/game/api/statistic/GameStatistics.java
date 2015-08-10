package com.yazino.game.api.statistic;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.*;

public class GameStatistics implements Serializable, Iterable<GameStatistic> {
    private static final long serialVersionUID = 4283222299561815191L;

    private final Collection<GameStatistic> statistics;
    private final Map<String, Collection<GameStatistic>> statisticsNames;

    public GameStatistics(final Collection<GameStatistic> statistics) {
        this.statistics = new ArrayList<GameStatistic>(statistics);
        this.statisticsNames = new HashMap<String, Collection<GameStatistic>>();

        for (GameStatistic statistic : statistics) {
            Collection<GameStatistic> statsForName = statisticsNames.get(statistic.getName());
            if (statsForName == null) {
                statsForName = new ArrayList<GameStatistic>();
                statisticsNames.put(statistic.getName(), statsForName);
            }
            statsForName.add(statistic);
        }
    }

    public Iterator<GameStatistic> iterator() {
        return statistics.iterator();
    }


    public Collection<GameStatistic> findByName(final String name) {
        final Collection<GameStatistic> statisticsForName = statisticsNames.get(name);
        if (statisticsForName == null) {
            return Collections.emptyList();
        }
        return statisticsForName;
    }

    public GameStatistic findUniqueByName(final String name) {
        final Collection<GameStatistic> matching = findByName(name);
        if (matching.size() == 0) {
            return null;
        }
        if (matching.size() > 1) {
            throw new IllegalArgumentException("Statistic '" + name
                    + "' is not unique (found " + matching.size() + ")");
        }
        return matching.iterator().next();
    }

    public boolean contains(final String... names) {
        for (String name : names) {
            if (!statisticsNames.containsKey(name)) {
                return false;
            }
        }
        return true;
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
        final GameStatistics rhs = (GameStatistics) obj;
        return new EqualsBuilder()
                .append(statistics, rhs.statistics)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(statistics)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(statistics)
                .toString();
    }
}
