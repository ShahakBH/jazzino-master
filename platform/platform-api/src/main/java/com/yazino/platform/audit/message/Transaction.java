package com.yazino.platform.audit.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 8379260471203809818L;

    @JsonProperty("acc")
    private BigDecimal accountId;
    @JsonProperty("amt")
    private BigDecimal amount;
    @JsonProperty("type")
    private String type;
    @JsonProperty("ref")
    private String reference;
    @JsonProperty("ts")
    private Long timestamp;
    @JsonProperty("bal")
    private BigDecimal runningBalance;
    @JsonProperty("gid")
    private Long gameId;
    @JsonProperty("tid")
    private BigDecimal tableId;
    @JsonProperty("sid")
    private BigDecimal sessionId;
    @JsonProperty
    private BigDecimal playerId;

    public Transaction() {
    }

    public Transaction(final BigDecimal accountId,
                       final BigDecimal amount,
                       final String type,
                       final String reference,
                       final Long timestamp,
                       final BigDecimal runningBalance,
                       final Long gameId,
                       final BigDecimal tableId,
                       final BigDecimal sessionId,
                       final BigDecimal playerId) {
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.reference = reference;
        this.timestamp = timestamp;
        this.runningBalance = runningBalance;
        this.gameId = gameId;
        this.tableId = tableId;
        this.sessionId = sessionId;
        this.playerId = playerId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(final Long gameId) {
        this.gameId = gameId;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(final BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    private BigDecimal strip(final BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final Transaction rhs = (Transaction) obj;
        return new EqualsBuilder()
                .append(strip(amount), strip(rhs.amount))
                .append(reference, rhs.reference)
                .append(type, rhs.type)
                .append(timestamp, rhs.timestamp)
                .append(strip(runningBalance), strip(rhs.runningBalance))
                .append(gameId, rhs.gameId)
                .append(sessionId, rhs.sessionId)
                .isEquals()
                && BigDecimals.equalByComparison(accountId, rhs.accountId)
                && BigDecimals.equalByComparison(tableId, rhs.tableId)
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(accountId))
                .append(amount)
                .append(reference)
                .append(type)
                .append(timestamp)
                .append(runningBalance)
                .append(gameId)
                .append(BigDecimals.strip(tableId))
                .append(sessionId)
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
