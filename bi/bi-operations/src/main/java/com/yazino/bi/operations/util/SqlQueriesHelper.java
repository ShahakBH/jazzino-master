package com.yazino.bi.operations.util;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Assists the SQL queries generation
 */
public final class SqlQueriesHelper {
    /**
     * No public constructor
     */
    private SqlQueriesHelper() {
    }

    /**
     * Generates the "players per source" query taking into account the exchange rates
     *
     * @param conversionRates Conversion rates table
     * @return Full query to execute
     */
    public static String generatePlayerQueryPerRate(final Map<String, Double> conversionRates) {
        final StringBuilder query = new StringBuilder("CASE WHEN x.CURRENCY_CODE='GBP' THEN x.AMOUNT");
        if (conversionRates != null) {
            for (final Entry<String, Double> rate : conversionRates.entrySet()) {
                query.append(" WHEN x.CURRENCY_CODE='" + rate.getKey() + "' THEN x.AMOUNT/" + rate.getValue());
            }
        }
        query.append(" ELSE 0 END");
        return query.toString();
    }
}
