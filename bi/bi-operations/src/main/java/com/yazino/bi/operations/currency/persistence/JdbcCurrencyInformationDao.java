package com.yazino.bi.operations.currency.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Providing information on used currencies
 */
public class JdbcCurrencyInformationDao implements CurrencyInformationDao {

    static final String RATES_LIST_REQUEST = "SELECT CODE,RATE FROM CURRENCY_RATES ORDER BY ID";
    static final String RATES_INSERT_REQUEST = "INSERT INTO CURRENCY_RATES(CODE,RATE) VALUES(?, ?)";
    static final String RATES_UPDATE_REQUEST = "UPDATE CURRENCY_RATES SET RATE = ? WHERE CODE = ?";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcCurrencyInformationDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, BigDecimal> getExchangeRates() {
        return jdbcTemplate.query(RATES_LIST_REQUEST, new ResultSetExtractor<Map<String, BigDecimal>>() {
            @Override
            public Map<String, BigDecimal> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                final Map<String, BigDecimal> ratesTable = new LinkedHashMap<>();
                while (rs.next()) {
                    ratesTable.put(rs.getString("CODE"), rs.getBigDecimal("RATE"));
                }
                return ratesTable;
            }
        });
    }

    @Override
    public void updateCurrencyRates(final Map<String, BigDecimal> rates) {
        final Map<String, BigDecimal> sourceRates = getExchangeRates();
        for (final Entry<String, BigDecimal> rateEntry : rates.entrySet()) {
            final String code = rateEntry.getKey();
            if (sourceRates.get(code) != null) {
                jdbcTemplate.update(RATES_UPDATE_REQUEST, rateEntry.getValue(), code);
            } else {
                jdbcTemplate.update(RATES_INSERT_REQUEST, code, rateEntry.getValue());
            }
        }
    }
}
