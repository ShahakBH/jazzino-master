package com.yazino.platform.persistence.account;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.payment.PendingSettlement;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCPaymentSettlementDAO {
    private static final String SELECT_PENDING_SETTLEMENTS = "SELECT * FROM PAYMENT_SETTLEMENT WHERE TRANSACTION_TS + interval %d hour < NOW()";
    private static final String SELECT_SETTLEMENT = "SELECT * FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?";
    private static final String SELECT_PENDING_SETTLEMENTS_BY_PLAYER = "SELECT * FROM PAYMENT_SETTLEMENT WHERE PLAYER_ID=?";
    private static final String DELETE_SETTLEMENT = "DELETE FROM PAYMENT_SETTLEMENT WHERE INTERNAL_TRANSACTION_ID=?";
    private static final String INSERT_SETTLEMENT = "INSERT INTO PAYMENT_SETTLEMENT "
            + " (INTERNAL_TRANSACTION_ID,EXTERNAL_TRANSACTION_ID,PLAYER_ID,ACCOUNT_ID,CASHIER_NAME,TRANSACTION_TS,ACCOUNT_NUMBER,"
            + " PRICE,CURRENCY_CODE,CHIPS,TRANSACTION_TYPE,GAME_TYPE,PLATFORM,PAYMENT_OPTION_ID,PROMO_ID,BASE_CURRENCY_AMOUNT,"
            + " BASE_CURRENCY_CODE,EXCHANGE_RATE)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SELECT_SUMMARY
            = "SELECT SQL_NO_CACHE SQL_CALC_FOUND_ROWS "
            + "    ps.transaction_ts,"
            + "    ps.internal_transaction_id,"
            + "    ps.external_transaction_id,"
            + "    ps.player_id,"
            + "    lu.display_name,"
            + "    lu.country,"
            + "    ps.cashier_name,"
            + "    ps.currency_code,"
            + "    ps.price,"
            + "    ps.chips,"
            + "    ps.base_currency_amount,"
            + "    ps.base_currency_code "
            + "FROM PAYMENT_SETTLEMENT ps "
            + "LEFT JOIN LOBBY_USER lu ON ps.PLAYER_ID = lu.PLAYER_ID "
            + "ORDER BY ps.internal_transaction_id "
            + "LIMIT ? OFFSET ?";
    private static final String SQL_FOUND_ROWS = "SELECT FOUND_ROWS()";

    private final PaymentSettlementRowMapper paymentSettlementRowMapper = new PaymentSettlementRowMapper();
    private final PendingSettlementRowMapper pendingSettlementRowMapper = new PendingSettlementRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public JDBCPaymentSettlementDAO() {
        this.jdbcTemplate = null;
    }

    @Autowired
    public JDBCPaymentSettlementDAO(final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(final PaymentSettlement paymentSettlement) {
        notNull(paymentSettlement, "paymentSettlement may not be null");
        verifyInitialisation();

        jdbcTemplate.update(INSERT_SETTLEMENT, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement ps) throws SQLException {
                int index = 1;
                ps.setString(index++, paymentSettlement.getInternalTransactionId());
                ps.setString(index++, paymentSettlement.getExternalTransactionId());
                ps.setBigDecimal(index++, paymentSettlement.getPlayerId());
                ps.setBigDecimal(index++, paymentSettlement.getAccountId());
                ps.setString(index++, paymentSettlement.getCashierName());
                ps.setTimestamp(index++, asTimestamp(paymentSettlement.getTimestamp()));
                ps.setString(index++, paymentSettlement.getAccountNumber());
                ps.setBigDecimal(index++, paymentSettlement.getPrice());
                ps.setString(index++, currencyCodeFor(paymentSettlement.getCurrency()));
                ps.setBigDecimal(index++, paymentSettlement.getChips());
                ps.setString(index++, paymentSettlement.getTransactionType().name());
                ps.setString(index++, paymentSettlement.getGameType());
                ps.setString(index++, nameOf(paymentSettlement.getPlatform()));
                ps.setString(index++, paymentSettlement.getPaymentOptionId());
                if (paymentSettlement.getPromotionId() != null) {
                    ps.setLong(index++, paymentSettlement.getPromotionId());
                } else {
                    ps.setNull(index++, Types.INTEGER);
                }
                ps.setBigDecimal(index++, paymentSettlement.getBaseCurrencyAmount());
                ps.setString(index++, currencyCodeFor(paymentSettlement.getBaseCurrency()));
                ps.setBigDecimal(index, paymentSettlement.getExchangeRate());
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

    public void deleteById(final String internalTransactionId) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        verifyInitialisation();

        jdbcTemplate.update(DELETE_SETTLEMENT, internalTransactionId);
    }

    public Optional<PaymentSettlement> findById(final String internalTransactionId) {
        notNull(internalTransactionId, "internalTransactionId may not be null");
        verifyInitialisation();

        final List<PaymentSettlement> results = jdbcTemplate.query(SELECT_SETTLEMENT, paymentSettlementRowMapper, internalTransactionId);
        if (results != null && !results.isEmpty()) {
            return Optional.fromNullable(results.get(0));
        }
        return Optional.absent();
    }

    public Set<PaymentSettlement> findPendingSettlements(final long settlementDelayInHours) {
        verifyInitialisation();

        final List<PaymentSettlement> pendingSettlements = jdbcTemplate.query(
                format(SELECT_PENDING_SETTLEMENTS, settlementDelayInHours), paymentSettlementRowMapper);
        if (pendingSettlements != null) {
            return new HashSet<>(pendingSettlements);
        }
        return Collections.emptySet();
    }

    public Set<PaymentSettlement> findByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");
        verifyInitialisation();

        final List<PaymentSettlement> pendingSettlements = jdbcTemplate.query(SELECT_PENDING_SETTLEMENTS_BY_PLAYER, paymentSettlementRowMapper, playerId);
        if (pendingSettlements != null) {
            return new HashSet<>(pendingSettlements);
        }
        return Collections.emptySet();
    }

    @Transactional(readOnly = true)
    public PagedData<PendingSettlement> findSummarisedPendingSettlements(final int page,
                                                                         final int pageSize) {
        verifyInitialisation();

        final List<PendingSettlement> searchResults = jdbcTemplate.query(
                SELECT_SUMMARY, pendingSettlementRowMapper, pageSize, page * pageSize);
        final int totalRows = jdbcTemplate.queryForInt(SQL_FOUND_ROWS);
        return new PagedData<>(page * pageSize, searchResults.size(), totalRows, searchResults);
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

    private class PendingSettlementRowMapper implements RowMapper<PendingSettlement> {
        @Override
        public PendingSettlement mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new PendingSettlement(asDateTime(rs.getTimestamp("TRANSACTION_TS")),
                    rs.getString("INTERNAL_TRANSACTION_ID"),
                    rs.getString("EXTERNAL_TRANSACTION_ID"),
                    rs.getBigDecimal("PLAYER_ID"),
                    rs.getString("DISPLAY_NAME"),
                    rs.getString("COUNTRY"),
                    rs.getString("CASHIER_NAME"),
                    currencyFor(rs.getString("CURRENCY_CODE")),
                    rs.getBigDecimal("PRICE"),
                    rs.getBigDecimal("CHIPS"),
                    currencyFor(rs.getString("BASE_CURRENCY_CODE")),
                    rs.getBigDecimal("BASE_CURRENCY_AMOUNT"));
        }
    }

    private class PaymentSettlementRowMapper implements RowMapper<PaymentSettlement> {
        @Override
        public PaymentSettlement mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return PaymentSettlement.newSettlement(rs.getString("INTERNAL_TRANSACTION_ID"),
                    rs.getString("EXTERNAL_TRANSACTION_ID"),
                    rs.getBigDecimal("PLAYER_ID"),
                    rs.getBigDecimal("ACCOUNT_ID"),
                    rs.getString("CASHIER_NAME"),
                    asDateTime(rs.getTimestamp("TRANSACTION_TS")),
                    rs.getString("ACCOUNT_NUMBER"),
                    rs.getBigDecimal("PRICE"),
                    currencyFor(rs.getString("CURRENCY_CODE")),
                    rs.getBigDecimal("CHIPS"),
                    ExternalTransactionType.valueOf(rs.getString("TRANSACTION_TYPE")))
                    .withGameType(rs.getString("GAME_TYPE"))
                    .withPlatform(platformFor(rs.getString("PLATFORM")))
                    .withPaymentOptionId(rs.getString("PAYMENT_OPTION_ID"))
                    .withPromotionId(rs.getLong("PROMO_ID"))
                    .withBaseCurrencyAmount(rs.getBigDecimal("BASE_CURRENCY_AMOUNT"))
                    .withBaseCurrency(currencyFor(rs.getString("BASE_CURRENCY_CODE")))
                    .withExchangeRate(rs.getBigDecimal("EXCHANGE_RATE"))
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
