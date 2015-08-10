package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class TournamentRankView implements Serializable {
    private static final long serialVersionUID = 2903882001149193677L;
    private final long rank;
    private final String playerName;
    private final BigDecimal playerId;
    private final BigDecimal balance;
    private final BigDecimal prize;
    private final BigDecimal tableId;
    private final Status status;
    private final EliminationReason eliminationReason;

    public enum Status {
        ADDITION_PENDING, CHARGED, ACTIVE, ELIMINATED, REMOVAL_PENDING, REFUNDED, TERMINATED
    }

    public enum EliminationReason {
        OFFLINE, NOT_ENOUGH_CHIPS_FOR_ROUND, NO_CHIPS, KICKED_OUT_BY_GAME
    }

    private TournamentRankView(final long rank,
                               final BigDecimal playerId,
                               final String playerName,
                               final BigDecimal balance,
                               final BigDecimal prize,
                               final BigDecimal tableId,
                               final Status status,
                               final EliminationReason eliminationReason) {
        this.playerId = playerId;
        this.balance = balance;
        this.playerName = playerName;
        this.prize = prize;
        this.rank = rank;
        this.tableId = tableId;
        this.status = status;
        this.eliminationReason = eliminationReason;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getPlayerName() {
        return playerName;
    }

    public BigDecimal getPrize() {
        return prize;
    }

    public long getRank() {
        return rank;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Status getStatus() {
        return status;
    }

    public EliminationReason getEliminationReason() {
        return eliminationReason;
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
        final TournamentRankView rhs = (TournamentRankView) obj;
        return new EqualsBuilder()
                .append(balance, rhs.balance)
                .append(playerName, rhs.playerName)
                .append(prize, rhs.prize)
                .append(rank, rhs.rank)
                .append(status, rhs.status)
                .append(eliminationReason, rhs.eliminationReason)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(balance)
                .append(playerName)
                .append(prize)
                .append(rank)
                .append(BigDecimals.strip(tableId))
                .append(status)
                .append(eliminationReason)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(balance)
                .append(playerName)
                .append(prize)
                .append(rank)
                .append(tableId)
                .append(status)
                .append(eliminationReason)
                .toString();
    }

    public static class Builder {
        private long rank;
        private String playerName;
        private BigDecimal playerId;
        private BigDecimal balance;
        private BigDecimal prize;
        private BigDecimal tableId;
        private Status status;
        private EliminationReason eliminationReason;

        public TournamentRankView build() {
            return new TournamentRankView(
                    rank, playerId, playerName, balance, prize, tableId, status, eliminationReason);
        }

        public Builder rank(final int newRank) {
            this.rank = newRank;
            return this;
        }

        public Builder prize(final BigDecimal newPrize) {
            this.prize = newPrize;
            return this;
        }

        public Builder playerName(final String newPlayerName) {
            this.playerName = newPlayerName;
            return this;
        }

        public Builder playerId(final BigDecimal newPlayerId) {
            this.playerId = newPlayerId;
            return this;
        }

        public Builder tableId(final BigDecimal newTableId) {
            this.tableId = newTableId;
            return this;
        }

        public Builder status(final Status newStatus) {
            this.status = newStatus;
            return this;
        }

        public Builder eliminationReason(final EliminationReason newEliminationReason) {
            this.eliminationReason = newEliminationReason;
            return this;
        }

        public Builder balance(final BigDecimal newBalance) {
            this.balance = newBalance;
            return this;
        }
    }
}
