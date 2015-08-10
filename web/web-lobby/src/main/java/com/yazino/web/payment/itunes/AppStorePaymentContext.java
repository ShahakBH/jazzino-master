package com.yazino.web.payment.itunes;

import com.yazino.platform.Partner;
import com.yazino.web.payment.PaymentContext;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.Currency;

class AppStorePaymentContext extends PaymentContext {

    private final String mReceipt;
    private final BigDecimal mCashAmount;
    private final Currency mCurrency;
    private final String mTransactionIdentifier;
    private final String mProductIdentifier;
    private final Partner mPartnerId;

    AppStorePaymentContext(final BigDecimal playerId,
                           final BigDecimal sessionId,
                           final String gameType,
                           final String receipt,
                           final BigDecimal cashAmount,
                           final Currency currency,
                           final String transactionIdentifier,
                           final String productIdentifier,
                           final Partner partnerId) {
        super(playerId, sessionId, null, gameType, null, null, null, partnerId);
        mReceipt = receipt;
        mCashAmount = cashAmount;
        mCurrency = currency;
        mTransactionIdentifier = transactionIdentifier;
        mProductIdentifier = productIdentifier;
        mPartnerId = partnerId;
    }

    public Partner getPartnerId() {
        return mPartnerId;
    }

    public String getReceipt() {
        return mReceipt;
    }

    public Currency getCurrency() {
        return mCurrency;
    }

    public BigDecimal getCashAmount() {
        return mCashAmount;
    }

    public String getTransactionIdentifier() {
        return mTransactionIdentifier;
    }

    public String getProductIdentifier() {
        return mProductIdentifier;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("receipt", mReceipt);
        builder.append("currency", mCurrency);
        builder.append("cashAmount", mCashAmount);
        builder.append("transactionIdentifier", mTransactionIdentifier);
        builder.append("productIdentifier", mProductIdentifier);
        builder.append("partnerId", mPartnerId);
        return builder.toString();
    }
}
