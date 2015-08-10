package com.yazino.web.payment.itunes;

import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.payment.PaymentState;
import org.apache.commons.lang.builder.ToStringBuilder;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.Currency;

public class Order {

    private final String mCashier;
    private final PaymentPreferences.PaymentMethod mPaymentMethod;

    private BigDecimal mCashAmount = BigDecimal.ZERO;
    private PaymentState mPaymentState = PaymentState.Unknown;
    private BigDecimal mPlayerId;
    private PaymentOption mPaymentOption;
    private Currency mCurrency;
    private String mProductId;
    private String mGameType;
    private String mMessage;
    private String mOrderId;

    protected Order(final String cashier,
                    final PaymentPreferences.PaymentMethod paymentMethod) {
        mCashier = cashier;
        mPaymentMethod = paymentMethod;
    }

    public BigDecimal getCashAmount() {
        return mCashAmount;
    }

    public void setCashAmount(final BigDecimal cashAmount) {
        mCashAmount = cashAmount;
    }

    public BigDecimal getChipAmount() {
        final PaymentOption option = getPaymentOption();
        if (option != null) {
            return option.getNumChipsPerPurchase(getPaymentMethod().name());
        } else {
            return BigDecimal.ZERO;
        }
    }

    public void setPaymentOption(final PaymentOption paymentOption) {
        mPaymentOption = paymentOption;
    }

    public PaymentOption getPaymentOption() {
        return mPaymentOption;
    }

    public PaymentState getPaymentState() {
        return mPaymentState;
    }

    public void setPaymentState(final PaymentState paymentState) {
        mPaymentState = paymentState;
    }

    public Currency getCurrency() {
        return mCurrency;
    }

    public void setCurrency(final Currency currency) {
        mCurrency = currency;
    }

    public String getProductId() {
        return mProductId;
    }

    public void setProductId(final String productId) {
        mProductId = productId;
    }

    public String getGameType() {
        return mGameType;
    }

    public void setGameType(final String gameType) {
        mGameType = gameType;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(final String message) {
        mMessage = message;
    }

    public String getCashier() {
        return mCashier;
    }

    public PaymentPreferences.PaymentMethod getPaymentMethod() {
        return mPaymentMethod;
    }

    public void setOrderId(final String orderId) {
        mOrderId = orderId;
    }

    public String getOrderId() {
        return mOrderId;
    }

    public BigDecimal getPlayerId() {
        return mPlayerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        mPlayerId = playerId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("cashier", mCashier).
                append("paymentMethod", mPaymentMethod).
                append("cashAmount", mCashAmount).
                append("paymentState", mPaymentState).
                append("playerId", mPlayerId).
                append("paymentOption", mPaymentOption).
                append("currency", mCurrency).
                append("productId", mProductId).
                append("gameType", mGameType).
                append("message", mMessage).
                append("orderId", mOrderId).
                toString();
    }
}
