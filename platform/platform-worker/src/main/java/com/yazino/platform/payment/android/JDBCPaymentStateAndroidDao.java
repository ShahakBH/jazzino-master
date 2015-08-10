package com.yazino.platform.payment.android;

import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Repository
public class JDBCPaymentStateAndroidDao {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCPaymentStateAndroidDao.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCPaymentStateAndroidDao(final NamedParameterJdbcTemplate template) throws DataAccessException {
        jdbcTemplate = template;
    }

    public void createPaymentState(BigDecimal playerId,
                                   String gameType,
                                   String internalTransactionId,
                                   String productId,
                                   Long promoId) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("playerId", playerId);
        parameters.put("state", AndroidPaymentState.CREATED.name());
        parameters.put("internalTransactionId", internalTransactionId);
        parameters.put("gameType", gameType);
        parameters.put("productId", productId);
        parameters.put("promoId", promoId);
        parameters.put("updateTs", new Timestamp(new DateTime().getMillis()));

        jdbcTemplate.update("insert into PAYMENT_STATE_ANDROID (PLAYER_ID, STATE, INTERNAL_TRANSACTION_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID, UPDATED_TS) "
                + "values(:playerId, :state, :internalTransactionId, :gameType, :productId, :promoId, :updateTs)"
                , parameters);
    }

    public int updateState(BigDecimal playerId, String internalTransactionId, AndroidPaymentState state) throws DataAccessException {
        LOG.debug("Updating payment state for playerId={}, internalTransactionId={}, state={}", playerId, internalTransactionId, state.name());

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("playerId", playerId);
        parameters.put("state", state.name());
        parameters.put("internalTransactionId", internalTransactionId);
        parameters.put("updateTs", new Timestamp(new DateTime().getMillis()));
        return jdbcTemplate.update("update PAYMENT_STATE_ANDROID set state=:state, updated_ts=:updateTs "
                + "where player_id=:playerId and internal_transaction_id=:internalTransactionId",
                parameters);
    }

    public AndroidPaymentState readState(BigDecimal playerId, String internalTransactionId) throws DataAccessException {
        LOG.debug("Reading android payment state for playerId={} and internalTrabsactionId={}", playerId, internalTransactionId);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("playerId", playerId);
        parameters.put("internalTransactionId", internalTransactionId);
        String androidPaymentStateAsString = null;
        try {
            androidPaymentStateAsString = jdbcTemplate.queryForObject("select state from PAYMENT_STATE_ANDROID where player_id=:playerId and internal_transaction_id=:internalTransactionId",
                    parameters, String.class);
        } catch (EmptyResultDataAccessException e) {
            // do nothing - null is returned
        }
        if (androidPaymentStateAsString == null) {
            return null;
        }
        return AndroidPaymentState.valueOf(androidPaymentStateAsString);
    }

    public AndroidPaymentStateDetails loadPaymentStateDetails(String internalTransactionId) {
        LOG.debug("looking up payment state details for internalTrabsactionId={}", internalTransactionId);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("internalTransactionId", internalTransactionId);
        try {
            return jdbcTemplate.queryForObject("select * from PAYMENT_STATE_ANDROID where internal_transaction_id=:internalTransactionId",
                    parameters, new RowMapper<AndroidPaymentStateDetails>() {
                @Override
                public AndroidPaymentStateDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
                    AndroidPaymentStateDetails details = new AndroidPaymentStateDetails();
                    details.setInternalTransactionId(rs.getString("INTERNAL_TRANSACTION_ID"));
                    details.setGameType(rs.getString("GAME_TYPE"));
                    details.setGoogleOrderNumber(rs.getString("GOOGLE_ORDER_NUMBER"));
                    details.setPlayerId(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")));
                    details.setProductId(rs.getString("PRODUCT_ID"));
                    long promoId = rs.getLong("PROMO_ID");
                    if (!rs.wasNull()) {
                        details.setPromoId(promoId);
                    }
                    details.setState(AndroidPaymentState.valueOf(rs.getString("STATE")));
                    return details;
                }
            });
        } catch (DataAccessException e) {
            // do nothing - null is returned
        }
        return null;
    }
}
