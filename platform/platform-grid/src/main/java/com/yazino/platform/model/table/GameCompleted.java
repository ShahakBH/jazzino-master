package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.GameStatus;

import java.io.Serializable;
import java.math.BigDecimal;

@SpaceClass(replicate = false)
public class GameCompleted implements Serializable {
    private static final long serialVersionUID = 3036221237464381899L;

    private String id;
    private BigDecimal tableId;
    private String clientId;
    private GameStatus gameStatus;
    private String gameType;

    public GameCompleted() {
    }


    public GameCompleted(final GameStatus gameStatus,
                         final String gameType,
                         final BigDecimal tableId,
                         final String clientId) {
        this.gameStatus = gameStatus;
        this.gameType = gameType;
        this.tableId = tableId;
        this.clientId = clientId;
    }


    @SpaceId(autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @SpaceRouting
    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(final GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
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
        final GameCompleted rhs = (GameCompleted) obj;
        return new EqualsBuilder()
                .append(clientId, rhs.clientId)
                .append(gameStatus, rhs.gameStatus)
                .append(gameType, rhs.gameType)
                .append(id, rhs.id)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(clientId)
                .append(gameStatus)
                .append(gameType)
                .append(id)
                .append(BigDecimals.strip(tableId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(clientId)
                .append(gameStatus)
                .append(gameType)
                .append(id)
                .append(tableId)
                .toString();
    }
}
