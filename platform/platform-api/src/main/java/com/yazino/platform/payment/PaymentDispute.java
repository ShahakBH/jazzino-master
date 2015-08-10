package com.yazino.platform.payment;

import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;

import static org.apache.commons.lang3.Validate.notNull;

public class PaymentDispute implements Serializable {
    private static final long serialVersionUID = 3267525167863770924L;

    private final String internalTransactionId;
    private final String cashierName;
    private final String externalTransactionId;
    private final BigDecimal playerId;
    private final BigDecimal accountId;
    private final DisputeStatus status;
    private final DateTime disputeTimestamp;
    private final BigDecimal price;
    private final Currency currency;
    private final BigDecimal chips;
    private final ExternalTransactionType transactionType;
    private final String gameType;
    private final Platform platform;
    private final String paymentOptionId;
    private final Long promotionId;
    private final String description;
    private final DisputeResolution resolution;
    private final DateTime resolutionTimestamp;
    private final String resolutionNote;
    private final String resolvedBy;

    public PaymentDispute(final String internalTransactionId,
                          final String cashierName,
                          final String externalTransactionId,
                          final BigDecimal playerId,
                          final BigDecimal accountId,
                          final DisputeStatus status,
                          final DateTime disputeTimestamp,
                          final BigDecimal price,
                          final Currency currency,
                          final BigDecimal chips,
                          final ExternalTransactionType transactionType,
                          final String gameType,
                          final Platform platform,
                          final String paymentOptionId,
                          final Long promotionId,
                          final String description,
                          final DisputeResolution resolution,
                          final DateTime resolutionTimestamp,
                          final String resolutionNote,
                          final String resolvedBy) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        notNull(externalTransactionId, "externalTransactionId may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(accountId, "accountId may not be null");
        notNull(cashierName, "cashierName may not be null");
        notNull(status, "status may not be null");
        notNull(disputeTimestamp, "disputeTimestamp may not be null");
        notNull(price, "price may not be null");
        notNull(currency, "currency may not be null");
        notNull(chips, "chips may not be null");
        notNull(transactionType, "transactionType may not be null");
        notNull(description, "description may not be null");

        this.internalTransactionId = internalTransactionId;
        this.cashierName = cashierName;
        this.externalTransactionId = externalTransactionId;
        this.playerId = playerId;
        this.accountId = accountId;
        this.status = status;
        this.disputeTimestamp = disputeTimestamp;
        this.price = price;
        this.currency = currency;
        this.chips = chips;
        this.transactionType = transactionType;
        this.gameType = gameType;
        this.platform = platform;
        this.paymentOptionId = paymentOptionId;
        this.promotionId = promotionId;
        this.description = description;
        this.resolution = resolution;
        this.resolutionTimestamp = resolutionTimestamp;
        this.resolutionNote = resolutionNote;
        this.resolvedBy = resolvedBy;
    }

    public static PaymentDisputeBuilder copy(final PaymentDispute dispute) {
        return new PaymentDisputeBuilder(dispute);
    }

