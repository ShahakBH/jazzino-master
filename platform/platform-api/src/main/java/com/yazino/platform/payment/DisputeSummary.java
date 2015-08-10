package com.yazino.platform.payment;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class DisputeSummary implements Serializable {
    private static final long serialVersionUID = 483313968170119023L;

    private final DateTime timestamp;
    private final String internalTransactionId;
    private final String externalTransactionId;
    private final BigDecimal playerId;
    private final String playerName;
    private final String playerCountry;
    private final String cashierName;
    private final Currency currency;
    private final BigDecimal price;
    private final BigDecimal chips;
    private final String description;
    private final DisputeStatus status;

    public DisputeSummary(final String internalTransactionId,
                          final String cashierName,
                          final String externalTransactionId,
                          final DisputeStatus status,
                          final DateTime timestamp,
                          final BigDecimal playerId,
                          final String playerName,
                          final String playerCountry,
                          final Currency currency,
                          final BigDecimal price,
                          final BigDecimal chips,
                          final String description) {
        notNull(timestamp, "timestamp may not be null");
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(externalTransactionId, "externalTransactionId may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(cashierName, "cashierName may not be null");
        notNull(currency, "currency may not be null");
        notNull(price, "price may not be null");
        notNull(chips, "chips may not be null");
        notNull(status, "status may not be null");

        this.timestamp = timestamp;
        this.internalTransactionId = internalTransactionId;
        this.externalTransactionId = externalTransactionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerCountry = playerCountry;
        this.cashierName = cashierName;
        this.currency = currency;
        this.price = price;
        this.chips = chips;
        this.description = description;
        this.status = status;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerCountry() {
        return playerCountry;
    }

    public String getCashierName() {
        return cashierName;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public String getDescription() {
        return description;
    }

    public DisputeStatus getStatus() {
        return status;
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
        DisputeSummary rhs = (DisputeSummary) obj;
        return new EqualsBuilder()
                .append(this.timestamp, rhs.timestamp)
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .append(this.externalTransactionId, rhs.externalTransactionId)
                .append(this.playerId, rhs.playerId)
                .append(this.playerName, rhs.playerName)
                .append(this.playerCountry, rhs.playerCountry)
                .append(this.cashierName, rhs.cashierName)
                .append(this.currency, rhs.currency)
                .append(this.price, rhs.price)
                .append(this.chips, rhs.chips)
                .append(this.description, rhs.description)
                .append(this.status, rhs.status)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(timestamp)
                .append(internalTransactionId)
                .append(externalTransactionId)
                .append(playerId)
                .append(playerName)
                .append(playerCountry)
                .append(cashierName)
                .append(currency)
                .append(price)
                .append(chips)
                .append(description)
                .append(status)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("timestamp", timestamp)
                .append("internalTransactionId", internalTransactionId)
                .append("externalTransactionId", externalTransactionId)
                .append("playerId", playerId)
                .append("playerName", playerName)
                .append("playerCountry", playerCountry)
                .append("cashierName", cashierName)
                .append("currency", currency)
                .append("price", price)
                .append("chips", chips)
                .append("description", description)
                .append("status", status)
                .toString();
    }
}
