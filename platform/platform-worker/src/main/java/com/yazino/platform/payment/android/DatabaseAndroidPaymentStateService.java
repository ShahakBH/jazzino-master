package com.yazino.platform.payment.android;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.yazino.platform.payment.android.AndroidPaymentState.*;
import static java.util.Arrays.asList;

@Service("androidPaymentStateService")
public class DatabaseAndroidPaymentStateService implements AndroidPaymentStateService {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseAndroidPaymentStateService.class);

    private static final Set<AndroidPaymentState> LOCKED_STATES = new HashSet<>(asList(CREDITING, CREDITED));

    private JDBCPaymentStateAndroidDao dao;

    @Autowired
    public DatabaseAndroidPaymentStateService(JDBCPaymentStateAndroidDao dao) {
        this.dao = dao;
    }

    @Override
    public void createPurchaseRequest(BigDecimal playerId,
                                      String gameType,
                                      String internalTransactionId,
                                      String productId,
                                      Long promoId) throws AndroidPaymentStateException {
        try {
            dao.createPaymentState(playerId, gameType, internalTransactionId, productId, promoId);
        } catch (DataIntegrityViolationException e) {
            LOG.warn("Attempted to create android payment state record when one already exists, playerId={}, internalTransactionId={}"
                    , playerId, internalTransactionId);
            final AndroidPaymentState existingState = attemptRead(playerId, internalTransactionId);
            throw new AndroidPaymentStateException(e.getMessage(), e);
        } catch (DataAccessException e) {
            LOG.warn("Error while attempting to create android payment state record. Record exists, playerId={}, internalTransactionId={}"
                    , playerId, internalTransactionId, e);
            throw new AndroidPaymentStateException(e.getMessage(), e);
        }
    }

    @Override
    public void createCreditPurchaseLock(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException {
        LOG.debug("Changing state to CREDITING for playerId={}, internalTransactionId={}"
                , playerId, internalTransactionId);
        final AndroidPaymentState existingState = attemptRead(playerId, internalTransactionId);
        try {
            if (LOCKED_STATES.contains(existingState)) {
                throw new AndroidPaymentStateException("Purchase is locked (currentState=" + existingState + ", transactionId=" + internalTransactionId + ")");
            }
            dao.updateState(playerId, internalTransactionId, CREDITING);
        } catch (DataAccessException e) {
            LOG.warn("Error while attempting to create credit lock, playerId={}, internalTransactionId={}"
                    , playerId, internalTransactionId, e);
            throw new AndroidPaymentStateException(e.getMessage(), e);
        }
    }

    @Override
    public void markPurchaseAsCredited(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException {
        changePurchaseState(playerId, internalTransactionId, CREDITING, CREDITED);
    }

    @Override
    public void markPurchaseAsCancelled(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException {
        changePurchaseState(playerId, internalTransactionId, CREDITING, CANCELLED);
    }

    @Override
    public void markPurchaseAsUserCancelled(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException {
        changePurchaseState(playerId, internalTransactionId, CREATED, CANCELLED);
    }

    @Override
    public void markPurchaseAsFailed(BigDecimal playerId, String internalTransactionId) throws AndroidPaymentStateException {
        // here we don't care what the existing state
        changePurchaseState(playerId, internalTransactionId, FAILED);
    }

    @Override
    public AndroidPaymentStateDetails findPaymentStateDetailsFor(String internalTransactionId) {
        LOG.debug("loading payment state details for internalTransactionId={}", internalTransactionId);
        return dao.loadPaymentStateDetails(internalTransactionId);
    }

    private void changePurchaseState(BigDecimal playerId, String internalTransactionId, AndroidPaymentState fromState, AndroidPaymentState toState) throws AndroidPaymentStateException {
        LOG.debug("Changing state to " + toState.name() + " for playerId={}, internalTransactionId={}"
                , playerId, internalTransactionId);
        final AndroidPaymentState existingState = attemptRead(playerId, internalTransactionId);
        try {
            if (existingState != fromState) {
                throw new AndroidPaymentStateException("Invalid 'from' state (received " + fromState + ", actual is " + existingState + ")");
            }
            dao.updateState(playerId, internalTransactionId, toState);
        } catch (DataAccessException e) {
            LOG.warn("Error while attempting to change state to {} for playerId={}, internalTransactionId={}. Current state is {}."
                    , toState.name(), playerId, internalTransactionId, existingState.name(), e);
            throw new AndroidPaymentStateException(e.getMessage(), e);
        }
    }

    private void changePurchaseState(BigDecimal playerId, String internalTransactionId, AndroidPaymentState toState) throws AndroidPaymentStateException {
        LOG.debug("Changing state to " + toState.name() + " for playerId={}, internalTransactionId={}"
                , playerId, internalTransactionId);
        try {
            dao.updateState(playerId, internalTransactionId, toState);
        } catch (DataAccessException e) {
            LOG.warn("Error while attempting to change state to {} for playerId={}, internalTransactionId={}."
                    , toState.name(), playerId, internalTransactionId, e);
            throw new AndroidPaymentStateException(e.getMessage(), e);
        }
    }

    private AndroidPaymentState attemptRead(BigDecimal playerId, String internalTransactionId) {
        try {
            return dao.readState(playerId, internalTransactionId);
        } catch (DataAccessException e) {
            LOG.warn("Failed to read payment state for playerId={}, internalTransactionId={} due to {}",
                    playerId, internalTransactionId, e.getMessage());
        }
        return UNKNOWN;
    }
}
