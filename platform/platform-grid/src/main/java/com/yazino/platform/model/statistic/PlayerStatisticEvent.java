package com.yazino.platform.model.statistic;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.statistic.StatisticEvent;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class PlayerStatisticEvent implements Serializable, Iterable<StatisticEvent> {
    private static final long serialVersionUID = -3445271564996690300L;

    private String spaceId;
    private BigDecimal playerId;
    private String gameType;
    private Collection<StatisticEvent> events;

    public PlayerStatisticEvent() {
    }

    public PlayerStatisticEvent(final BigDecimal playerId,
                                final String gameType,
                                final Collection<StatisticEvent> events) {
        notNull(playerId, "Player ID may not be null");
        notNull(gameType, "Game Type may not be null");

        this.playerId = playerId;
        this.gameType = gameType;

        this.events = new CopyOnWriteArrayList<StatisticEvent>();
        if (events != null) {
            this.events.addAll(events);
        }
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Collection<StatisticEvent> getEvents() {
        return events;
    }

    public void setEvents(final Collection<StatisticEvent> events) {
        this.events = events;
    }


    public Collection<StatisticEvent> eventsByNames(final Collection<String> eventNames) {
        notNull(eventNames, "Event Name may not be null");

        if (events == null || eventNames.size() == 0) {
            return Collections.emptyList();
        }

        final Collection<StatisticEvent> matchingEvents
                = Collections2.filter(events, new Predicate<StatisticEvent>() {
            @Override
            public boolean apply(final StatisticEvent achievementEvent) {
                return eventNames.contains(achievementEvent.getEvent());
            }
        });

        if (matchingEvents == null) {
            return Collections.emptyList();
        }

        return newArrayList(matchingEvents);
    }

    @Override
    public Iterator<StatisticEvent> iterator() {
        if (events != null) {
            return events.iterator();
        }
        return Collections.<StatisticEvent>emptySet().iterator();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final PlayerStatisticEvent rhs = (PlayerStatisticEvent) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(gameType, rhs.gameType)
                .append(events, rhs.events)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(spaceId)
                .append(BigDecimals.strip(playerId))
                .append(gameType)
                .append(events)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
