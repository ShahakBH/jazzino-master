package strata.server.lobby.api.promotion;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Unless player has been topped up all fields other player id and status will be null.
 */
public class TopUpResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TopUpStatus status;
    private final BigDecimal playerId;
    private final DateTime lastTopUpDate;
    private BigDecimal totalTopUpAmount;
    private Integer consecutiveDaysPlayed;

    public TopUpResult(final BigDecimal playerId, final TopUpStatus status, final DateTime lastTopUpDate) {
        this.playerId = playerId;
        this.status = status;
        this.lastTopUpDate = lastTopUpDate;
    }

    public TopUpStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalTopUpAmount() {
        return totalTopUpAmount;
    }

    public void setTotalTopUpAmount(final BigDecimal totalTopUpAmount) {
        this.totalTopUpAmount = totalTopUpAmount;
    }

    public Integer getConsecutiveDaysPlayed() {
        return consecutiveDaysPlayed;
    }

    public void setConsecutiveDaysPlayed(final Integer consecutiveDaysPlayed) {
        this.consecutiveDaysPlayed = consecutiveDaysPlayed;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public DateTime getLastTopUpDate() {
        return lastTopUpDate;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(17, 37, this);
    }
}
