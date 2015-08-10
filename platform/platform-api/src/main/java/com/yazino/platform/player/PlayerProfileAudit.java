package com.yazino.platform.player;

import com.google.common.collect.ComparisonChain;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerProfileAudit implements Serializable, Comparable<PlayerProfileAudit> {
    private static final long serialVersionUID = 1283838262854467085L;

    private final BigDecimal playerId;
    private final PlayerProfileStatus oldStatus;
    private final PlayerProfileStatus newStatus;
    private final String changedBy;
    private final String reason;
    private final DateTime timestamp;

    public PlayerProfileAudit(final BigDecimal playerId,
                              final PlayerProfileStatus oldStatus,
                              final PlayerProfileStatus newStatus,
                              final String changedBy,
                              final String reason,
                              final DateTime timestamp) {
        notNull(playerId, "playerId may not be null");
        notNull(oldStatus, "oldStatus may not be null");
        notNull(newStatus, "newStatus may not be null");
        notNull(changedBy, "changedBy may not be null");
        notNull(reason, "reason may not be null");
        notNull(timestamp, "timestamp may not be null");

        this.playerId = playerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public PlayerProfileStatus getOldStatus() {
        return oldStatus;
    }

    public PlayerProfileStatus getNewStatus() {
        return newStatus;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getReason() {
        return reason;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public int compareTo(final PlayerProfileAudit otherRecord) {
        if (otherRecord == null) {
            return 1;
        }
        return ComparisonChain.start()
                .compare(timestamp, otherRecord.getTimestamp())
                .compare(playerId, otherRecord.getPlayerId())
                .compare(newStatus, otherRecord.getNewStatus())
                .compare(oldStatus, otherRecord.getOldStatus())
                .result();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PlayerProfileAudit rhs = (PlayerProfileAudit) obj;
        return new EqualsBuilder()
                .append(this.playerId, rhs.playerId)
                .append(this.oldStatus, rhs.oldStatus)
                .append(this.newStatus, rhs.newStatus)
                .append(this.changedBy, rhs.changedBy)
                .append(this.reason, rhs.reason)
                .append(this.timestamp, rhs.timestamp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(oldStatus)
                .append(newStatus)
                .append(changedBy)
                .append(reason)
                .append(timestamp)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("oldStatus", oldStatus)
                .append("newStatus", newStatus)
                .append("changedBy", changedBy)
                .append("reason", reason)
                .append("timestamp", timestamp)
                .toString();
    }
}
