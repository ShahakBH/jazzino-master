package com.yazino.bi.operations.currency.persistence;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.bi.operations.currency.persistence.JdbcCurrencyInformationDao.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
@RunWith(MockitoJUnitRunner.class)
public class JdbcCurrencyInformationDaoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet rs;

    private CurrencyInformationDao underTest;

    @Before
    public void init() {
        underTest = new JdbcCurrencyInformationDao(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReadCurrenciesList() throws SQLException {
        // GIVEN the query from persistence returns a correct map
        given(jdbcTemplate.query(eq(RATES_LIST_REQUEST), any(ResultSetExtractor.class))).willAnswer(
                new Answer<Map<String, BigDecimal>>() {
                    @Override
                    public Map<String, BigDecimal> answer(final InvocationOnMock invocation) throws Throwable {
                        final ResultSetExtractor<Map<String, BigDecimal>> rse =
                                (ResultSetExtractor<Map<String, BigDecimal>>) invocation.getArguments()[1];
                        return rse.extractData(rs);
                    }
                });

        // AND the result set returns a sequence of values
        given(rs.next()).willReturn(true, true, false);
        given(rs.getString("CODE")).willReturn("USD", "EUR");
        given(rs.getBigDecimal("RATE")).willReturn(new BigDecimal(3), new BigDecimal(2));

        // WHEN querying the DAO for the list of currencies
        final Map<String, BigDecimal> exchangeRates = underTest.getExchangeRates();

        // THEN the returned map matches one read from persistence
        final Map<String, BigDecimal> expectedRates = new LinkedHashMap<>();
        expectedRates.put("USD", new BigDecimal(3));
        expectedRates.put("EUR", new BigDecimal(2));
        assertEquals(expectedRates, exchangeRates);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateCurrenciesList() {
        // GIVEN the currencies table
        final Map<String, BigDecimal> existingRates = new LinkedHashMap<>();
        existingRates.put("USD", new BigDecimal(1));
        given(jdbcTemplate.query(eq(RATES_LIST_REQUEST), any(ResultSetExtractor.class))).willReturn(existingRates);

        final Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("USD", new BigDecimal(3));
        rates.put("EUR", new BigDecimal(2));

        // WHEN requesting the update
        underTest.updateCurrencyRates(rates);

        // THEN all the needed refreshes are done in the data source
        verify(jdbcTemplate).update(RATES_INSERT_REQUEST, "EUR", new BigDecimal(2));
        verify(jdbcTemplate).update(RATES_UPDATE_REQUEST, new BigDecimal(3), "USD");
    }
}
