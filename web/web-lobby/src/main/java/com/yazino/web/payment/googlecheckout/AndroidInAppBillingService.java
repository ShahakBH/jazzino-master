package com.yazino.web.payment.googlecheckout;

import com.google.common.base.Objects;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.util.JsonHelper;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static com.yazino.platform.account.ExternalTransactionType.DEPOSIT;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static com.yazino.web.payment.googlecheckout.GooglePlayBillingTransactions.GooglePlayTransaction;

@Service
public class AndroidInAppBillingService {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidInAppBillingService.class);
    public static final String PURCHASE_STATE_PURCHASED = "0";

    static final String OBSCURED_CREDITCARD_NUMBER = "none";
    static final String CASHIER_NAME = "GoogleCheckout";
    private static final String PROPERTY_PARTNER_ID = "strata.lobby.partnerid";
    private static final String DEFAULT_PARTNER_ID = "YAZINO";
    // internal id format is: cashierName_productId_accountId_datetime e.g. GoogleCheckout_poker_usd5_buys_10k_124345_20120524T094505693
    private static final String INTERNAL_ID_FORMAT = "%s_%s_%s_%4$tY%<tm%<tdT%<tH%<tM%<tS%<tL";
    private static final String UNKNOW_PRODUCT_FAILURE_MSG_FORMAT =
            "Failed to credit chips for productId=%s, playerId=%s.\n"
                    + "The transaction has been authorized/charged by Google Play but we cannot credit the player "
                    + "with chips as we do not recognise the productId.";

    private final AndroidInAppOrderSecurity androidInAppOrderSecurity;
    private final PaymentStateService paymentStateService;
    private final ChipBundleResolver chipBundleResolver;
    private final WalletService walletService;
    private final PlayerService playerService;
    private final CommunityService communityService;
    private final QuietPlayerEmailer emailer;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final String sender;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public AndroidInAppBillingService(AndroidInAppOrderSecurity androidInAppOrderSecurity,
                                      PaymentStateService paymentStateService,
                                      ChipBundleResolver chipBundleResolver,
                                      WalletService walletService,
                                      PlayerService playerService,
                                      CommunityService communityService,
                                      QuietPlayerEmailer emailer,
                                      @Qualifier("safeBuyChipsPromotionService") BuyChipsPromotionService buyChipsPromotionService,
                                      @Value("${strata.email.from-address}") final String sender,
                                      final YazinoConfiguration yazinoConfiguration) {
        this.androidInAppOrderSecurity = androidInAppOrderSecurity;
        this.paymentStateService = paymentStateService;
        this.chipBundleResolver = chipBundleResolver;
        this.walletService = walletService;
        this.playerService = playerService;
        this.communityService = communityService;
        this.emailer = emailer;
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.sender = sender;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    /**
     * Completes each transaction listed in {@code orderJSON}, crediting player with the appropriate amount of chips, and logging
     * any promotions used.
     * See <a ref="http://wiki.london.yazino.com/display/DEV/Android+In-app+Billing">In-App Billing Integration</a>
     *
     * @param paymentContext context of order. Note paymentOption and promotionId fields are not used.
     * @param orderJSON      data returned from in-app billing containing the state and details of in-app transactions
     * @param signature      the signature of the data (orderJSON), signed with the private key by google play
     * @param promoIds       json map of promoIds keyed on orderId. Each order that was for a promoted product will have an entry in this
     *                       map. The map will be empty if no purchases were for promoted products.
     * @return List of verified orders. If {@code orderJSON wasn't signed correctly all orders will have ERROR state}.
     * Orders with a purchase state other than 0 (i.e. purchased) will also have the error state.
     */
    public List<VerifiedOrder> verifyAndCompleteTransactions(PaymentContext paymentContext,
                                                             String orderJSON,
                                                             String signature,
                                                             String promoIds) {
        validateAndLogVerifyArgs(paymentContext, orderJSON, signature, promoIds);

        final List<VerifiedOrder> verifiedOrders = new ArrayList<VerifiedOrder>();
        final Partner partnerId;
        if ((paymentContext.getPartnerId() == null)) {
            partnerId = Partner.parse(yazinoConfiguration.getString(PROPERTY_PARTNER_ID, DEFAULT_PARTNER_ID));
        } else {
            partnerId = paymentContext.getPartnerId();
        }
        final boolean verified = androidInAppOrderSecurity.verify(paymentContext.getGameType(), orderJSON, signature, partnerId);
        if (verified) {
            final GooglePlayBillingTransactions billingTransactions = new JsonHelper().deserialize(GooglePlayBillingTransactions.class,
                    orderJSON);
            // orderId -> promoId
            final Map<String, String> promoIdMap = new JsonHelper().deserialize(Map.class, promoIds);

            for (GooglePlayTransaction googlePlayTransaction : billingTransactions.getGooglePlayTransactions()) {
                VerifiedOrder order = processChargedOrder(paymentContext, googlePlayTransaction, promoIdMap);
                verifiedOrders.add(order);
            }
        }
        return verifiedOrders;
    }

    private void validateAndLogVerifyArgs(PaymentContext paymentContext, String orderJSON, String signature, String promoIds) {
        Validate.notNull(paymentContext, "cannot complete android transaction with null paymentContext");
        Validate.notNull(orderJSON, "cannot complete android transaction with null orderJSON");
        Validate.notNull(signature, "cannot complete android transaction with null signature");
        Validate.notNull(promoIds, "cannot complete android transaction with null promoIds");
        LOG.info("Verifying and completing google play orders:  player {}, gameType {}, orders {}, signature {}, promoIds {}",
                paymentContext.getPlayerId(), paymentContext.getGameType(), orderJSON, signature, promoIds);
    }

    /*
     * Transforms a GooglePlayTransaction to a VerifiedOrder with given gameType. Note that the order's status is set to ERROR.
     * The chip bundle is retrieved for the transaction's productId, and the chip amount, price and currency code are set on the returned
     * object. These fields will be null if a bundle was not found for the given productId.
     */
    private VerifiedOrder transformTransaction(GooglePlayTransaction googlePlayTransaction, String gameType) {
        final String productId = googlePlayTransaction.getProductId();
        VerifiedOrderBuilder verifiedOrderBuilder = new VerifiedOrderBuilder()
                .withOrderId(googlePlayTransaction.getOrderId())
                .withProductId(productId)
                .withStatus(OrderStatus.ERROR);
        final ChipBundle chipBundle = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        if (chipBundle != null) {
            verifiedOrderBuilder
                    .withChips(chipBundle.getChips())
                    .withCurrencyCode(chipBundle.getCurrency().getCurrencyCode())
                    .withPrice(chipBundle.getPrice())
                    .withDefaultChips(chipBundle.getDefaultChips());
        }
        return verifiedOrderBuilder.buildVerifiedOrder();
    }

    private VerifiedOrder processChargedOrder(PaymentContext paymentContext,
                                              GooglePlayTransaction googlePlayTransaction,
                                              Map<String, String> promoIdMap) {
        LOG.info("Processing Android In-App transaction: " + googlePlayTransaction);

        VerifiedOrder order = transformTransaction(googlePlayTransaction, paymentContext.getGameType());
        try {
            if (PURCHASE_STATE_PURCHASED.equals(googlePlayTransaction.getPurchaseState())) {
                paymentStateService.startPayment(GoogleCheckoutService.CASHIER_NAME, order.getOrderId());
                if (order.getChips() == null) {
                    logAndFinishFailedTransactionDueToUnknownProduct(paymentContext,
                            googlePlayTransaction.getPurchaseTime(),
                            order);
                } else {
                    creditPlayerWithChips(paymentContext, googlePlayTransaction.getPurchaseTime(), order, promoIdMap);
                }
            }
        } catch (PaymentStateException e) {
            handleStartPaymentException(order, e);

        }
        return order;
    }

    private void handleStartPaymentException(VerifiedOrder order, PaymentStateException e) {
        // we want to keep the client simple as possible, hence only two states DELIVERED or ERROR, with this in mind we treat
        // all payment state exceptions as ERRORs other than Finished (which indicates a prior successful completion).
        if (e.getState() == PaymentState.Finished) {
            order.setStatus(OrderStatus.DELIVERED);
            LOG.info("Attempting to complete an already completed google play transaction: {}, payment state: {}",
                    order, e.getState());
        } else {
            LOG.error("Failed to start payment for google play transaction: {}, payment state: {}", order, e.getState());
        }
    }

    private ExternalTransaction buildExternalTransaction(ExternalTransactionStatus txnStatus,
                                                         PaymentContext paymentContext,
                                                         DateTime messageTimeStamp,
                                                         VerifiedOrder order,
                                                         String message, Long promoId) {
        final BigDecimal accountId = playerService.getAccountId(paymentContext.getPlayerId());
        final String internalTransactionId = buildInternalId(messageTimeStamp, accountId, order.getProductId());
        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalTransactionId)
                .withExternalTransactionId(order.getOrderId())
                .withMessage(message, messageTimeStamp)
                .withAmount(Currency.getInstance(Objects.firstNonNull(order.getCurrencyCode(), "USD")),
                        Objects.firstNonNull(order.getPrice(), BigDecimal.ZERO))
                .withPaymentOption(Objects.firstNonNull(order.getChips(), BigDecimal.ZERO), null)
                .withCreditCardNumber(OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(CASHIER_NAME)
                .withStatus(txnStatus)
                .withType(DEPOSIT)
                .withGameType(paymentContext.getGameType())
                .withPlayerId(paymentContext.getPlayerId())
                .withSessionId(paymentContext.getSessionId())
                .withPromotionId(promoId)
                .withPlatform(ANDROID)
                .build();
    }

    private String buildInternalId(DateTime messageTimeStamp, BigDecimal accountId, String productIdentifier) {
        final DateTime dtLondon = messageTimeStamp.withZone(DateTimeZone.forID("Europe/London"));
        return String.format(INTERNAL_ID_FORMAT, CASHIER_NAME, productIdentifier, accountId, dtLondon.toDate());
    }

    private void creditPlayerWithChips(PaymentContext paymentContext,
                                       DateTime purchaseTime,
                                       VerifiedOrder order,
                                       Map<String, String> promoIdMap) {
        final String orderId = order.getOrderId();
        final BigDecimal playerId = paymentContext.getPlayerId();
        try {
            final Long promoId = promoIdMap.get(orderId) == null ? null : Long.parseLong(promoIdMap.get(orderId));
            final ExternalTransaction txn = buildExternalTransaction(SUCCESS, paymentContext, purchaseTime, order, "", promoId);
            walletService.record(txn);
            order.setStatus(OrderStatus.DELIVERED);
            finishPayment(orderId, playerId);
            logPromotion(purchaseTime, order, promoId, playerId);
            communityService.asyncPublishBalance(playerId);
            notifyPlayer(paymentContext, purchaseTime, order);
        } catch (WalletServiceException e) {
            handleWalletException(paymentContext, order, e);
        }
    }

    private void logPromotion(DateTime messageTimeStamp, VerifiedOrder order, Long promoId, BigDecimal playerId) {
        if (promoId != null) {
            buyChipsPromotionService.logPlayerReward(playerId,
                    promoId,
                    order.getDefaultChips(),
                    order.getChips(),
                    GOOGLE_CHECKOUT,
                    messageTimeStamp);
        }
    }

    private void finishPayment(String orderId, BigDecimal playerId) {
        try {
            paymentStateService.finishPayment(CASHIER_NAME, orderId);
        } catch (PaymentStateException e) {
            LOG.error("Failed to update payment state to Finished for android order {}, player {}. Chips have been credited.",
                    orderId, playerId, e);
        }
    }

    private void handleWalletException(final PaymentContext paymentContext,
                                       final VerifiedOrder order,
                                       final WalletServiceException e) {
        final String orderId = order.getOrderId();
        final BigDecimal playerId = paymentContext.getPlayerId();
        try {
            paymentStateService.failPayment(CASHIER_NAME, orderId, false);
        } catch (PaymentStateException pe) {
            LOG.error("Failed to update payment state to FinishedFailed for android order: {}, player: {}. Chips have been credited.",
                    orderId, playerId, e);
        }
        LOG.error("Failed to update player balance for android order. orderId: {}, player: {}.", orderId, playerId, e);
    }

    private void logAndFinishFailedTransactionDueToUnknownProduct(PaymentContext paymentContext,
                                                                  DateTime purchaseTime,
                                                                  VerifiedOrder order) {
        final BigDecimal playerId = paymentContext.getPlayerId();
        final String productId = order.getProductId();
        LOG.error("Failed to find PaymentOption for product {}. Player {} has not been credited with chips but has been charged.",
                productId, playerId);
        String message = String.format(UNKNOW_PRODUCT_FAILURE_MSG_FORMAT, productId, playerId);
        final ExternalTransaction txn = buildExternalTransaction(FAILURE, paymentContext, purchaseTime, order, message, null);
        record(txn);
        try {
            paymentStateService.failPayment(CASHIER_NAME, txn.getExternalTransactionId(), false);
        } catch (PaymentStateException e) {
            LOG.warn("Failed to update payment state to FinishedFailed for order {}", order.getOrderId());
        }
    }

    private void record(final ExternalTransaction txn) {
        try {
            walletService.record(txn);
        } catch (WalletServiceException e) {
            LOG.error("Unable to record transaction: {}", txn, e);
        }
    }

    private void notifyPlayer(final PaymentContext context,
                              final DateTime timestamp,
                              final VerifiedOrder order) {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        builder.withEmailAddress(context.getEmailAddress());
        builder.withFirstName(context.getPlayerName());
        builder.withPurchasedChips(order.getChips());
        builder.withCurrency(Currency.getInstance(order.getCurrencyCode()));
        builder.withCost(order.getPrice());
        builder.withPaymentDate(timestamp.toDate());
        builder.withCardNumber("");
        builder.withPaymentId(order.getOrderId());
        builder.withPaymentEmailBodyTemplate(PaymentEmailBodyTemplate.GoogleCheckout);
        emailer.quietlySendEmail(builder);
    }
}
