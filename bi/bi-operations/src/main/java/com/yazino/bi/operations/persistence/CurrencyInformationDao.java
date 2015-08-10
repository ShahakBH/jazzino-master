package com.yazino.bi.operations.persistence;

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
    Map<String, Double> getExchangeRates();
}
