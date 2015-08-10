package com.yazino.platform.payment;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class PendingSettlement implements Serializable {
    private static final long serialVersionUID = 483313968170119095L;

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
    private final Currency baseCurrency;
    private final BigDecimal basePrice;

    public PendingSettlement(final DateTime timestamp,
                             final String internalTransactionId,
                             final String externalTransactionId,
                             final BigDecimal playerId,
                             final String playerName,
                             final String playerCountry,
                             final String cashierName,
                             final Currency currency,
                             final BigDecimal price,
                             final BigDecimal chips,
                             final Currency baseCurrency,
                             final BigDecimal basePrice) {
        notNull(timestamp, "timestamp may not be null");
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(externalTransactionId, "externalTransactionId may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(cashierName, "cashierName may not be null");
        notNull(currency, "currency may not be null");
        notNull(price, "price may not be null");
        notNull(chips, "chips may not be null");

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
        this.baseCurrency = baseCurrency;
        this.basePrice = basePrice;
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

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
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
        PendingSettlement rhs = (PendingSettlement) obj;
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
                .append(this.baseCurrency, rhs.baseCurrency)
                .append(this.basePrice, rhs.basePrice)
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
                .append(baseCurrency)
                .append(basePrice)
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
                .append("baseCurrency", baseCurrency)
                .append("basePrice", basePrice)
                .toString();
    }
}
