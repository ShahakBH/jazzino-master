package com.yazino.bi.aggregator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;


public class PlayerActivityDailyRecord extends PlayerActivity {
    private String referrer;
    private DateTime regDate;

    public PlayerActivityDailyRecord(BigDecimal playerId, String game, String platform, DateTime activityTs, String referrer, DateTime regDate) {
        super(playerId, game, platform, activityTs);
        this.referrer = referrer;
        this.regDate = regDate;
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
        final PlayerActivityDailyRecord rhs=(PlayerActivityDailyRecord) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(referrer, rhs.referrer)
                .append(regDate, rhs.regDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(referrer)
                .append(regDate)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append(referrer)
                .append(regDate)
                .toString();
    }
}
