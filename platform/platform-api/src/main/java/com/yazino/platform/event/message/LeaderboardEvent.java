package com.yazino.platform.event.message;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaderboardEvent implements PlatformEvent, Serializable {

    private static final long serialVersionUID = 400307168487858007L;
    @JsonProperty("id")
    private BigDecimal leaderboardId;
    @JsonProperty("gt")
    private String gameType;
    @JsonProperty("pos")
    private Map<Integer, BigDecimal> playerPositions;
    @JsonProperty("endTs")
    private DateTime endTs;

    private LeaderboardEvent() {
    }

    public LeaderboardEvent(final BigDecimal leaderboardId,
                            final String gameType,
                            final DateTime endTs,
                            final Map<Integer, BigDecimal> playerPositions) {
        notNull(leaderboardId, "leaderboardId may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(playerPositions, "playerPositions may not be null");
        this.leaderboardId = leaderboardId;
        this.gameType = gameType;
        this.endTs = endTs;
        this.playerPositions = playerPositions;
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.LEADERBOARD;
    }

    @Override
    public String toString() {
        return "LeaderboardEvent{"
                + "leaderboardId=" + leaderboardId
                + ", gameType='" + gameType + '\''
                + ", playerPositions=" + playerPositions
                + ", endTs=" + endTs
                + '}';
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
        final LeaderboardEvent rhs = (LeaderboardEvent) obj;
        return new EqualsBuilder()
                .append(endTs, rhs.endTs)
                .append(gameType, rhs.gameType)
                .append(leaderboardId, rhs.leaderboardId)
                .append(playerPositions, rhs.playerPositions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(endTs)
                .append(gameType)
                .append(leaderboardId)
                .append(playerPositions)
                .toHashCode();
    }

    public BigDecimal getLeaderboardId() {
        return leaderboardId;
    }

    private void setLeaderboardId(final BigDecimal leaderboardId) {
        this.leaderboardId = leaderboardId;
    }

    public String getGameType() {
        return gameType;
    }

    private void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Map<Integer, BigDecimal> getPlayerPositions() {
        return playerPositions;
    }

    private void setPlayerPositions(final Map<Integer, BigDecimal> playerPositions) {
        this.playerPositions = playerPositions;
    }

    public DateTime getEndTs() {
        return endTs;
    }

    private void setEndTs(final DateTime endTs) {
        this.endTs = endTs;
    }
}
