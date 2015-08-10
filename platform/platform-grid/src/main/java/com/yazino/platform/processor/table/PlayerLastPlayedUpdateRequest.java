package com.yazino.platform.processor.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class PlayerLastPlayedUpdateRequest {
    private BigDecimal playerId;
    private DateTime lastPlayed;

    public PlayerLastPlayedUpdateRequest() {
    }

    public PlayerLastPlayedUpdateRequest(final BigDecimal playerId, final DateTime lastPlayed) {
        notNull(playerId);

        this.playerId = playerId;
        this.lastPlayed = lastPlayed;
    }

    @SpaceId(autoGenerate = false)
    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public DateTime getLastPlayed() {
        return lastPlayed;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setLastPlayed(final DateTime lastPlayed) {
        this.lastPlayed = lastPlayed;
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
        PlayerLastPlayedUpdateRequest rhs = (PlayerLastPlayedUpdateRequest) obj;
        return new EqualsBuilder()
                .append(this.playerId, rhs.playerId)
                .append(this.lastPlayed, rhs.lastPlayed)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(lastPlayed)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("lastPlayed", lastPlayed)
                .toString();
    }
}
