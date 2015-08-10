package com.yazino.platform.payment;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCPaymentDisputeDAO {
    private static final String INSERT_DISPUTE = "INSERT INTO PAYMENT_DISPUTE "
            + " (INTERNAL_TRANSACTION_ID,CASHIER_NAME,EXTERNAL_TRANSACTION_ID,PLAYER_ID,ACCOUNT_ID,DISPUTE_STATUS,DISPUTE_TS,"
            + " PRICE,CURRENCY_CODE,CHIPS,TRANSACTION_TYPE,GAME_TYPE,PLATFORM,PAYMENT_OPTION_ID,PROMO_ID,DESCRIPTION,"
            + " RESOLUTION,RESOLUTION_TS,RESOLUTION_NOTE,RESOLVED_BY)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE "
            + " DISPUTE_STATUS=VALUES(DISPUTE_STATUS),RESOLUTION=VALUES(RESOLUTION),RESOLUTION_TS=VALUES(RESOLUTION_TS),"
            + " RESOLUTION_NOTE=VALUES(RESOLUTION_NOTE),RESOLVED_BY=VALUES(RESOLVED_BY)";
    private static final String SELECT_DISPUTE = "SELECT * FROM PAYMENT_DISPUTE WHERE INTERNAL_TRANSACTION_ID=?";
    private static final String SELECT_SUMMARY
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS "
            + "    pd.dispute_ts,"
            + "    pd.internal_transaction_id,"
            + "    pd.external_transaction_id,"
            + "    pd.player_id,"
            + "    lu.display_name,"
            + "    lu.country,"
            + "    pd.cashier_name,"
            + "    pd.currency_code,"
            + "    pd.price,"
            + "    pd.chips,"
            + "    pd.dispute_status,"
            + "    pd.description "
            + "FROM PAYMENT_DISPUTE pd "
            + "LEFT JOIN LOBBY_USER lu ON pd.PLAYER_ID = lu.PLAYER_ID "
            + "WHERE pd.dispute_status = 'OPEN' "
            + "ORDER BY pd.internal_transaction_id "
            + "LIMIT ? OFFSET ?";
    private static final String SQL_FOUND_ROWS = "SELECT FOUND_ROWS()";

    private final PaymentDisputeRowMapper disputeMapper = new PaymentDisputeRowMapper();
    private final DisputeSummaryRowMapper summaryMapper = new DisputeSummaryRowMapper();
    private final JdbcTemplate jdbcTemplate;

    public JDBCPaymentDisputeDAO() {
        // CGLib constructor
        this.jdbcTemplate = null;
    }

    @Autowired
    public JDBCPaymentDisputeDAO(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PaymentDispute> findByInternalTransactionId(final String internalTransactionId) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        verifyInitialisation();

        final List<PaymentDispute> result = jdbcTemplate.query(SELECT_DISPUTE, disputeMapper, internalTransactionId);
        if (result != null && !result.isEmpty()) {
            return Optional.of(result.get(0));
        }
        return Optional.absent();
    }

    @Transactional(readOnly = true)
    public PagedData<DisputeSummary> findOpenDisputes(final int page,
                                                      final int pageSize) {
        verifyInitialisation();

        final List<DisputeSummary> searchResults = jdbcTemplate.query(
                SELECT_SUMMARY, summaryMapper, pageSize, page * pageSize);
        final int totalRows = jdbcTemplate.queryForObject(SQL_FOUND_ROWS, Integer.class);
        return new PagedData<>(page * pageSize, searchResults.size(), totalRows, searchResults);
    }

    public void save(final PaymentDispute paymentDispute) {
        notNull(paymentDispute, "paymentDispute may not be null");
        verifyInitialisation();

        jdbcTemplate.update(INSERT_DISPUTE, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                int index = 1;
                ps.setString(index++, paymentDispute.getInternalTransactionId());
                ps.setString(index++, paymentDispute.getCashierName());
                ps.setString(index++, paymentDispute.getExternalTransactionId());
                ps.setBigDecimal(index++, paymentDispute.getPlayerId());
                ps.setBigDecimal(index++, paymentDispute.getAccountId());
                ps.setString(index++, nameOf(paymentDispute.getStatus()));
                ps.setTimestamp(index++, asTimestamp(paymentDispute.getDisputeTimestamp()));
                ps.setBigDecimal(index++, paymentDispute.getPrice());
                ps.setString(index++, currencyCodeFor(paymentDispute.getCurrency()));
                ps.setBigDecimal(index++, paymentDispute.getChips());
                ps.setString(index++, nameOf(paymentDispute.getTransactionType()));
                ps.setString(index++, paymentDispute.getGameType());
                ps.setString(index++, nameOf(paymentDispute.getPlatform()));
                ps.setString(index++, paymentDispute.getPaymentOptionId());
                if (paymentDispute.getPromotionId() != null) {
                    ps.setLong(index++, paymentDispute.getPromotionId());
                } else {
                    ps.setNull(index++, Types.INTEGER);
                }
                ps.setString(index++, paymentDispute.getDescription());
                ps.setString(index++, nameOf(paymentDispute.getResolution()));
                ps.setTimestamp(index++, asTimestamp(paymentDispute.getResolutionTimestamp()));
                ps.setString(index++, paymentDispute.getResolutionNote());
                ps.setString(index, paymentDispute.getResolvedBy());
            }

            private String nameOf(final Enum<?> enumeration) {
                if (enumeration != null) {
                    return enumeration.name();
                }
                return null;
            }

            private String currencyCodeFor(final Currency currency) {
                if (currency != null) {
                    return currency.getCurrencyCode();
                }
                return null;
            }

            private Timestamp asTimestamp(final DateTime dateTime) {
                if (dateTime != null) {
                    return new Timestamp(dateTime.getMillis());
                }
                return null;
            }
        });
    }

    private void verifyInitialisation() {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("Class was initialised with CGLib constructor");
        }
    }

    private DateTime asDateTime(final Date date) {
        if (date != null) {
            return new DateTime(date);
        }
        return null;
    }

    private Currency currencyFor(final String currencyCode) {
        if (currencyCode != null) {
            return Currency.getInstance(currencyCode);
        }
        return null;
    }

    private class DisputeSummaryRowMapper implements RowMapper<DisputeSummary> {
        @Override
        public DisputeSummary mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new DisputeSummary(
                    rs.getString("INTERNAL_TRANSACTION_ID"),
                    rs.getString("CASHIER_NAME"),
                    rs.getString("EXTERNAL_TRANSACTION_ID"),
                    DisputeStatus.valueOf(rs.getString("DISPUTE_STATUS")),
                    asDateTime(rs.getTimestamp("DISPUTE_TS")),
                    BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    rs.getString("DISPLAY_NAME"),
                    rs.getString("COUNTRY"),
                    currencyFor(rs.getString("CURRENCY_CODE")),
                    rs.getBigDecimal("PRICE"),
                    rs.getBigDecimal("CHIPS"),
                    rs.getString("DESCRIPTION"));
        }
    }

    private class PaymentDisputeRowMapper implements RowMapper<PaymentDispute> {
        @Override
        public PaymentDispute mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return PaymentDispute.newDispute(rs.getString("INTERNAL_TRANSACTION_ID"),
                    rs.getString("CASHIER_NAME"),
                    rs.getString("EXTERNAL_TRANSACTION_ID"),
                    BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")),
                    BigDecimals.strip(rs.getBigDecimal("ACCOUNT_ID")),
                    asDateTime(rs.getTimestamp("DISPUTE_TS")),
                    rs.getBigDecimal("PRICE"),
                    currencyFor(rs.getString("CURRENCY_CODE")),
                    rs.getBigDecimal("CHIPS"),
                    ExternalTransactionType.valueOf(rs.getString("TRANSACTION_TYPE")),
                    rs.getString("DESCRIPTION"))
                    .withGameType(rs.getString("GAME_TYPE"))
                    .withPlatform(platformFor(rs.getString("PLATFORM")))
                    .withPaymentOptionId(rs.getString("PAYMENT_OPTION_ID"))
                    .withPromotionId(rs.getLong("PROMO_ID"))
                    .withResolution(DisputeResolution.valueOfOrNull(rs.getString("RESOLUTION")),
                            asDateTime(rs.getTimestamp("RESOLUTION_TS")),
                            rs.getString("RESOLUTION_NOTE"),
                            rs.getString("RESOLVED_BY"))
                    .build();
        }

        private Platform platformFor(final String platform) {
            if (platform != null) {
                return Platform.valueOf(platform);
            }
            return null;
        }
    }
}
