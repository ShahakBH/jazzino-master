package com.yazino.platform.account;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class TransactionContext implements Serializable {
    private static final long serialVersionUID = -4211782998893294194L;

    public static final TransactionContext EMPTY = new TransactionContext();

    private final Long gameId;
    private final BigDecimal tableId;
    private final BigDecimal sessionId;
    private final BigDecimal playerId;

    public TransactionContext() {
        this.gameId = null;
        this.tableId = null;
        this.sessionId = null;
        this.playerId = null;
    }

    private TransactionContext(final Long gameId,
                               final BigDecimal tableId,
                               final BigDecimal sessionId,
                               final BigDecimal playerId) {
        this.gameId = gameId;
        this.tableId = tableId;
        this.sessionId = sessionId;
        this.playerId = playerId;
    }

    public Long getGameId() {
        return gameId;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public static TransactionContextBuilder transactionContext() {
        return new TransactionContextBuilder();
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
        TransactionContext rhs = (TransactionContext) obj;
        return new EqualsBuilder()
                .append(this.gameId, rhs.gameId)
                .append(this.sessionId, rhs.sessionId)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId)
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameId)
                .append(BigDecimals.strip(tableId))
                .append(BigDecimals.strip(playerId))
                .append(sessionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("gameId", gameId)
                .append("tableId", tableId)
                .append("sessionId", sessionId)
                .append("playerId", playerId)
                .toString();
    }

    public static class TransactionContextBuilder {
        private Long gameId;
        private BigDecimal tableId;
        private BigDecimal sessionId;
        private BigDecimal playerId;

        public TransactionContextBuilder withGameId(final Long newGameId) {
            this.gameId = newGameId;
            return this;
        }

        public TransactionContextBuilder withTableId(final BigDecimal newTableId) {
            this.tableId = newTableId;
            return this;
        }

        public TransactionContextBuilder withSessionId(final BigDecimal newSessionId) {
            this.sessionId = newSessionId;
            return this;
        }

        public TransactionContextBuilder withPlayerId(final BigDecimal newPlayerId) {
            this.playerId = newPlayerId;
            return this;
        }

        public TransactionContext build() {
            return new TransactionContext(gameId, tableId, sessionId, playerId);
        }
    }
}
