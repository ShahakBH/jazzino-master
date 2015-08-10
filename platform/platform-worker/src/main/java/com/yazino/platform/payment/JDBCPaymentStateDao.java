package com.yazino.platform.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.*;

@Repository("paymentStateDao")
public class JDBCPaymentStateDao {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCPaymentStateDao.class);

    static final String TABLE_NAME = "PAYMENT_STATE";
    static final String CASHIER_NAME = "CASHIER_NAME";
    static final String EXTERNAL_TRANSACTION_ID = "EXTERNAL_TRANSACTION_ID";
    static final String STATE = "STATE";
    static final String UPDATED_TS = "UPDATED_TS";

    static final String RETRIEVE_STATE = String.format("select * from %s where %s=? and %s=?",
            TABLE_NAME, CASHIER_NAME, EXTERNAL_TRANSACTION_ID);
    static final String INSERT_STATE = String.format("insert into %s(%s, %s, %s, %s) values (?, ?, ?, ?)",
            TABLE_NAME, CASHIER_NAME, EXTERNAL_TRANSACTION_ID, STATE, UPDATED_TS);
    static final String UPDATE_STATE = String.format("%s on duplicate key update %s=?, %s=?",
            INSERT_STATE, STATE, UPDATED_TS);

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCPaymentStateDao(@Qualifier("jdbcTemplate") final JdbcTemplate template) {
        notNull(template);
        jdbcTemplate = template;
    }

    public int insertState(final String cashierName,
                           final String externalTransactionId,
                           final PaymentState state) throws DataAccessException {
        validateParameters(cashierName, externalTransactionId, state);
        final Date date = new Date();
        final String stateName = state.name();
        LOG.debug("Executing [{}] using parameters {}={}, {}={}, {}={}, {}={}",
                new Object[]{INSERT_STATE, CASHIER_NAME, cashierName, EXTERNAL_TRANSACTION_ID,
                        externalTransactionId, STATE, stateName, UPDATED_TS, date});
        return jdbcTemplate.update(INSERT_STATE, cashierName, externalTransactionId, state.name(), new Date());
    }

    public int updateState(final String cashierName,
                           final String externalTransactionId,
                           final PaymentState state) throws DataAccessException {
        validateParameters(cashierName, externalTransactionId, state);
        final Date date = new Date();
        final String stateName = state.name();
        LOG.debug("Executing [{}] using parameters {}={}, {}={}, {}={}, {}={}",
                new Object[]{UPDATE_STATE, CASHIER_NAME, cashierName, EXTERNAL_TRANSACTION_ID,
                        externalTransactionId, STATE, stateName, UPDATED_TS, date});
        return jdbcTemplate.update(UPDATE_STATE, cashierName, externalTransactionId, stateName, date, stateName, date);
    }

    public PaymentState readState(final String cashierName,
                                  final String externalTransactionId) throws DataAccessException {
        validateParameters(cashierName, externalTransactionId, PaymentState.Started);
        LOG.debug("Reading using parameters {}={}, {}={}",
                new Object[]{CASHIER_NAME, cashierName, EXTERNAL_TRANSACTION_ID, externalTransactionId});
        final List<Map<String, Object>> results = jdbcTemplate.queryForList(
                RETRIEVE_STATE, cashierName, externalTransactionId);
        if (results.size() == 0) {
            return null;
        }
        if (results.size() > 1) {
            LOG.warn("Read {} results {}, expected 1", results.size(), results);
        }
        final Map<String, Object> result = results.get(0);
        final String state = (String) result.get(STATE);
        if (state == null) {
            return null;
        }
        return PaymentState.valueOf(state);
    }

    private static void validateParameters(final String cashierName,
                                           final String externalTransactionId,
                                           final PaymentState state) {
        notBlank(cashierName);
        notBlank(externalTransactionId);
        notNull(state);
        isTrue(state != PaymentState.Unknown, "Invalid PaymentState (Unknown)");
    }

}
