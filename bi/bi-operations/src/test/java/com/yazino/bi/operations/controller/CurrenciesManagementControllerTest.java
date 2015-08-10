package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.currency.CurrencyRatesDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.model.CurrencyRatesCommand;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class CurrenciesManagementControllerTest {
    private CurrenciesManagementController underTest;

    @Mock
    private CurrencyRatesDefinition rates;

    @Before
    public void init() {
        underTest = new CurrenciesManagementController(rates);
    }

    @Test
    public void shouldDoInitialRatesLoad() {
        // GIVEN the rates update returns a map
        final Map<String, BigDecimal> rateMap = new HashMap<>();
        rateMap.put("USD", new BigDecimal(3));
        rateMap.put("EUR", new BigDecimal(2));
        given(rates.getConversionRates()).willReturn(rateMap);

        // WHEN calling the initial load from the controller
        final ModelAndView mv = underTest.initializeRatesTable();

        // THEN the MV contains the same map
        assertEquals(rateMap, ((CurrencyRatesCommand) mv.getModelMap().get("command")).getRates());

        // AND the model name matches the expectations
        assertEquals("ratesDefinition", mv.getViewName());

        // AND the rates are updates
        verify(rates).updateFromDatabase();
    }

    @Test
    public void shouldUpdateRatesOnSubmit() {
        // GIVEN the map of updated rates
        final Map<String, BigDecimal> rateMap = new HashMap<>();
        rateMap.put("USD", new BigDecimal(3));
        rateMap.put("EUR", new BigDecimal(2));
        final CurrencyRatesCommand command = new CurrencyRatesCommand();
        command.setRates(rateMap);

        // AND the rates definition returns the same thing as it receives
        given(rates.getConversionRates()).willReturn(rateMap);

        // WHEN the command with the map is submitted to the controller
        final ModelAndView mv = underTest.updateRatesTable(command);

        // THEN the rates table is consistently updated
        verify(rates).updateRatesFromExternalSource(rateMap);

        // AND the updated table is sent back to the view
        assertEquals(rateMap, ((CurrencyRatesCommand) mv.getModelMap().get("command")).getRates());
    }
}
