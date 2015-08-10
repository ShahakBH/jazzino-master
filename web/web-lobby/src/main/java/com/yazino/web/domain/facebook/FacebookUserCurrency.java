package com.yazino.web.domain.facebook;

import com.restfb.json.JsonObject;

import java.math.BigDecimal;

import static java.math.BigDecimal.valueOf;

public class FacebookUserCurrency {

    private final Double currencyExchange;
    private final String userCurrency;
    private final Integer currencyOffset;
    private final Double currencyExchangeInverse;

    public FacebookUserCurrency(final Double currencyExchange, // TODO double or integer?
                                final String userCurrency,
                                final Integer currencyOffset,
                                final Double currencyExchangeInverse) {
        this.currencyExchange = currencyExchange;
        this.userCurrency = userCurrency;
        this.currencyOffset = currencyOffset;
        this.currencyExchangeInverse = currencyExchangeInverse;
    }

    public Double getCurrencyExchange() {
        return currencyExchange;
    }

    public String getUserCurrency() {
        return userCurrency;
    }

    public BigDecimal getCurrencyOffset() {
        return valueOf(currencyOffset);
    }

    public BigDecimal getCurrencyExchangeInverse() {
        return valueOf(currencyExchangeInverse);
    }

    public FacebookUserCurrency(JsonObject currencyObject) {
         currencyExchange = currencyObject.getDouble("currency_exchange");
         userCurrency = currencyObject.getString("user_currency");
         currencyOffset = currencyObject.getInt("currency_offset");
         currencyExchangeInverse = currencyObject.getDouble("currency_exchange_inverse");
    }
}
