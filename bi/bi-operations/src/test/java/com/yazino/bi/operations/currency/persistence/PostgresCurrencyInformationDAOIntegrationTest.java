package com.yazino.bi.operations.currency.persistence;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class PostgresCurrencyInformationDAOIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PostgresCurrencyInformationDAO underTest;

    @Before
    public void setUp() {
        jdbcTemplate.update("DELETE FROM CURRENCY_RATES");
        jdbcTemplate.update("INSERT INTO CURRENCY_RATES (CURRENCY_CODE,RATE) VALUES ('GBP',1.0),('USD',0.7),('CAD',0.75),('AUD',0.76),('EUR','1.15')");
    }

    @Test
    public void theExchangeRatesCanBeRetrievedFromTheDataSource() {
        final Map<String, BigDecimal> exchangeRates = underTest.getExchangeRates();

        assertThat(exchangeRates.size(), is(equalTo(5)));
        assertThat(exchangeRates.get("GBP"), is(equalTo(new BigDecimal("1.0000"))));
        assertThat(exchangeRates.get("USD"), is(equalTo(new BigDecimal("0.7000"))));
        assertThat(exchangeRates.get("CAD"), is(equalTo(new BigDecimal("0.7500"))));
        assertThat(exchangeRates.get("AUD"), is(equalTo(new BigDecimal("0.7600"))));
        assertThat(exchangeRates.get("EUR"), is(equalTo(new BigDecimal("1.1500"))));
    }

    @Test
    public void currencyRatesCanBeUpdated() {
        final Map<String, BigDecimal> newRates = new HashMap<>();
        newRates.put("USD", new BigDecimal("0.7133"));
        newRates.put("AUD", new BigDecimal("0.7634"));

        underTest.updateCurrencyRates(newRates);

        final Map<String, BigDecimal> exchangeRates = underTest.getExchangeRates();
        assertThat(exchangeRates.size(), is(equalTo(5)));
        assertThat(exchangeRates.get("USD"), is(equalTo(new BigDecimal("0.7133"))));
        assertThat(exchangeRates.get("AUD"), is(equalTo(new BigDecimal("0.7634"))));
    }

    @Test
    public void updatingCurrencyRatesDoesNotAlterTheRatesOfNonUpdatedCurrencies() {
        final Map<String, BigDecimal> newRates = new HashMap<>();
        newRates.put("USD", new BigDecimal("0.7133"));
        newRates.put("AUD", new BigDecimal("0.7634"));

        underTest.updateCurrencyRates(newRates);

        final Map<String, BigDecimal> exchangeRates = underTest.getExchangeRates();
        assertThat(exchangeRates.size(), is(equalTo(5)));
        assertThat(exchangeRates.get("GBP"), is(equalTo(new BigDecimal("1.0000"))));
        assertThat(exchangeRates.get("CAD"), is(equalTo(new BigDecimal("0.7500"))));
        assertThat(exchangeRates.get("EUR"), is(equalTo(new BigDecimal("1.1500"))));
    }

}