    public static PaymentDisputeBuilder newDispute(final String internalTransactionId,
                                                   final String cashierName,
                                                   final String externalTransactionId,
                                                   final BigDecimal playerId,
                                                   final BigDecimal accountId,
                                                   final DateTime disputeTimestamp,
                                                   final BigDecimal price,
                                                   final Currency currency,
                                                   final BigDecimal chips,
                                                   final ExternalTransactionType transactionType,
                                                   final String description) {
        return new PaymentDisputeBuilder(internalTransactionId,
                cashierName,
                externalTransactionId,
                playerId,
                accountId,
                disputeTimestamp,
                price,
                currency,
                chips,
                transactionType,
                description);
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getAccountId() {
        return accountId;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public DateTime getDisputeTimestamp() {
        return disputeTimestamp;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getChips() {
        return chips;
    }

    public ExternalTransactionType getTransactionType() {
        return transactionType;
    }

    public String getGameType() {
        return gameType;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public String getDescription() {
        return description;
    }

    public DisputeResolution getResolution() {
        return resolution;
    }

    public DateTime getResolutionTimestamp() {
        return resolutionTimestamp;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getResolvedBy() {
        return resolvedBy;
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
        PaymentDispute rhs = (PaymentDispute) obj;
        return new EqualsBuilder()
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .append(this.cashierName, rhs.cashierName)
                .append(this.externalTransactionId, rhs.externalTransactionId)
                .append(this.playerId, rhs.playerId)
                .append(this.accountId, rhs.accountId)
                .append(this.status, rhs.status)
                .append(this.disputeTimestamp, rhs.disputeTimestamp)
                .append(this.price, rhs.price)
                .append(this.currency, rhs.currency)
                .append(this.chips, rhs.chips)
                .append(this.transactionType, rhs.transactionType)
                .append(this.gameType, rhs.gameType)
                .append(this.platform, rhs.platform)
                .append(this.paymentOptionId, rhs.paymentOptionId)
                .append(this.promotionId, rhs.promotionId)
                .append(this.description, rhs.description)
                .append(this.resolution, rhs.resolution)
                .append(this.resolutionTimestamp, rhs.resolutionTimestamp)
                .append(this.resolutionNote, rhs.resolutionNote)
                .append(this.resolvedBy, rhs.resolvedBy)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(internalTransactionId)
                .append(cashierName)
                .append(externalTransactionId)
                .append(playerId)
                .append(accountId)
                .append(status)
                .append(disputeTimestamp)
                .append(price)
                .append(currency)
                .append(chips)
                .append(transactionType)
                .append(gameType)
                .append(platform)
                .append(paymentOptionId)
                .append(promotionId)
                .append(description)
                .append(resolution)
                .append(resolutionTimestamp)
                .append(resolutionNote)
                .append(resolvedBy)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("internalTransactionId", internalTransactionId)
                .append("cashierName", cashierName)
                .append("externalTransactionId", externalTransactionId)
                .append("playerId", playerId)
                .append("accountId", accountId)
                .append("status", status)
                .append("disputeTimestamp", disputeTimestamp)
                .append("price", price)
                .append("currency", currency)
                .append("chips", chips)
                .append("transactionType", transactionType)
                .append("gameType", gameType)
                .append("platform", platform)
                .append("paymentOptionId", paymentOptionId)
                .append("promotionId", promotionId)
                .append("description", description)
                .append("resolution", resolution)
                .append("resolutionTimestamp", resolutionTimestamp)
                .append("resolutionNote", resolutionNote)
                .append("resolvedBy", resolvedBy)
                .toString();
    }

    public static class PaymentDisputeBuilder {
        private String internalTransactionId;
        private String cashierName;
        private String externalTransactionId;
        private BigDecimal playerId;
        private BigDecimal accountId;
        private DisputeStatus status;
        private DateTime disputeTimestamp;
        private BigDecimal price;
        private Currency currency;
        private BigDecimal chips;
        private ExternalTransactionType transactionType;
        private String gameType;
        private Platform platform;
        private String paymentOptionId;
        private Long promotionId;
        private String description;
        private DisputeResolution resolution;
        private DateTime resolutionTimestamp;
        private String resolutionNote;
        private String resolvedBy;

        public PaymentDisputeBuilder(final PaymentDispute dispute) {
            this.internalTransactionId = dispute.getInternalTransactionId();
            this.cashierName = dispute.getCashierName();
            this.externalTransactionId = dispute.getExternalTransactionId();
            this.playerId = dispute.getPlayerId();
            this.accountId = dispute.getAccountId();
            this.status = dispute.getStatus();
            this.disputeTimestamp = dispute.getDisputeTimestamp();
            this.price = dispute.getPrice();
            this.currency = dispute.getCurrency();
            this.chips = dispute.getChips();
            this.transactionType = dispute.getTransactionType();
            this.gameType = dispute.getGameType();
            this.platform = dispute.getPlatform();
            this.paymentOptionId = dispute.getPaymentOptionId();
            this.promotionId = dispute.getPromotionId();
            this.description = dispute.getDescription();
            this.resolution = dispute.getResolution();
            this.resolutionTimestamp = dispute.getResolutionTimestamp();
            this.resolutionNote = dispute.getResolutionNote();
            this.resolvedBy = dispute.getResolvedBy();
        }

        public PaymentDisputeBuilder(final String internalTransactionId,
                                     final String cashierName,
                                     final String externalTransactionId,
                                     final BigDecimal playerId,
                                     final BigDecimal accountId,
                                     final DateTime disputeTimestamp,
                                     final BigDecimal price,
                                     final Currency currency,
                                     final BigDecimal chips,
                                     final ExternalTransactionType transactionType,
                                     final String description) {
            this.internalTransactionId = internalTransactionId;
            this.cashierName = cashierName;
            this.externalTransactionId = externalTransactionId;
            this.playerId = playerId;
            this.accountId = accountId;
            this.disputeTimestamp = disputeTimestamp;
            this.price = price;
            this.currency = currency;
            this.chips = chips;
            this.transactionType = transactionType;
            this.description = description;

            this.status = DisputeStatus.OPEN;
        }

        public PaymentDisputeBuilder withResolution(final DisputeResolution newResolution,
                                                    final DateTime newResolutionTimestamp,
                                                    final String newResolutionNote,
                                                    final String newResolvedBy) {
            if (newResolution != null) {
                notNull(newResolutionTimestamp, "resolutionTimestamp may not be null when resolution is non-null");
                notNull(newResolutionNote, "resolutionNote may not be null when resolution is non-null");
                notNull(newResolvedBy, "resolvedBy may not be null when resolution is non-null");

                this.status = DisputeStatus.CLOSED;
            }

            this.resolution = newResolution;
            this.resolutionTimestamp = newResolutionTimestamp;
            this.resolutionNote = newResolutionNote;
            this.resolvedBy = newResolvedBy;
            return this;
        }

        public PaymentDisputeBuilder withGameType(final String newGameType) {
            this.gameType = newGameType;
            return this;
        }

        public PaymentDisputeBuilder withPlatform(final Platform newPlatform) {
            this.platform = newPlatform;
            return this;
        }

        public PaymentDisputeBuilder withPaymentOptionId(final String newPaymentOptionId) {
            this.paymentOptionId = newPaymentOptionId;
            return this;
        }

        public PaymentDisputeBuilder withPromotionId(final Long newPromotionId) {
            this.promotionId = newPromotionId;
            return this;
        }

        public PaymentDispute build() {
            return new PaymentDispute(internalTransactionId,
                    cashierName,
                    externalTransactionId,
                    playerId,
                    accountId,
                    status,
                    disputeTimestamp,
                    price,
                    currency,
                    chips,
                    transactionType,
                    gameType,
                    platform,
                    paymentOptionId,
                    promotionId,
                    description,
                    resolution,
                    resolutionTimestamp,
                    resolutionNote,
                    resolvedBy);
        }
    }
}
