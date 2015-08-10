package com.yazino.bi.payment.persistence;

import com.yazino.bi.payment.Chargeback;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;

import static java.lang.String.format;
import static java.math.BigDecimal.ROUND_HALF_EVEN;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCPaymentChargebackDAO {
    private static final String SQL_SAVE = "INSERT INTO PAYMENT_CHARGEBACK "
            + "(CHARGEBACK_REFERENCE,PROCESSING_DATE,INTERNAL_TRANSACTION_ID,TRANSACTION_DATE,PLAYER_ID,"
            + " CHARGEBACK_REASON_CODE,CHARGEBACK_REASON,ACCOUNT_NUMBER,AMOUNT,CURRENCY_CODE) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE CHARGEBACK_REASON_CODE=VALUES(CHARGEBACK_REASON_CODE),CHARGEBACK_REASON=VALUES(CHARGEBACK_REASON),"
            + "AMOUNT=VALUES(AMOUNT),CURRENCY_CODE=VALUES(CURRENCY_CODE)";

    private static final String SQL_SEARCH_BY_PROCESSING_DATE
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS c.*,p.NAME FROM PAYMENT_CHARGEBACK c "
            + "LEFT JOIN PLAYER p ON p.PLAYER_ID = c.PLAYER_ID "
            + "WHERE PROCESSING_DATE >= ? AND PROCESSING_DATE <= ? "
            + "ORDER BY CHARGEBACK_REFERENCE LIMIT ? OFFSET ?";
    private static final String SQL_SEARCH_BY_PROCESSING_DATE_AND_REASON
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS c.*,p.NAME FROM PAYMENT_CHARGEBACK c "
            + "LEFT JOIN PLAYER p ON p.PLAYER_ID = c.PLAYER_ID "
            + "WHERE PROCESSING_DATE >= ?  AND PROCESSING_DATE <= ? AND CHARGEBACK_REASON_CODE IN (%s) "
            + "ORDER BY CHARGEBACK_REFERENCE LIMIT ? OFFSET ?";
    private static final String SQL_FOUND_ROWS = "SELECT FOUND_ROWS()";

    private final ChargebackRowMapper chargebackRowMapper = new ChargebackRowMapper();

    private final JdbcTemplate jdbcTemplate;

    /**
     * CGLib constructor.
     */
    JDBCPaymentChargebackDAO() {
        this.jdbcTemplate = null;
    }

    @Autowired
    public JDBCPaymentChargebackDAO(@Qualifier("marketingJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(final Chargeback chargeback) {
        notNull(chargeback, "chargeback may not be null");

        jdbcTemplate.update(SQL_SAVE, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                ps.setString(1, chargeback.getReference());
                ps.setDate(2, new Date(chargeback.getProcessingDate().getMillis()));
                ps.setString(3, chargeback.getInternalTransactionId());
                ps.setDate(4, new Date(chargeback.getTransactionDate().getMillis()));
                ps.setBigDecimal(5, chargeback.getPlayerId());
                ps.setString(6, chargeback.getReasonCode().orNull());
                ps.setString(7, chargeback.getReason());
                ps.setString(8, chargeback.getAccountNumber());
                ps.setBigDecimal(9, chargeback.getAmount());
                ps.setString(10, chargeback.getCurrency().getCurrencyCode());
            }
        });
    }

    @Transactional(readOnly = true)
    public PagedData<Chargeback> search(final DateTime startDate,
                                        final DateTime endDate,
                                        final List<String> chargebackReasonCodes,
                                        final int page,
                                        final int pageSize) {
        notNull(startDate, "startDate may not be null");

        final List<Chargeback> searchResults;
        if (chargebackReasonCodes != null && !chargebackReasonCodes.isEmpty()) {
            final String query = format(SQL_SEARCH_BY_PROCESSING_DATE_AND_REASON, inClauseOfLength(chargebackReasonCodes.size()));
            searchResults = jdbcTemplate.query(query, new PreparedStatementSetter() {
                @Override
                public void setValues(final PreparedStatement ps) throws SQLException {
                    int index = 1;
                    ps.setDate(index++, new Date(startDate.getMillis()));
                    ps.setDate(index++, new Date(endDate.getMillis()));
                    for (String chargebackReasonCode : chargebackReasonCodes) {
                        ps.setString(index++, chargebackReasonCode);
                    }
                    ps.setInt(index++, pageSize);
                    ps.setInt(index, page * pageSize);
                }
            }, chargebackRowMapper);
        } else {
            searchResults = jdbcTemplate.query(SQL_SEARCH_BY_PROCESSING_DATE, chargebackRowMapper,
                    new Date(startDate.getMillis()), new Date(endDate.getMillis()), pageSize, page * pageSize);
        }

        final int totalRows = jdbcTemplate.queryForObject(SQL_FOUND_ROWS, Integer.class);
        return new PagedData<>(page * pageSize, searchResults.size(), totalRows, searchResults);
    }

    private String inClauseOfLength(final int size) {
        final StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            if (i != 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        return inClause.toString();
    }

    private class ChargebackRowMapper implements RowMapper<Chargeback> {
        @Override
        public Chargeback mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Currency currency = Currency.getInstance(rs.getString("CURRENCY_CODE"));
            return new Chargeback(rs.getString("CHARGEBACK_REFERENCE"),
                    new DateTime(rs.getDate("PROCESSING_DATE")),
                    rs.getString("INTERNAL_TRANSACTION_ID"),
                    new DateTime(rs.getDate("TRANSACTION_DATE")),
                    BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    rs.getString("NAME"),
                    rs.getString("CHARGEBACK_REASON_CODE"),
                    rs.getString("CHARGEBACK_REASON"),
                    rs.getString("ACCOUNT_NUMBER"),
                    rs.getBigDecimal("AMOUNT").setScale(currency.getDefaultFractionDigits(), ROUND_HALF_EVEN),
                    currency);
        }
    }
}
