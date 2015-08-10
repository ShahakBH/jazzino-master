package com.yazino.bi.payment;

import java.math.BigDecimal;

public class PaymentOptionBuilder {
    private String id;
    private BigDecimal numChipsPerPurchase;
    private BigDecimal amountRealMoneyPerPurchase;
    private String realMoneyCurrency;
    private String currencyLabel;

    public PaymentOptionBuilder setId(final String newId) {
        this.id = newId;
        return this;
    }

    public PaymentOptionBuilder setNumChipsPerPurchase(final BigDecimal newNumChipsPerPurchase) {
        this.numChipsPerPurchase = newNumChipsPerPurchase;
        return this;
    }

    public PaymentOptionBuilder setAmountRealMoneyPerPurchase(final BigDecimal newAmountRealMoneyPerPurchase) {
        this.amountRealMoneyPerPurchase = newAmountRealMoneyPerPurchase;
        return this;
    }

    public PaymentOptionBuilder setRealMoneyCurrency(final String newRealMoneyCurrency) {
        this.realMoneyCurrency = newRealMoneyCurrency;
        return this;
    }

    public PaymentOptionBuilder setCurrencyLabel(final String newCurrencyLabel) {
        this.currencyLabel = newCurrencyLabel;
        return this;
    }

    public PaymentOption createPaymentOption() {
        final PaymentOption option =  new PaymentOption();
        option.setId(id);
        option.setNumChipsPerPurchase(numChipsPerPurchase);
        option.setAmountRealMoneyPerPurchase(amountRealMoneyPerPurchase);
        option.setCurrencyLabel(currencyLabel);
        option.setRealMoneyCurrency(realMoneyCurrency);
        return option;
    }
}
