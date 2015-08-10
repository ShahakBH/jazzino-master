package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

public class TrophyWinner implements Serializable {
    private static final long serialVersionUID = -1078960135941569539L;

    private final BigDecimal playerId;
    private final String name;
    private final String pictureUrl;
    private final int position;
    private final DateTime awardTime;

    public TrophyWinner(final BigDecimal playerId,
                        final String name,
                        final String pictureUrl,
                        final Integer position) {
        this(playerId, name, pictureUrl, position, null);
    }

    public TrophyWinner(final BigDecimal playerId,
                        final String name,
                        final String pictureUrl,
                        final DateTime awardTime) {
        this(playerId, name, pictureUrl, null, awardTime);
    }

    public TrophyWinner(final BigDecimal playerId,
                        final String name,
                        final String pictureUrl,
                        final Integer position,
                        final DateTime awardTime) {
        this.playerId = playerId;
        this.name = name;
        this.pictureUrl = pictureUrl;
        if (position != null) {
            this.position = position;
        } else {
            this.position = -1;
        }
        this.awardTime = awardTime;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public int getPosition() {
        return position;
    }

    public DateTime getAwardTime() {
        return awardTime;
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
        final TrophyWinner rhs = (TrophyWinner) obj;
        return new EqualsBuilder()
                .append(position, rhs.position)
                .append(awardTime, rhs.awardTime)
                .append(name, rhs.name)
                .append(pictureUrl, rhs.pictureUrl)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(position)
                .append(awardTime)
                .append(name)
                .append(pictureUrl)
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(position)
                .append(awardTime)
                .append(name)
                .append(pictureUrl)
                .append(playerId)
                .toString();
    }
}
