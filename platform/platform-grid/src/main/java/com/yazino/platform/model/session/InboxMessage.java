package com.yazino.platform.model.session;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import com.yazino.game.api.NewsEvent;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * InboxMessage is a persistent version of a NewsEvent
 */
public class InboxMessage implements Serializable {

    private static final long serialVersionUID = -6361092699541209371L;

    private final BigDecimal playerId;
    private final NewsEvent newsEvent;
    private final DateTime receivedTime;
    private boolean read;

    public InboxMessage(final BigDecimal playerId,
                        final NewsEvent newsEvent,
                        final DateTime receivedTime) {
        notNull(playerId, "playerId is null");
        notNull(newsEvent, "newsEvent is null");
        notNull(receivedTime, "receivedTime is null");
        this.playerId = playerId;
        this.newsEvent = newsEvent;
        this.receivedTime = receivedTime;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public NewsEvent getNewsEvent() {
        return newsEvent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(final boolean read) {
        this.read = read;
    }

    public DateTime getReceivedTime() {
        return receivedTime;
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
        final InboxMessage rhs = (InboxMessage) obj;
        return new EqualsBuilder()
                .append(read, rhs.read)
                .append(newsEvent, rhs.newsEvent)
                .append(receivedTime, rhs.receivedTime)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(read)
                .append(newsEvent)
                .append(BigDecimals.strip(playerId))
                .append(receivedTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(read)
                .append(newsEvent)
                .append(playerId)
                .append(receivedTime)
                .toString();
    }
}
