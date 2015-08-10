package strata.server.lobby.api.promotion.domain;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

public class PlayerPromotionStatus implements Serializable {
    private static final long serialVersionUID = -1365037865712354135L;

    private final BigDecimal playerId;
    private final DateTime lastPlayed;
    private final DateTime lastTopup;
    private final int consecutiveDaysPlayed;
    private final boolean topUpAcknowledged;


    public PlayerPromotionStatus(final BigDecimal playerId,
                                 final DateTime lastPlayed,
                                 final DateTime lastTopup,
                                 final int consecutiveDaysPlayed,
                                 final boolean topUpAcknowledged) {
        this.playerId = playerId;
        this.lastPlayed = lastPlayed;
        this.lastTopup = lastTopup;
        this.consecutiveDaysPlayed = consecutiveDaysPlayed;
        this.topUpAcknowledged = topUpAcknowledged;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public DateTime getLastPlayed() {
        return lastPlayed;
    }

    public int getConsecutiveDaysPlayed() {
        return consecutiveDaysPlayed;
    }

    public DateTime getLastTopup() {
        return lastTopup;
    }

    public boolean isTopUpAcknowledged() {
        return topUpAcknowledged;

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
        final PlayerPromotionStatus rhs = (PlayerPromotionStatus) obj;
        return new EqualsBuilder()
                .append(lastPlayed, rhs.lastPlayed)
                .append(lastTopup, rhs.lastTopup)
                .append(consecutiveDaysPlayed, rhs.consecutiveDaysPlayed)
                .append(topUpAcknowledged, rhs.topUpAcknowledged)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(lastPlayed)
                .append(lastTopup)
                .append(consecutiveDaysPlayed)
                .append(topUpAcknowledged)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(lastPlayed)
                .append(lastTopup)
                .append(consecutiveDaysPlayed)
                .append(topUpAcknowledged)
                .toString();
    }
}
