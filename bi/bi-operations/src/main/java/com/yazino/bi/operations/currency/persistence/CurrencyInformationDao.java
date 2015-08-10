package com.yazino.bi.operations.currency.persistence;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Providing information on used currencies
 */
public interface CurrencyInformationDao {

    /**
     * Loads the exchange rates from the database
     *
     * @return Map of currency exchange rates
     */
    Map<String, BigDecimal> getExchangeRates();

    /**
     * Persists the currency rates set
     *
     * @param rates Rates table
     */
    void updateCurrencyRates(Map<String, BigDecimal> rates);
}
