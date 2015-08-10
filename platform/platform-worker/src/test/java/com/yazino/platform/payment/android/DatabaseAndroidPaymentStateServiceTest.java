package com.yazino.platform.payment.android;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static com.yazino.platform.payment.android.AndroidPaymentState.*;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class DatabaseAndroidPaymentStateServiceTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "sample product id";
    public static final long PROMO_ID = 7654L;
    public static final String INTERNAL_TRANSACTION_ID = "sample internal transaction id";
    public static final String GOOGLE_ORDER_NUMBER = "sample order number";

    private final JDBCPaymentStateAndroidDao dao = Mockito.mock(JDBCPaymentStateAndroidDao.class);
    private final AndroidPaymentStateService underTest = new DatabaseAndroidPaymentStateService(dao);

    @Test
    public void shouldUseDaoToCreatePaymentState() throws Exception {
        underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);

        verify(dao).createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
    }

    @Test
    public void createPaymentStateShouldThrowPaymentStateExceptionWhenPaymentStateRecordExists() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        doThrow(new DataIntegrityViolationException("record exists"))
                .when(dao)
                .createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
        try {
            underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("record exists"));
        }
    }

    @Test
    public void createPaymentStateShouldThrowExceptionWithUnknownStateWhenDataAccessExceptionIsThrown() throws Exception {
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .createPaymentState(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
        try {
            underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void shouldCreateCreditPurchaseLock() throws AndroidPaymentStateException {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        when(dao.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CREDITING)).thenReturn(1);

        underTest.createCreditPurchaseLock(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        verify(dao).updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CREDITING);
    }

    @Test
    public void createCreditPurchaseLockShouldThrowIfLocked() throws AndroidPaymentStateException {
        AndroidPaymentState[] lockedStates = {CREDITING, CREDITED};
        for (AndroidPaymentState state : lockedStates) {
            when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(state);

            try {
                underTest.createCreditPurchaseLock(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            } catch (AndroidPaymentStateException e) {
                assertTrue(e.getMessage().matches("Purchase is locked \\(currentState=[^,]+, transactionId=[^)]+\\)"));
            }
        }
    }

    @Test
    public void createCreditPurchaseLockShouldNotThrowIfNotLocked() throws AndroidPaymentStateException {
        AndroidPaymentState[] unlockedStates = {CREATED, CANCELLED, FAILED, UNKNOWN};
        for (AndroidPaymentState state : unlockedStates) {
            when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(state);

            underTest.createCreditPurchaseLock(PLAYER_ID, INTERNAL_TRANSACTION_ID);
        }
    }

    @Test
    public void createCreditPurchaseLockShouldThrowExceptionWhenDaoUpdateFails() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITING);
        try {
            underTest.createCreditPurchaseLock(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void shouldUpdatePaymentStateToCREDITED() throws AndroidPaymentStateException {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        when(dao.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CREDITED)).thenReturn(1);

        underTest.markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        verify(dao).updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CREDITED);
    }

    @Test
    public void markPurchaseAsCreditedShouldThrowExceptionWhenDaoUpdateFails() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, AndroidPaymentState.CREDITED);
        try {
            underTest.markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void markPurchaseAsCreditedShouldThrowExceptionWhenInitialStateISNOtCREDITING() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        try {
            underTest.markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("Invalid 'from' state (received CREDITING, actual is CREATED)"));
        }
    }

    @Test
    public void shouldUpdatePaymentStateToFAILED() throws AndroidPaymentStateException {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        when(dao.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, FAILED)).thenReturn(1);

        underTest.markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        verify(dao).updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, FAILED);
    }

    @Test
    public void markPurchaseAsFailedShouldThrowExceptionWhenDaoUpdateFails() throws Exception {
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, FAILED);
        try {
            underTest.markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void shouldUpdatePaymentStateToCancelled() throws AndroidPaymentStateException {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        when(dao.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, FAILED)).thenReturn(1);

        underTest.markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        verify(dao).updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CANCELLED);
    }

    @Test
    public void markPurchaseAsCancelledShouldThrowExceptionWhenDaoUpdateFails() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREDITING);
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CANCELLED);
        try {
            underTest.markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void markPurchaseAsCancelledShouldThrowExceptionWhenInitialStateISNotCREDITING() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        try {
            underTest.markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("Invalid 'from' state (received CREDITING, actual is CREATED)"));
        }
    }

    @Test
    public void markPurchaseAsUserCancelledShouldUpdatePaymentStateToCancelled() throws AndroidPaymentStateException {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        when(dao.updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, FAILED)).thenReturn(1);

        underTest.markPurchaseAsUserCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        verify(dao).updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CANCELLED);
    }

    @Test
    public void markPurchaseAsUserCancelledShouldThrowExceptionWhenDaoUpdateFails() throws Exception {
        when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(CREATED);
        doThrow(new GenericDataAccessException("a data access exception"))
                .when(dao)
                .updateState(PLAYER_ID, INTERNAL_TRANSACTION_ID, CANCELLED);
        try {
            underTest.markPurchaseAsUserCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
            fail("expected AndroidPaymentStateException");
        } catch (AndroidPaymentStateException e) {
            assertThat(e.getMessage(), is("a data access exception"));
        }
    }

    @Test
    public void markPurchaseAsCancelledShouldThrowExceptionWhenInitialStateIsNotCREATED() throws Exception {
        for (AndroidPaymentState state: AndroidPaymentState.values()) {
            if (state == CREATED) {
                continue;
            }
            when(dao.readState(PLAYER_ID, INTERNAL_TRANSACTION_ID)).thenReturn(state);
            try {
                underTest.markPurchaseAsUserCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
                fail("expected AndroidPaymentStateException");
            } catch (AndroidPaymentStateException e) {
                assertThat(e.getMessage(), is("Invalid 'from' state (received CREATED, actual is " + state.name() +")"));
            }
        }
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
        when(dao.loadPaymentStateDetails(INTERNAL_TRANSACTION_ID)).thenReturn(expectedDetails);

        AndroidPaymentStateDetails actualDetails = underTest.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID);
        assertThat(actualDetails, is(actualDetails));
    }

    class GenericDataAccessException extends DataAccessException {
        public GenericDataAccessException(String msg) {
            super(msg);
        }
    }


}
