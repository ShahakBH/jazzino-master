package com.yazino.platform.model.statistic;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.statistic.GameStatistic;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

@SpaceClass(replicate = false)
public class PlayerGameStatistics implements Serializable {
    private static final long serialVersionUID = 7997592485734550895L;

    private String id;
    private BigDecimal playerId;
    private BigDecimal tableId;
    private String gameType;
    private String clientId;
    private Collection<GameStatistic> statistics;

    public PlayerGameStatistics() {
    }

    public PlayerGameStatistics(final BigDecimal playerId,
                                final BigDecimal tableId,
                                final String gameType,
                                final String clientId,
                                final Collection<GameStatistic> statistics) {
        this(playerId, gameType, statistics);
        this.tableId = tableId;
        this.clientId = clientId;
    }

    public PlayerGameStatistics(final BigDecimal playerId,
                                final String gameType,
                                final Collection<GameStatistic> statistics) {
        this.playerId = playerId;
        this.gameType = gameType;
        this.statistics = statistics;
    }

    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Collection<GameStatistic> getStatistics() {
        return statistics;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public void setStatistics(final Collection<GameStatistic> statistics) {
        this.statistics = statistics;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
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
        final PlayerGameStatistics rhs = (PlayerGameStatistics) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(gameType, rhs.gameType)
                .append(clientId, rhs.clientId)
                .append(statistics, rhs.statistics)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(BigDecimals.strip(playerId))
                .append(BigDecimals.strip(tableId))
                .append(gameType)
                .append(clientId)
                .append(statistics)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(playerId)
                .append(tableId)
                .append(gameType)
                .append(clientId)
                .append(statistics)
                .toString();
    }
}
