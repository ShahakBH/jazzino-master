package com.yazino.bi.operations.persistence;

import com.yazino.platform.account.ExternalTransactionStatus;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.yazino.bi.operations.view.reportbeans.PaymentTransactionData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JdbcPaymentTransactionReportDao implements PaymentTransactionReportDao {

    static final String PAYMENT_METHODS_QUERY =
            "SELECT DISTINCT CASHIER_NAME FROM EXTERNAL_TRANSACTION ORDER BY CASHIER_NAME";
    static final String PAYMENT_TXN_SELECT = "select et.player_id,"
            + "  lu.display_name playerName, "
            + "  to_char(et.message_ts, 'dd/mm/YYYY') date,"
            + "  et.currency_code,"
            + "  et.amount,"
            + "  et.internal_transaction_id,"
            + "  et.external_transaction_id,"
            + "  et.failure_reason,"
            + "  et.cashier_name,"
            + "  et.external_transaction_status,"
            + "  et.amount_chips,"
            + "  et.game_type,"
            + "  lu.country, "
            + "  etv.first_purchase_date,"
            + "  lu.reg_ts::date AS registration_date,"
            + "  ca.is_purchase "
            + "from EXTERNAL_TRANSACTION et"
            + " left join (select distinct player_id, first_purchase_date from external_transaction_mv) etv on etv.player_id =et.player_id "
            + " join LOBBY_USER lu on lu.PLAYER_ID = et.PLAYER_ID "
            + " left join cashiers ca on et.cashier_name = ca.cashier_name ";
    static final String PAYMENT_TXN_DATE_CLAUSE = " where et.message_ts >= ? and et.message_ts <= ?";
    static final String PAYMENT_TXN_STATUS_CLAUSE = " and et.external_transaction_status = ?";
    static final String PAYMENT_TXN_SUCCESSFUL_STATUS_CLAUSE = String.format(
            " and (et.external_transaction_status = '%s' or et.external_transaction_status = '%s')",
            ExternalTransactionStatus.SUCCESS,
            ExternalTransactionStatus.AUTHORISED);
    static final String PAYMENT_TXN_CURRENCY_CLAUSE = " and et.currency_code = ?";
    static final String PAYMENT_TXN_CASHIER_CLAUSE = " and et.cashier_name = ?";
    static final String PAYMENT_TXN_CASHIER_PAYMENTS_CLAUSE = " and ca.is_purchase= ?";
    static final String PAYMENT_TXN_ID_CLAUSE =
            " where (et.internal_transaction_id = ? or et.external_transaction_id = ?)";
    static final String PAYMENT_TXN_ORDER_BY = " order by et.message_ts desc limit 10000";

    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    public JdbcPaymentTransactionReportDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<String> getAvailablePaymentMethods() {
        return jdbcTemplate.query(PAYMENT_METHODS_QUERY, new RowMapper<String>() {
            @Override
            public String mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return rs.getString(1);
            }
        });
    }


    @Override
    public List<PaymentTransactionData> getPaymentTransactionData(final DateTime fromDate,
                                                                  final DateTime toDate,
                                                                  final String currencyCode,
                                                                  final String cashier,
                                                                  final String txnStatus) {
        final StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PAYMENT_TXN_SELECT);
        queryBuilder.append(PAYMENT_TXN_DATE_CLAUSE);
        final List<Object> parameters = new ArrayList<>();
        parameters.add(fromDate.toDate());
        parameters.add(toDate.plusDays(1).toDate());
        if (StringUtils.isNotBlank(txnStatus)) {
            if (SUCCESSFUL_STATUS.equals(txnStatus)) {
                queryBuilder.append(PAYMENT_TXN_SUCCESSFUL_STATUS_CLAUSE);
            } else {
                queryBuilder.append(PAYMENT_TXN_STATUS_CLAUSE);
                parameters.add(txnStatus);
            }
        }
        if (StringUtils.isNotBlank(currencyCode)) {
            queryBuilder.append(PAYMENT_TXN_CURRENCY_CLAUSE);
            parameters.add(currencyCode);
        }
        if (StringUtils.isNotBlank(cashier)) {
            if (purchasesOnly(cashier)) {
                queryBuilder.append(PAYMENT_TXN_CASHIER_PAYMENTS_CLAUSE);
                parameters.add(true);
            } else if (offersOnly(cashier)) {
                queryBuilder.append(PAYMENT_TXN_CASHIER_PAYMENTS_CLAUSE);
                parameters.add(false);
            } else {
                queryBuilder.append(PAYMENT_TXN_CASHIER_CLAUSE);
                parameters.add(cashier);
            }
        }
        queryBuilder.append(PAYMENT_TXN_ORDER_BY);
        return jdbcTemplate.query(queryBuilder.toString(), parameters.toArray(),
                new PaymentTransactionDataRowMapper());
    }

    private boolean offersOnly(final String cashier) {
        return PaymentTransactionReportDao.OFFER_TRANSACTIONS.equals(cashier);
    }

    private boolean purchasesOnly(final String cashier) {
        return PaymentTransactionReportDao.PURCHASE_TRANSACTIONS.equals(cashier);
    }

    @Override
    public List<PaymentTransactionData> getPaymentTransactionData(final String transactionId) {
        final StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append(PAYMENT_TXN_SELECT).append(PAYMENT_TXN_ID_CLAUSE);
        return jdbcTemplate.query(queryBuilder.toString(), new Object[]{transactionId, transactionId},
                new PaymentTransactionDataRowMapper());
    }

    private static class PaymentTransactionDataRowMapper implements RowMapper<PaymentTransactionData> {
        @Override
        public PaymentTransactionData mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final PaymentTransactionData bean = new PaymentTransactionData();
            bean.setAmount(resultSet.getDouble("amount"));
            bean.setCurrencyCode(resultSet.getString("currency_code"));
            bean.setDate(resultSet.getString("date"));
            bean.setDetails(resultSet.getString("failure_reason"));
            bean.setExternalId(resultSet.getString("external_transaction_id"));
            bean.setInternalId(resultSet.getString("internal_transaction_id"));
            bean.setPlayerId(resultSet.getLong("player_id"));
            bean.setPlayerName(resultSet.getString("playerName"));
            bean.setTxnStatus(resultSet.getString("external_transaction_status"));
            bean.setCashier(resultSet.getString("cashier_name"));
            bean.setAmountChips(resultSet.getLong("amount_chips"));
            bean.setGameType(resultSet.getString("game_type"));
            bean.setRegCountry(resultSet.getString("country"));
            bean.setRegistrationDate(resultSet.getString("registration_date"));
            bean.setFirstPurchaseDate(resultSet.getString("first_purchase_date"));
            return bean;
        }
    }
}
