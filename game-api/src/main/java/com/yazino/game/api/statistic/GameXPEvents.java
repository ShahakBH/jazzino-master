package com.yazino.game.api.statistic;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class GameXPEvents {

    private final Map<BigDecimal, Set<StatisticEvent>> playerXPEvents = new HashMap<BigDecimal, Set<StatisticEvent>>();

    public void addEvent(final BigDecimal playerId,
                         final XPStatisticEventType type,
                         final Object... parameters) {
        notNull(playerId, "playerId is null");
        notNull(type, "type is null");
        if (!playerXPEvents.containsKey(playerId)) {
            playerXPEvents.put(playerId, new HashSet<StatisticEvent>());
        }
        playerXPEvents.get(playerId).add(new StatisticEvent(type.name(), 0, 1, parameters));
    }

    public void clear() {
        playerXPEvents.clear();
    }

    public Map<BigDecimal, Set<StatisticEvent>> getPlayerXPEvents() {
        return Collections.unmodifiableMap(playerXPEvents);
    }

    public Set<StatisticEvent> getXPEventsForPlayer(final BigDecimal playerId) {
        final Set<StatisticEvent> events = playerXPEvents.get(playerId);
        if (events == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(events);
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
        final GameXPEvents rhs = (GameXPEvents) obj;
        return new EqualsBuilder()
                .append(playerXPEvents, rhs.playerXPEvents)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerXPEvents)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerXPEvents)
                .toString();
    }
}
