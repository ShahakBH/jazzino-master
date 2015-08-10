package com.yazino.web.payment;

import com.yazino.platform.Partner;

import java.math.BigDecimal;

public class PaymentContextBuilder {
    private BigDecimal playerId;
    private BigDecimal sessionId;
    private String playerName;
    private String gameType;
    private String emailAddress;
    private String paymentOptionId;
    private Long promotionId;
    private Partner partnerId;

    public static PaymentContextBuilder builder() {
        return new PaymentContextBuilder();
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public PaymentContextBuilder withPartnerId(final Partner partnerId) {
            this.partnerId = partnerId;
            return this;
        }


    public BigDecimal getPlayerId() {
        return playerId;
    }

    public PaymentContextBuilder withPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
        return this;
    }

    public String getPlayerName() {
        return playerName;
    }

    public PaymentContextBuilder withPlayerName(final String playerName) {
        this.playerName = playerName;
        return this;
    }

    public String getGameType() {
        return gameType;
    }

    public PaymentContextBuilder withGameType(final String gameType) {
        this.gameType = gameType;
        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public PaymentContextBuilder withEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public String getPaymentOptionId() {
        return paymentOptionId;
    }

    public PaymentContextBuilder withPaymentOptionId(final String paymentOptionId) {
        this.paymentOptionId = paymentOptionId;
        return this;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public PaymentContextBuilder withPromotionId(final Long promotionId) {
        this.promotionId = promotionId;
        return this;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public PaymentContextBuilder withSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public PaymentContext build() {
        return new PaymentContext(playerId, sessionId, playerName, gameType, emailAddress, paymentOptionId, promotionId, partnerId);
    }

}
