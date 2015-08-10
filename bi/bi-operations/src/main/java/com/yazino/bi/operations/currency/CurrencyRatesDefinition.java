package com.yazino.bi.operations.currency;

import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.bi.operations.currency.persistence.CurrencyInformationDao;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Bean used to define the currency rates (/GBP) in Spring configs
 */
public class CurrencyRatesDefinition {
    private final Map<String, BigDecimal> conversionRates;

    private CurrencyInformationDao dao;
    private boolean initialized = false;

    /**
     * Creates the rates holder and initializes it with a map of rates
     *
     * @param conversionRates Map of rates
     * @param dao             DAO used to extract additional currency rates
     */
    @Autowired
    public CurrencyRatesDefinition(final Map<String, BigDecimal> conversionRates,
                                   final CurrencyInformationDao dao) {
        this.conversionRates = conversionRates;
        this.dao = dao;
    }

    /**
     * Gets the conversion rates list from the server and updates the actually loaded list
     */
    public void updateFromDatabase() {
        if (dao == null) {
            return;
        }
        final Map<String, BigDecimal> exchangeRates = dao.getExchangeRates();
        for (final Entry<String, BigDecimal> rateEntry : exchangeRates.entrySet()) {
            conversionRates.put(rateEntry.getKey(), rateEntry.getValue());
        }
    }

    /**
     * Updates from the data source if the service was not initialized only
     */
    private void updateIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;
        updateFromDatabase();
    }

    /**
     * Returns a rate for the given code
     *
     * @param currencyCode Currency code
     * @return Rate related to a main defined currency
     */
    public BigDecimal getRate(final String currencyCode) {
        updateIfNeeded();
        return conversionRates.get(currencyCode);
    }

    public Map<String, BigDecimal> getConversionRates() {
        updateIfNeeded();
        return conversionRates;
    }

    /**
     * Merges the external source with existing currency rates table
     *
     * @param otherRates new (eventually, partial) rates table
     */
    public void updateRatesFromExternalSource(final Map<String, BigDecimal> otherRates) {
        for (final Entry<String, BigDecimal> rate : otherRates.entrySet()) {
            conversionRates.put(rate.getKey(), rate.getValue());
        }
        if (dao == null) {
            return;
        }
        dao.updateCurrencyRates(conversionRates);
    }
}
