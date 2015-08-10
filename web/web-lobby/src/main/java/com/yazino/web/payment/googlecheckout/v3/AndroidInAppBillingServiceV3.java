package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.android.AndroidPaymentState;
import com.yazino.platform.payment.android.AndroidPaymentStateDetails;
import com.yazino.platform.payment.android.AndroidPaymentStateException;
import com.yazino.platform.payment.android.AndroidPaymentStateService;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.googlecheckout.AndroidInAppOrderSecurity;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.web.payment.PurchaseStatus.*;

@Service
public class AndroidInAppBillingServiceV3 {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidInAppBillingServiceV3.class);

    private AndroidPromotionServiceV3 promotionService;
    private JavaUUIDSource uuidSource;
    private AndroidPaymentStateService paymentStateService;
    private WalletService walletService;
    private PlayerService playerService;
    private ChipBundleResolver chipBundleResolver;
    private AndroidInAppOrderSecurity security;
    private CreditPurchaseOperation creditPurchaseOperation;

    @Autowired
    public AndroidInAppBillingServiceV3(AndroidPromotionServiceV3 promotionService,
                                        JavaUUIDSource uuidSource,
                                        AndroidPaymentStateService paymentStateService,
                                        WalletService walletService,
                                        PlayerService playerService,
                                        ChipBundleResolver chipBundleResolver,
                                        AndroidInAppOrderSecurity security,
                                        CreditPurchaseOperation creditPurchaseOperation) {
        this.promotionService = promotionService;
        this.uuidSource = uuidSource;
        this.paymentStateService = paymentStateService;
        this.walletService = walletService;
        this.playerService = playerService;
        this.chipBundleResolver = chipBundleResolver;
        this.security = security;
        this.creditPurchaseOperation = creditPurchaseOperation;
    }

    /// TODO refactor
    public GooglePurchase createPurchaseRequest(BigDecimal playerId, String gameType, String productId, Long promoId) throws PurchaseException {
        DateTime requestTime = new DateTime();
        GooglePurchase purchaseRequest = new GooglePurchase();
        AndroidStoreProducts products = promotionService.getAvailableProducts(playerId, Platform.ANDROID, gameType);

        if (promoId != null && !promoId.equals(products.getPromoId())) {
            LOG.error("Failed to create purchase request. promotion id is invalid or has expired, product: {}, promoId={}"
                    , productId, promoId);
            purchaseRequest.setStatus(STALE_PROMOTION);
            purchaseRequest.setErrorMessage("Stale promotion (expired or unknown)");
            return purchaseRequest;
        }

        ChipBundle chipBundleRequested = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        if (chipBundleRequested == null) {
            LOG.error("Failed to create purchase request. No chip bundle found for product: {}", productId);
            purchaseRequest.setStatus(FAILED);
            purchaseRequest.setErrorMessage("Unknown product id");
            return purchaseRequest;
        }

        // TODO use TransactionIdGenerator instead
        String internalTransactionId = uuidSource.getNewUUID();
        purchaseRequest.setPurchaseId(internalTransactionId);
        try {
            paymentStateService.createPurchaseRequest(playerId, gameType, internalTransactionId, productId, promoId);
        } catch (AndroidPaymentStateException e) {
            LOG.error("Failed to create purchase request record. playerId={}, productId={}", playerId, productId, e);
            purchaseRequest.setStatus(FAILED);
            purchaseRequest.setErrorMessage("Could not create new purchase (" + e.getMessage() + ")");
            return purchaseRequest;
        }

        purchaseRequest.setStatus(CREATED);
        logExternalTransactionRequest(playerId, gameType, productId, promoId, requestTime, purchaseRequest, chipBundleRequested);

        return purchaseRequest;
    }

    public GooglePurchase creditPurchase(String gameType,
                                         String orderData,
                                         String signature,
                                         final Partner partnerId) throws PurchaseException {
        LOG.info("creditPurchase:  gameType {}, orderData {}, signature {}", gameType, orderData, signature);

        try {
            validateCreditPurchaseArgs(gameType, orderData, signature);

            verifyOrder(gameType, orderData, signature, partnerId);
            Order order = deserialiseOrder(orderData);
            DeveloperPayload developerPayload = deserialiseDeveloperPayload(order.getDeveloperPayload());
            validateDeveloperPayload(developerPayload);

            ChipBundle bundle = findChipBundle(gameType, order.getProductId());
            AndroidPaymentStateDetails paymentRecord = findPaymentStateDetailsFor(developerPayload.getPurchaseId());
            switch (order.getPurchaseState()) {
                case PURCHASED:
                    if (paymentRecord.getState() != AndroidPaymentState.CREDITED) {
                        creditPurchaseOperation.creditPurchase(paymentRecord, order, bundle);
                    }
                    return asSuccessfulPurchase(bundle);
                case CANCELED:
                    if (paymentRecord.getState() != AndroidPaymentState.CANCELLED) {
                        try {
                            paymentStateService.markPurchaseAsCancelled(paymentRecord.getPlayerId(), paymentRecord.getInternalTransactionId());
                        } catch (AndroidPaymentStateException e) {
                            throw new PurchaseException(FAILED, false, "Failed to update payment state for orderId=" + order.getOrderId(),
                                    String.format("Failed to update payment state from %s to %s", paymentRecord.getState(), order.getPurchaseState()), e);
                        }
                    }
                    throw new PurchaseException(CANCELLED, true, "Not crediting cancelled purchase (orderId=" + order.getOrderId() + "). Purchase state is NOT PURCHASED");
                default:
                    if (paymentRecord.getState() != AndroidPaymentState.FAILED) {
                        try {
                            paymentStateService.markPurchaseAsFailed(paymentRecord.getPlayerId(), paymentRecord.getInternalTransactionId());
                        } catch (AndroidPaymentStateException e) {
                            throw new PurchaseException(FAILED, false, "Failed to update payment state for orderId=" + order.getOrderId(),
                                    String.format("Failed to update payment state from %s to %s", paymentRecord.getState(), order.getPurchaseState(), e));
                        }
                    }
                    throw new PurchaseException(FAILED, true, "Not crediting failed purchase (orderId=" + order.getOrderId() + "). Purchase state is NOT PURCHASED");
            }
        } catch (PurchaseException e) {
            logCreditPurchaseException(gameType, orderData, signature, e);
            throw e;
        }
    }

    private void validateCreditPurchaseArgs(String gameType, String orderData, String signature) throws PurchaseException {
        try {
            Validate.notNull(gameType, "gameType");
            Validate.notNull(orderData, "orderData");
            Validate.notNull(signature, "signature");
        } catch (Exception e) {
            throw new PurchaseException(FAILED, false, "invalid args", "missing " + e.getMessage());
        }
    }

    private void verifyOrder(String gameType, String orderData, String signature, final Partner partnerId) throws PurchaseException {
        final boolean verified = security.verify(gameType, orderData, signature, partnerId);
        if (!verified) {
            throw new PurchaseException(FAILED, false, "Signature invalid");
        }
    }

    private Order deserialiseOrder(String orderData) throws PurchaseException {
        try {
            return objectMapper().readValue(orderData, Order.class);
        } catch (IOException e) {
            throw new PurchaseException(FAILED, false, "Cannot deserialize order data", e);
        }
    }

    private ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    private DeveloperPayload deserialiseDeveloperPayload(String developerPayload) throws PurchaseException {
        try {
            return objectMapper().readValue(developerPayload, DeveloperPayload.class);
        } catch (IOException e) {
            throw new PurchaseException(FAILED, false, "Cannot deserialize developer payload", e);
        }
    }

    private void validateDeveloperPayload(DeveloperPayload developerPayload) throws PurchaseException {
        if (developerPayload.getPurchaseId() == null) {
            throw new PurchaseException(FAILED, false, "PurchaseId not present");
        }
    }

    private ChipBundle findChipBundle(String gameType, String productId) throws PurchaseException {
        ChipBundle bundle = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        if (bundle == null) {
            throw new PurchaseException(FAILED, false, "Unknown product id");
        }
        return bundle;
    }

    private AndroidPaymentStateDetails findPaymentStateDetailsFor(String purchaseId) throws PurchaseException {
        AndroidPaymentStateDetails paymentDetails = paymentStateService.findPaymentStateDetailsFor(purchaseId);

        if (paymentDetails == null) {
            throw new PurchaseException(FAILED, false, "Could not find transaction");
        }

        return paymentDetails;
    }

    private GooglePurchase asSuccessfulPurchase(ChipBundle bundle) {
        GooglePurchase purchase = new GooglePurchase();
        purchase.setCanConsume(true);
        purchase.setStatus(SUCCESS);
        purchase.setChips(bundle.getChips());
        purchase.setPrice(bundle.getPrice());
        purchase.setCurrencyCode(bundle.getCurrency().getCurrencyCode());
        return purchase;
    }

    private void logCreditPurchaseException(String gameType, String orderData, String signature, Exception e) {
        if (e instanceof PurchaseException) {
            PurchaseException p = (PurchaseException) e;
            if ("signature invalid".equalsIgnoreCase(p.getErrorMessage())) {
                LOG.info("Failed to credit purchase due to invalid signature: gameType={}, orderData={}, signature={}, status={}, errorMessage={}, debugMessage={}. Exception follows:",
                        gameType, orderData, signature, p.getStatus(), p.getErrorMessage(), p.getDebugMessage(), p);
            } else {
                LOG.error("Failed to credit purchase: gameType={}, orderData={}, signature={}, status={}, errorMessage={}, debugMessage={}. Exception follows:",
                        gameType, orderData, signature, p.getStatus(), p.getErrorMessage(), p.getDebugMessage(), p);
            }
        } else {
            LOG.error("Failed to credit purchase: gameType={}, orderData={}, signature={}. Exception follows:", gameType, orderData, signature, e);
        }
    }

    private void logExternalTransactionRequest(BigDecimal playerId,
                                               String gameType,
                                               String productId,
                                               Long promoId,
                                               DateTime requestTime,
                                               GooglePurchase purchaseRequest,
                                               ChipBundle chipBundleRequested) {
        final BigDecimal accountId = playerService.getAccountId(playerId);
        ExternalTransaction txn = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(purchaseRequest.getPurchaseId())
                .withExternalTransactionId(null)
                .withMessage("productId: " + productId, requestTime)
                .withAmount(chipBundleRequested.getCurrency(), chipBundleRequested.getPrice())
                .withPaymentOption(chipBundleRequested.getChips(), null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withPromotionId(promoId)
                .withPlatform(Platform.ANDROID)
                .build();
        record(txn);
    }

    public void logFailedTransaction(String internalTransactionId, String message) {
        LOG.debug("logging failed transaction: purchaseId={}, message={}", internalTransactionId, message);

        AndroidPaymentStateDetails paymentStateDetails = paymentStateService.findPaymentStateDetailsFor(internalTransactionId);
        if (paymentStateDetails == null) {
            LOG.error("Failed to load payment state details for internalTransactionId={}, message={}", internalTransactionId, message);
            return;
        }
        try {
            paymentStateService.markPurchaseAsFailed(paymentStateDetails.getPlayerId(), internalTransactionId);
            logExternalTransaction(ExternalTransactionStatus.FAILURE, paymentStateDetails, message, new DateTime());
        } catch (AndroidPaymentStateException e) {
            LOG.error("failed to update payment state to FAILED when logging failed txn, internalTransactionId={}, message={}",
                    internalTransactionId, message, e);
        }
    }

    public void logUserCancelledTransaction(String internalTransactionId, String message) {
        LOG.debug("logging user cancelled transaction: purchaseId={}, message={}", internalTransactionId, message);

        AndroidPaymentStateDetails paymentStateDetails = paymentStateService.findPaymentStateDetailsFor(internalTransactionId);
        if (paymentStateDetails == null) {
            LOG.error("Failed to load payment state details when logging user cancelled txn, internalTransactionId = {}, message={}",
                    internalTransactionId, message);
            return;
        }
        try {
            paymentStateService.markPurchaseAsUserCancelled(paymentStateDetails.getPlayerId(), internalTransactionId);
            logExternalTransaction(ExternalTransactionStatus.CANCELLED, paymentStateDetails, message, new DateTime());
        } catch (AndroidPaymentStateException e) {
            LOG.error("failed to update payment state to CANCELLED for internalTransactionId={}, message={}", internalTransactionId, message, e);
        }
    }

    private void logExternalTransaction(ExternalTransactionStatus status,
                                        AndroidPaymentStateDetails paymentStateDetails,
                                        String message,
                                        DateTime requestTime) {
        ChipBundle chipBundle = getChipBundle(paymentStateDetails.getGameType(), paymentStateDetails.getProductId());
        ExternalTransaction txn = buildExternalTransaction(paymentStateDetails, chipBundle, message, requestTime, status, null);
        record(txn);
    }

    private void record(final ExternalTransaction txn) {
        try {
            walletService.record(txn);
        } catch (WalletServiceException e) {
            LOG.error("Unable to record transaction: {}", txn, e);
        }
    }

    private ExternalTransaction buildExternalTransaction(AndroidPaymentStateDetails paymentStateDetails,
                                                         ChipBundle chipBundle,
                                                         String message,
                                                         DateTime requestTime,
                                                         ExternalTransactionStatus status,
                                                         String orderId) {
        BigDecimal playerId = paymentStateDetails.getPlayerId();
        final BigDecimal accountId = playerService.getAccountId(playerId);
        String gameType = paymentStateDetails.getGameType();
        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(paymentStateDetails.getInternalTransactionId())
                .withExternalTransactionId(orderId)
                .withMessage(message, requestTime)
                .withAmount(chipBundle.getCurrency(), chipBundle.getPrice())
                .withPaymentOption(chipBundle.getChips(), null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(status)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withPromotionId(paymentStateDetails.getPromoId())
                .withPlatform(Platform.ANDROID)
                .build();
    }

    private ChipBundle getChipBundle(String gameType, String productId) {
        ChipBundle chipBundle = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        if (chipBundle == null) {
            LOG.error("No chip bundle found for product: {}", productId);
            chipBundle = getChipBundleForUnknownProductId();
        }
        return chipBundle;
    }


    private ChipBundle getChipBundleForUnknownProductId() {
        ChipBundle chipBundle;
        chipBundle = new ChipBundle();
        chipBundle.setCurrency(Currency.getInstance("USD"));
        chipBundle.setChips(BigDecimal.ZERO);
        chipBundle.setPrice(BigDecimal.ZERO);
        return chipBundle;
    }
}
