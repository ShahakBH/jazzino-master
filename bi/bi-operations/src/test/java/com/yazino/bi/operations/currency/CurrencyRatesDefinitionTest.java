package com.yazino.bi.operations.currency;

import com.yazino.bi.operations.currency.persistence.CurrencyInformationDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class CurrencyRatesDefinitionTest {
    @Mock
    private CurrencyInformationDao dao;

    @Test
    public void shouldMergePresetAndLoadedCurrencyLists() {
        // AND the exchange rates are loaded from persistence
        final Map<String, BigDecimal> dbRates = new LinkedHashMap<>();
        dbRates.put("AUD", new BigDecimal("2"));
        dbRates.put("USD", new BigDecimal("1.6"));
        given(dao.getExchangeRates()).willReturn(dbRates);

        // AND the currency rates definition
        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("1.5"));
        rates.put("USD", new BigDecimal("1.7"));

        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<>(rates), dao);

        // WHEN getting the map of currencies
        ratesDef.updateFromDatabase();
        final Map<String, BigDecimal> returnedRates = ratesDef.getConversionRates();

        // THEN the result is a merge of a preset map and the loaded currencies
        final Map<String, BigDecimal> expectedRates = new LinkedHashMap<>();
        expectedRates.put("EUR", new BigDecimal("1.5"));
        expectedRates.put("USD", new BigDecimal("1.6"));
        expectedRates.put("AUD", new BigDecimal("2"));
        assertEquals(expectedRates, returnedRates);
    }

    @Test
    public void shouldNotDefaultToZeroRate() {
        final Map<String, BigDecimal> dbRates = new LinkedHashMap<>();
        dbRates.put("AUD", new BigDecimal("2"));
        dbRates.put("USD", new BigDecimal("1.6"));
        given(dao.getExchangeRates()).willReturn(dbRates);

        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<String, BigDecimal>(), dao);

        assertNull(ratesDef.getRate("SEK")); //... rather than 0, which is what the original implementation returned
    }

    @Test
    public void shouldAllowOperationWithoutDAO() {
        // GIVEN the currency rates definition with no DAO set
        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("1.5"));
        rates.put("USD", new BigDecimal("1.7"));

        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<>(rates), null);

        // WHEN getting the map of currencies
        final Map<String, BigDecimal> returnedRates = ratesDef.getConversionRates();
        ratesDef.updateFromDatabase();

        // THEN the result is the preset list of currencies
        assertEquals(rates, returnedRates);
    }

    @Test
    public void shouldUpdateRatesFromExternalSource() {
        // GIVEN the external rates map
        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("1.5"));
        rates.put("USD", new BigDecimal("1.7"));

        final Map<String, BigDecimal> otherRates = new LinkedHashMap<>();
        otherRates.put("USD", new BigDecimal("1.6"));
        otherRates.put("GBP", new BigDecimal("1"));

        // WHEN applying the new map to the exchange rates definition
        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<>(rates), dao);
        ratesDef.updateRatesFromExternalSource(otherRates);

        // THEN we see the correct rates as a result
        final Map<String, BigDecimal> resRates = new LinkedHashMap<>();
        resRates.put("EUR", new BigDecimal("1.5"));
        resRates.put("USD", new BigDecimal("1.6"));
        resRates.put("GBP", new BigDecimal("1"));
        assertEquals(resRates, ratesDef.getConversionRates());

        // AND the new rates are persisted
        verify(dao).updateCurrencyRates(eq(resRates));
    }

    @Test
    public void shouldAllowUpdateWithNoDao() {
        // GIVEN the external rates map
        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("1.5"));
        rates.put("USD", new BigDecimal("1.7"));

        final Map<String, BigDecimal> otherRates = new LinkedHashMap<>();
        otherRates.put("USD", new BigDecimal("1.6"));
        otherRates.put("GBP", new BigDecimal("1"));

        // WHEN applying the new map to the exchange rates definition
        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<>(rates), null);
        ratesDef.updateRatesFromExternalSource(otherRates);

        // THEN we see the correct rates as a result
        final Map<String, BigDecimal> resRates = new LinkedHashMap<>();
        resRates.put("EUR", new BigDecimal("1.5"));
        resRates.put("USD", new BigDecimal("1.6"));
        resRates.put("GBP", new BigDecimal("1"));
        assertEquals(resRates, ratesDef.getConversionRates());
    }

    @Test
    public void shouldUpdateRatesOnFirstCallOnly() {
        // GIVEN the initial rates list
        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("1.5"));
        rates.put("USD", new BigDecimal("1.7"));

        // WHEN making a sequence of calls
        final CurrencyRatesDefinition ratesDef =
                new CurrencyRatesDefinition(new LinkedHashMap<>(rates), dao);
        ratesDef.getRate("USD");
        ratesDef.getRate("EUR");
        ratesDef.getConversionRates();

        // THEN only the first one asks for an update from persistence
        verify(dao).getExchangeRates();
        verifyNoMoreInteractions(dao);
    }
}
