package com.yazino.bi.operations.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Bean used to define the currecy rates (/GBP) in Spring configs
 */
@Service
public class CurrencyRatesDefinition {
    private final Map<String, Double> conversionRates = new LinkedHashMap<String, Double>();

    private CurrencyInformationDao dao;

    public static final String USD_CODE = "USD";
    public static final String EUR_CODE = "EUR";
    public static final String GBP_CODE = "GBP";
    public static final String SEK_CODE = "SEK";
    public static final String AUD_CODE = "AUD";
    public static final String NZD_CODE = "NZD";
    public static final String CAD_CODE = "CAD";

    /**
     * Creates the rates holder and initializes it with a map of rates
     *
     * @param conversionRates Map of rates
     * @param dao             DAO used to extract additional currency rates
     */
    @Autowired
    public CurrencyRatesDefinition(final CurrencyInformationDao dao) {
        super();
        this.dao = dao;
    }

    @Resource(name = "currencyRates")
    public void setConversionRates(final Map conversionRates) {
        // magic to cope with type erasure
        this.conversionRates.clear();
        if (conversionRates != null) {
            for (Object key : conversionRates.keySet()) {
                Object value = conversionRates.get(key);
                if (value != null && !(value instanceof Double)) {
                    value = Double.valueOf(value.toString());
                }
                this.conversionRates.put(key.toString(), (Double) value);
            }
        }
    }

    /**
     * Gets the conversion rates list from the server and updates the actually loaded list
     */
    public void updateConversionRates() {
        if (dao == null) {
            return;
        }
        final Map<String, Double> exchangeRates = dao.getExchangeRates();
        for (final Entry<String, Double> rateEntry : exchangeRates.entrySet()) {
            conversionRates.put(rateEntry.getKey(), rateEntry.getValue());
        }
    }

    /**
     * Returns a rate for the given code
     *
     * @param currencyCode Currency code
     * @return Rate related to a main defined currency
     */
    public Double getRate(final String currencyCode) {
        final Double rate = conversionRates.get(currencyCode);
        if (rate == null) {
            return 0D;
        }
        return rate;
    }

    public Map<String, Double> getConversionRates() {
        updateConversionRates();
        return Collections.unmodifiableMap(conversionRates);
    }
}
