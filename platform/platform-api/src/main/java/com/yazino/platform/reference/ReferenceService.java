package com.yazino.platform.reference;

import java.util.Set;

public interface ReferenceService {

    /**
     * Retrieve the available countries.
     *
     * @return a set of the available countries. Never null.
     */
    Set<Country> getCountries();

    /**
     * Get the currency for a country.
     * <p/>
     * If the country is unknown or null then a default currency is returned.
     *
     * @param countryNameOrISOCode the country's name or its ISO 3166-1 code.
     * @return the currency to use. Never null.
     */
    Currency getPreferredCurrency(String countryNameOrISOCode);

}
