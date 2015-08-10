package com.yazino.platform.payment.android;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class AndroidPaymentStateDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal playerId;
    private AndroidPaymentState state;
    private String internalTransactionId;
    private String googleOrderNumber;
    private String gameType;
    private String productId;
    private Long promoId;

    public AndroidPaymentStateDetails() {
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public AndroidPaymentState getState() {
        return state;
    }

    public void setState(AndroidPaymentState state) {
        this.state = state;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public void setInternalTransactionId(String internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

    public String getGoogleOrderNumber() {
        return googleOrderNumber;
    }

    public void setGoogleOrderNumber(String googleOrderNumber) {
        this.googleOrderNumber = googleOrderNumber;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getPromoId() {
        return promoId;
    }

    public void setPromoId(Long promoId) {
        this.promoId = promoId;
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
        AndroidPaymentStateDetails rhs = (AndroidPaymentStateDetails) obj;
        return new EqualsBuilder()
                .append(this.state, rhs.state)
                .append(this.internalTransactionId, rhs.internalTransactionId)
                .append(this.googleOrderNumber, rhs.googleOrderNumber)
                .append(this.gameType, rhs.gameType)
                .append(this.productId, rhs.productId)
                .append(this.promoId, rhs.promoId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(state)
                .append(internalTransactionId)
                .append(googleOrderNumber)
                .append(gameType)
                .append(productId)
                .append(promoId)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("state", state)
                .append("internalTransactionId", internalTransactionId)
                .append("googleOrderNumber", googleOrderNumber)
                .append("gameType", gameType)
                .append("productId", productId)
                .append("promoId", promoId)
                .toString();
    }
}
