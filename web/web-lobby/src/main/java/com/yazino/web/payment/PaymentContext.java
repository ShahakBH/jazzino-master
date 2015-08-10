package com.yazino.web.payment;

import com.yazino.platform.Partner;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class PaymentContext {
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final String playerName;
    private final String gameType;
    private final String emailAddress;
    private final String paymentOptionId;
    private final Long promotionId;
    private final Partner partnerId;

    public PaymentContext(final BigDecimal playerId,
                          final BigDecimal sessionId,
                          final String playerName,
                          final String gameType,
                          final String emailAddress,
                          final String paymentOptionId,
                          final Long promotionId,
                          final Partner partnerId) {
        this.playerId = playerId;
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.gameType = gameType;
        this.emailAddress = emailAddress;
        this.paymentOptionId = paymentOptionId;
        this.promotionId = promotionId;
        this.partnerId = partnerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getGameType() {
        return gameType;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public Long getPromotionId() {
        return promotionId;
    }
    public Partner getPartnerId() {
        return partnerId;
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
        final PaymentContext rhs = (PaymentContext) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(sessionId, rhs.sessionId)
                .append(playerName, rhs.playerName)
                .append(gameType, rhs.gameType)
                .append(emailAddress, rhs.emailAddress)
                .append(paymentOptionId, rhs.paymentOptionId)
                .append(promotionId, rhs.promotionId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerId)
                .append(sessionId)
                .append(playerName)
                .append(gameType)
                .append(emailAddress)
                .append(paymentOptionId)
                .append(promotionId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(sessionId)
                .append(playerName)
                .append(gameType)
                .append(emailAddress)
                .append(paymentOptionId)
                .append(promotionId)
                .toString();
    }

}
