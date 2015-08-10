package com.yazino.bi.payment.persistence;

import com.yazino.bi.payment.ExchangeRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCPaymentFXDAO {
    private static final String SQL_SAVE = "INSERT INTO PAYMENT_FX (CURRENCY,BASE_CURRENCY,SETTLEMENT_DATE,EXCHANGE_RATE) "
            + "VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE EXCHANGE_RATE=VALUES(EXCHANGE_RATE)";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCPaymentFXDAO(@Qualifier("marketingJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(final ExchangeRate exchangeRate) {
        notNull(exchangeRate, "exchangeRate may not be null");

        jdbcTemplate.update(SQL_SAVE, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setString(1, exchangeRate.getCurrency().getCurrencyCode());
                ps.setString(2, exchangeRate.getBaseCurrency().getCurrencyCode());
                ps.setTimestamp(3, new Timestamp(exchangeRate.getSettlementDate().getMillis()));
                ps.setBigDecimal(4, exchangeRate.getExchangeRate());
            }
        });
    }

}
