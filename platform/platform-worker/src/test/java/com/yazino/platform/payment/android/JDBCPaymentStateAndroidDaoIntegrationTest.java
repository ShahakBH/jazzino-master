package com.yazino.platform.payment.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
@Transactional
public class JDBCPaymentStateAndroidDaoIntegrationTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1234);
    public static final String GAME_TYPE = "SLOTS";
    public static final String INTERNAL_TRANSACTION_ID = "internal transaction id";
    public static final String PRODUCT_ID = "product id";
    private static final Long PROMO_ID = 56L;
    public static final String GOOGLE_ORDER_NUMBER = "35425.21321";
    @Autowired
    private JdbcTemplate template;

    @Autowired
    private JDBCPaymentStateAndroidDao underTest;

    @Test
    public void shouldCreatePaymentState() {
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, null);

        assertPaymentState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREATED, null, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test
    public void shouldCreatePaymentStateWithPromoId() {
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);

        assertPaymentState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREATED, null, GAME_TYPE, PRODUCT_ID, PROMO_ID);
    }

    @Test(expected = DuplicateKeyException.class)
    public void shouldThrowExceptionIfPaymentStateExists() {
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, null);
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, null);
    }

    @Test
    public void shouldUpdatePaymentState() {
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, null);

        underTest.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITING);

        assertPaymentState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITING, null, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test(expected = EmptyResultDataAccessException.class)
    public void shouldThrowExceptionWhenUpdatingUnknowPaymentStateRecord() {
        underTest.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITING);

        assertPaymentState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITING, null, GAME_TYPE, PRODUCT_ID, null);
    }

    @Test
    public void shouldReadPaymentState() {
        underTest.createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, null);

        AndroidPaymentState androidPaymentState = underTest.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        assertThat(androidPaymentState, is(AndroidPaymentState.CREATED));
    }

    @Test
    public void readPaymentStateShouldReturnNullWhenRecordDoesNotExist() {
        AndroidPaymentState androidPaymentState = underTest.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        assertNull(androidPaymentState);
    }

    @Test
    public void shouldLoadPaymentStateDetails() {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setGoogleOrderNumber(GOOGLE_ORDER_NUMBER);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(PROMO_ID);
        expectedDetails.setState(AndroidPaymentState.CREATED);
        template.update("insert into PAYMENT_STATE_ANDROID (PLAYER_ID, STATE, INTERNAL_TRANSACTION_ID, GAME_TYPE,"
                + " PRODUCT_ID, PROMO_ID, GOOGLE_ORDER_NUMBER, UPDATED_TS) "
                + "values(?,?,?,?,?,?,?,?)"
                , expectedDetails.getPlayerId(), expectedDetails.getState().name(), expectedDetails.getInternalTransactionId(), expectedDetails.getGameType()
                , expectedDetails.getProductId(), expectedDetails.getPromoId(), expectedDetails.getGoogleOrderNumber(), new Date());

        AndroidPaymentStateDetails details = underTest.loadPaymentStateDetails(INTERNAL_TRANSACTION_ID);

        assertThat(details, is(expectedDetails));
    }

    @Test
    public void shouldLoadPaymentStateDetailsWithNullPromoId() {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setGoogleOrderNumber(GOOGLE_ORDER_NUMBER);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(null);
        expectedDetails.setState(AndroidPaymentState.CREATED);
        template.update("insert into PAYMENT_STATE_ANDROID (PLAYER_ID, STATE, INTERNAL_TRANSACTION_ID, GAME_TYPE,"
                + " PRODUCT_ID, PROMO_ID, GOOGLE_ORDER_NUMBER, UPDATED_TS) "
                + "values(?,?,?,?,?,?,?,?)"
                , expectedDetails.getPlayerId(), expectedDetails.getState().name(), expectedDetails.getInternalTransactionId(), expectedDetails.getGameType()
                , expectedDetails.getProductId(), null, expectedDetails.getGoogleOrderNumber(), new Date());

        AndroidPaymentStateDetails details = underTest.loadPaymentStateDetails(INTERNAL_TRANSACTION_ID);

        assertThat(details, is(expectedDetails));
    }

    @Test
    public void loadPaymentStateDetailsShouldReturnNullIfStateNotFound() {
        AndroidPaymentStateDetails details = underTest.loadPaymentStateDetails("unknown txn id");

        assertNull(details);
    }

    private void assertPaymentState(BigDecimal expectedPlayerId,
                                    String expectedInternalTransactionId,
                                    AndroidPaymentState expectedState,
                                    String expectedGoogleOrderNumber,
                                    String expectedGameType,
                                    String expectedProductId,
                                    Long expectedPromoId) {
        Map<String, Object> results = template.queryForMap(
                "select * from PAYMENT_STATE_ANDROID where player_id=? and internal_transaction_id=?", expectedPlayerId, expectedInternalTransactionId);
        assertThat(expectedPlayerId, is(comparesEqualTo(new BigDecimal(results.get("PLAYER_ID").toString()))));
        assertThat(expectedState.name(), is(results.get("STATE")));
        assertThat(expectedInternalTransactionId, is(results.get("INTERNAL_TRANSACTION_ID")));
        assertThat(expectedGoogleOrderNumber, is(results.get("GOOGLE_ORDER_NUMBER")));
        assertThat(expectedGameType, is(results.get("GAME_TYPE")));
        assertThat(expectedProductId, is(results.get("PRODUCT_ID")));
        if (expectedPromoId == null) {
            assertNull(results.get("PROMO_ID"));
        } else {
            assertThat(expectedPromoId, is(Long.valueOf(results.get("PROMO_ID").toString())));
        }
    }
}
