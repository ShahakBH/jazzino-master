package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.currency.CurrencyRatesDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.bi.operations.model.CurrencyRatesCommand;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages the list of available currencies and their rates
 */
@Controller
public class CurrenciesManagementController {

    private final CurrencyRatesDefinition rates;

    /**
     * Creates the controller and connects it to it's data source
     *
     * @param rates Currencies rates source
     */
    @Autowired
    public CurrenciesManagementController(final CurrencyRatesDefinition rates) {
        this.rates = rates;
    }

    /**
     * Does the initial load of the rates table
     *
     * @return Model and view pair
     */
    @RequestMapping(value = {"/ratesDefinition"}, method = RequestMethod.GET)
    public ModelAndView initializeRatesTable() {
        rates.updateFromDatabase();
        final Map<String, BigDecimal> commandRates = new LinkedHashMap<>(this.rates.getConversionRates());
        final CurrencyRatesCommand command = new CurrencyRatesCommand();
        command.setRates(commandRates);

        return new ModelAndView("ratesDefinition", "command", command);
    }

    /**
     * Updates the rates table
     *
     * @param command Command containing the currency rates
     * @return Updated model/view pair
     */
    @RequestMapping(value = {"/ratesDefinition"}, method = RequestMethod.POST)
    public ModelAndView updateRatesTable(final CurrencyRatesCommand command) {
        rates.updateRatesFromExternalSource(command.getRates());
        final Map<String, BigDecimal> commandRates = new LinkedHashMap<>(this.rates.getConversionRates());
        command.setRates(commandRates);

        return new ModelAndView("ratesDefinition", "command", command);
    }

}
