package com.yazino.platform.persistence.account;

import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.ObjectUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Currency;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("accountStatementDao")
public class JDBCAccountStatementDAO {

    private static final String SQL_INSERT = "INSERT INTO ACCOUNT_STATEMENT "
            + "(INTERNAL_TRANSACTION_ID,ACCOUNT_ID,CASHIER_NAME,GAME_TYPE,TRANSACTION_STATUS,"
            + "PURCHASE_TIMESTAMP,PURCHASE_CURRENCY,PURCHASE_AMOUNT,CHIPS_AMOUNT) "
            + "VALUES (?,?,?,?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE "
            + "ACCOUNT_ID=VALUES(ACCOUNT_ID),CASHIER_NAME=VALUES(CASHIER_NAME),GAME_TYPE=VALUES(GAME_TYPE),"
            + "TRANSACTION_STATUS=VALUES(TRANSACTION_STATUS),PURCHASE_TIMESTAMP=VALUES(PURCHASE_TIMESTAMP),"
            + "PURCHASE_CURRENCY=VALUES(PURCHASE_CURRENCY),PURCHASE_AMOUNT=VALUES(PURCHASE_AMOUNT),"
            + "CHIPS_AMOUNT=VALUES(CHIPS_AMOUNT)";
    private static final String SQL_SELECT = "SELECT * FROM ACCOUNT_STATEMENT WHERE INTERNAL_TRANSACTION_ID=?";
    private static final String SELECT_BY_ACC_AND_CASHIER = "SELECT * FROM ACCOUNT_STATEMENT WHERE ACCOUNT_ID=? AND CASHIER_NAME=? AND PURCHASE_TIMESTAMP > CURDATE()";

    private final AccountStatementRowMapper mapper = new AccountStatementRowMapper();
    private final JdbcOperations template;

    @Autowired
    public JDBCAccountStatementDAO(final JdbcOperations template) {
        notNull(template, "template may not be null");

        this.template = template;
    }

    public void save(final AccountStatement statement) {
        notNull(statement, "statement may not be null");

        template.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                final PreparedStatement stmt = con.prepareStatement(SQL_INSERT);

                int index = 1;
                stmt.setString(index++, statement.getInternalTransactionId());
                stmt.setBigDecimal(index++, statement.getAccountId());
                stmt.setString(index++, statement.getCashierName());
                stmt.setString(index++, statement.getGameType());
                stmt.setString(index++, ObjectUtils.toString(statement.getTransactionStatus(), null));
                stmt.setTimestamp(index++, asTimestamp(statement.getTimestamp()));
                stmt.setString(index++, asString(statement.getPurchaseCurrency()));
                stmt.setBigDecimal(index++, statement.getPurchaseAmount());
                stmt.setBigDecimal(index, statement.getChipsAmount());

                return stmt;
            }
        });
    }

    public AccountStatement findByInternalTransactionId(final String internalTransactionId) {
        notNull(internalTransactionId, "internalTransactionId may not be null");

        final List<AccountStatement> results = template.query(SQL_SELECT, mapper, internalTransactionId);
        if (!results.isEmpty()) {
            return results.get(0);
        }
        return null;
    }

    private String asString(final Currency purchaseCurrency) {
        if (purchaseCurrency != null) {
            return purchaseCurrency.getCurrencyCode();
        }
        return null;
    }

    private Timestamp asTimestamp(final DateTime timestamp) {
        if (timestamp != null) {
            return new Timestamp(timestamp.getMillis());
        }
        return null;
    }

    private ExternalTransactionStatus asTransactionStatus(final String transactionStatus) {
        if (transactionStatus != null) {
            return ExternalTransactionStatus.valueOf(transactionStatus);
        }
        return null;
    }

    private Currency asCurrency(final String currencyCode) {
        if (currencyCode != null) {
            return Currency.getInstance(currencyCode);
        }
        return null;
    }

    private DateTime asDateTime(final Timestamp timestamp) {
        if (timestamp != null) {
            return new DateTime(timestamp.getTime());
        }
        return null;
    }

    public List<AccountStatement> findBy(final BigDecimal accountId, final String cashierName) {
        notNull(accountId, "accountId may not be null");
        notNull(cashierName, "cashierName may not be null");

        final List<AccountStatement> results = template.query(SELECT_BY_ACC_AND_CASHIER, mapper, accountId, cashierName);
        if (!results.isEmpty()) {
            return results;
        }
        return null;
    }

    private class AccountStatementRowMapper implements RowMapper<AccountStatement> {
        @Override
        public AccountStatement mapRow(final ResultSet rs,
                                       final int rowNum) throws SQLException {
            return AccountStatement.forAccount(BigDecimals.strip(rs.getBigDecimal("ACCOUNT_ID")))
                    .withInternalTransactionId(rs.getString("INTERNAL_TRANSACTION_ID"))
                    .withCashierName(rs.getString("CASHIER_NAME"))
                    .withGameType(rs.getString("GAME_TYPE"))
                    .withTransactionStatus(asTransactionStatus(rs.getString("TRANSACTION_STATUS")))
                    .withTimestamp(asDateTime(rs.getTimestamp("PURCHASE_TIMESTAMP")))
                    .withPurchaseCurrency(asCurrency(rs.getString("PURCHASE_CURRENCY")))
                    .withPurchaseAmount(rs.getBigDecimal("PURCHASE_AMOUNT"))
                    .withChipsAmount(rs.getBigDecimal("CHIPS_AMOUNT"))
                    .asStatement();
        }
    }

}
