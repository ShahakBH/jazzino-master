package com.yazino.bi.operations.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;

/**
 * Providing information on used currencies
 */
@Repository
public class JdbcCurrencyInformationDao implements CurrencyInformationDao {

    static final String RATES_LIST_REQUEST = "SELECT CODE,RATE FROM CURRENCY_RATES ORDER BY ID";

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the DAO
     *
     * @param jdbcTemplate JDBC tempmate to use
     */
    @Autowired(required = true)
    public JdbcCurrencyInformationDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super();
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, Double> getExchangeRates() {
        return jdbcTemplate.query(RATES_LIST_REQUEST, new ResultSetExtractor<Map<String, Double>>() {
            @Override
            public Map<String, Double> extractData(final ResultSet rs) throws SQLException,
                    DataAccessException {
                final Map<String, Double> ratesTable = new LinkedHashMap<String, Double>();
                while (rs.next()) {
                    ratesTable.put(rs.getString("CODE"), rs.getDouble("RATE"));
                }
                return ratesTable;
            }
        });
    }
}
