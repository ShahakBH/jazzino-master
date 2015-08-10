package com.yazino.bi.aggregator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class PlayerActivity {
    protected BigDecimal playerId;
    private String game;
    private String platform;
    private DateTime activityTs;

    public PlayerActivity(BigDecimal playerId, String game, String platform, DateTime activityTs) {
        this.playerId = playerId;
        this.game = game;
        this.platform = platform;
        this.activityTs = activityTs;
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
        final PlayerActivity rhs = (PlayerActivity) obj;
        return new EqualsBuilder()
                .append(game, rhs.game)
                .append(platform, rhs.platform)
                .append(activityTs, rhs.activityTs)
                .isEquals()
                && playerId.compareTo(rhs.playerId) == 0;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerId)
                .append(game)
                .append(platform)
                .append(activityTs)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(game)
                .append(platform)
                .append(activityTs)
                .toString();
    }
}