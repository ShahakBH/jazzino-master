package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.ExternalTransaction;
import com.yazino.platform.util.BigDecimals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class PostgresExternalTransactionDAO extends PostgresDWDAO<ExternalTransaction> {
    private static final String SQL_FIND_PLAYER_ID_BY_TX_ID
            = "SELECT PLAYER_ID FROM EXTERNAL_TRANSACTION WHERE INTERNAL_TRANSACTION_ID=? LIMIT 1";

    private final JdbcTemplate jdbcTemplate;

    PostgresExternalTransactionDAO() {
        // cglib constructor
        super(null);
        this.jdbcTemplate = null;
    }

    @Autowired
    public PostgresExternalTransactionDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected String[] getBatchUpdates(final List<ExternalTransaction> events) {
        return new String[]{createInsertStatementFor(events)};
    }

    public BigDecimal findPlayerIdFor(final String internalTransactionId) {
        notNull(internalTransactionId, "internalTransactionId may not be null");

        return jdbcTemplate.query(SQL_FIND_PLAYER_ID_BY_TX_ID, new ResultSetExtractor<BigDecimal>() {
            @Override
            public BigDecimal extractData(final ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    return BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
                }
                return null;
            }
        }, internalTransactionId);
    }

    private String createInsertStatementFor(final List<ExternalTransaction> externalTransactions) {
        // The PostgreSQL 8 driver doesn't appear to optimise batched INSERTS in the same way MySQL does. Yay.

        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("EXTERNAL_TRANSACTION",
                "ACCOUNT_ID", "INTERNAL_TRANSACTION_ID", "EXTERNAL_TRANSACTION_ID",
                "MESSAGE", "MESSAGE_TS", "CURRENCY_CODE", "AMOUNT", "AMOUNT_CHIPS",
                "CREDIT_CARD_NUMBER", "CASHIER_NAME", "EXTERNAL_TRANSACTION_STATUS",
                "TRANSACTION_TYPE", "GAME_TYPE", "FAILURE_REASON", "PLAYER_ID", "PROMO_ID", "PLATFORM",
                "PAYMENT_OPTION_ID", "BASE_CURRENCY_CODE", "BASE_CURRENCY_AMOUNT", "EXCHANGE_RATE");
        for (ExternalTransaction externalTransaction : externalTransactions) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(externalTransaction.getAccountId()),
                    sqlString(externalTransaction.getInternalTransactionId()),
                    sqlString(externalTransaction.getExternalTransactionId()),
                    sqlString(externalTransaction.getCreditCardObscuredMessage()),
                    sqlTimestamp(externalTransaction.getMessageTimeStamp()),
                    sqlString(externalTransaction.getCurrency()),
                    sqlBigDecimal(externalTransaction.getAmountCash()),
                    sqlBigDecimal(externalTransaction.getAmountChips()),
                    sqlString(externalTransaction.getObscuredCreditCardNumber()),
                    sqlString(externalTransaction.getCashierName()),
                    sqlString(externalTransaction.getExternalTransactionStatus()),
                    sqlString(externalTransaction.getTransactionLogType()),
                    sqlString(externalTransaction.getGameType()),
                    sqlString(externalTransaction.getFailureReason()),
                    sqlBigDecimal(externalTransaction.getPlayerId()),
                    sqlLong(externalTransaction.getPromoId()),
                    sqlString(externalTransaction.getPlatform().name()),
                    sqlString(externalTransaction.getPaymentOptionId()),
                    sqlString(externalTransaction.getBaseCurrency()),
                    sqlBigDecimal(externalTransaction.getBaseCurrencyAmount()),
                    sqlBigDecimal(externalTransaction.getExchangeRate()));
        }

        return insertBuilder.toSql();
    }

}
