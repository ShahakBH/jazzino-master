package com.yazino.bi.operations.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Command object managing the currency rates
 */
public class CurrencyRatesCommand {
    private Map<String, BigDecimal> rates = new LinkedHashMap<>();

    public Map<String, BigDecimal> getRates() {
        return rates;
    }

    public void setRates(final Map<String, BigDecimal> rates) {
        this.rates = rates;
    }
}
