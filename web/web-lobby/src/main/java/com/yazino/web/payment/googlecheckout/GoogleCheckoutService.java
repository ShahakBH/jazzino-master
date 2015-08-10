package com.yazino.web.payment.googlecheckout;

import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.util.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static com.yazino.web.payment.googlecheckout.Order.Status.*;

@Service("googleCheckoutService")
public class GoogleCheckoutService {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleCheckoutService.class);

    // cashier name, product id, account id, date time
    // e.g. GoogleCheckout_poker_usd5_buys_10k_124345_20120524T094505693
    private static final String INTERNAL_ID_FORMAT = "%s_%s_%s_%4$tY%<tm%<tdT%<tH%<tM%<tS%<tL";

    static final String OBSCURED_CREDITCARD_NUMBER = "none";
    static final String CASHIER_NAME = "GoogleCheckout";
    static final String UNKNOW_PRODUCT_FAILURE_MSG_FORMAT =
            "Failed to credit chips for productId=%s, playerId=%s.\n"
                    + "The transaction has been authorized/charged by Google Play but we cannot credit the player "
                    + "with chips as we do not recognise the productId.";
    static final String DEFAULT_FAILURE_MSG_FORMAT =
            "Failed to credit chips for productId=%s, playerId=%s. The order status is %s.";
    public static final String DEFAULT_CURRENCY_IF_UNKNOWN = "usd";

    private final GoogleCheckoutApiIntegration googleCheckoutApiIntegration;
    private final WalletService walletService;
    private final PlayerService playerService;
    private final CommunityService communityService;
    private final QuietPlayerEmailer emailer;
    private final ChipBundleResolver chipBundleResolver;
    private final PaymentStateService paymentStateService;

    @Autowired
    public GoogleCheckoutService(final GoogleCheckoutApiIntegration googleCheckoutApiIntegration,
                                 final WalletService walletService,
                                 final PlayerService playerService,
                                 final CommunityService communityService,
                                 final QuietPlayerEmailer emailer,
                                 final ChipBundleResolver chipBundleResolver,
                                 final PaymentStateService paymentStateService,
                                 final AndroidInAppOrderSecurity androidInAppOrderSecurity) {
        Validate.notNull(googleCheckoutApiIntegration);
        Validate.notNull(walletService);
        Validate.notNull(playerService);
        Validate.notNull(communityService);
        Validate.notNull(emailer);
        Validate.notNull(chipBundleResolver);
        Validate.notNull(paymentStateService);
        this.googleCheckoutApiIntegration = googleCheckoutApiIntegration;
        this.walletService = walletService;
        this.playerService = playerService;
        this.communityService = communityService;
        this.emailer = emailer;
        this.chipBundleResolver = chipBundleResolver;
        this.paymentStateService = paymentStateService;
    }

    /**
     * Attempts to fulfill the order by crediting the player with chips.<br/>
     * Players will have chips credited if the transaction has been authorized.
     * <p/>
     * NOTE as of 2012-12-05 google return the 'Merchant Order Number' as orderId to client app. This method of completing an
     * order should only be used when the client hasn't sent through the order json from GooglePlay. Here the only way
     * of trying to fulfill the txn is to assume the order number is the Google Order Number. This will almost certainly fail.
     *
     * @param paymentContext context of order.
     * @param orderNumber    order to fulfill
     * @return summary of current order. The order's status will be one of the following:
     *         <ul>
     *         <li>DELIVERED - the order has already been fulfilled. Either now, or in a previous request.</li>
     *         <li>UNKNOWN_PRODUCT - the product id of the order is unknown.</li>
     *         <li>PAYMENT_NOT_AUTHORIZED - Google Checkout has not authorized the order yet.</li>
     *         <li>INVALID_ORDER_NUMBER - order number s unknown to Google, or foramt is invalid.</li>
     *         <li>CANCELLED. Payment cancelled</li>
     *         <li>IN_PROGESS - processing has already started. Indicates a repeated request.</li>
     *         <li>ERROR - unable to either start a new transaction or unable to retrieve existing state.
     *         </ul>
     */
    public Order fulfillLegacyBuyChipsOrder(final PaymentContext paymentContext, final String orderNumber) {
        Validate.notNull(paymentContext, "paymentContext cannot be null");
        Validate.notBlank(orderNumber, "orderNumber cannot be null or empty");

        final DateTime messageTimeStamp = new DateTime();
        logFulfillRequest(paymentContext.getPlayerId(), orderNumber);

        final Order.Status currentStatus = startPayment(orderNumber);
        if (currentStatus != null) {
            return new Order(orderNumber, currentStatus);
        }

        return processOrder(orderNumber, paymentContext, messageTimeStamp);
    }

    /**
     * @param paymentContext context of order.
     * @param orderJson      order data returned by GooglePlay can contain multiple orders
     * @return details of orders
     * @deprecated use {@link AndroidInAppBillingService#verifyAndCompleteTransactions(com.yazino.web.payment.PaymentContext, String, String, String)}.
     *             When number of clients using old endpoint '/payments/gogglecheckout/complete' has reduced to an acceptable level, delete this.
     */
    public List<Order> fulfillBuyChipsOrder(final PaymentContext paymentContext, String orderJson) {
        Validate.notNull(paymentContext, "paymentContext cannot be null");
        Validate.notNull(orderJson, "orderJson cannot be null or empty");

        final DateTime messageTimeStamp = new DateTime();
        logFulfillRequest(paymentContext.getPlayerId(), orderJson);

        final GooglePlayBillingTransactions billingTransactions = new JsonHelper().deserialize(GooglePlayBillingTransactions.class, orderJson);
        final List<GooglePlayBillingTransactions.GooglePlayTransaction> googlePlayTransactions = billingTransactions.getGooglePlayTransactions();

        List<Order> orders = new ArrayList<Order>();

        for (GooglePlayBillingTransactions.GooglePlayTransaction transaction : googlePlayTransactions) {
            final Order.Status currentStatus = startPayment(transaction.getOrderId());
            if (currentStatus != null) {
                orders.add(new Order(transaction.getOrderId(), currentStatus));
            } else if (isMerchantOrderNumber(transaction.getOrderId())) {
                orders.add(processOrderWithMerchantOrderNumber(paymentContext, transaction, messageTimeStamp));
            } else {
                orders.add(processOrder(transaction.getOrderId(), paymentContext, messageTimeStamp));
            }
        }
        return orders;
    }

    private Order processOrderWithMerchantOrderNumber(PaymentContext paymentContext, GooglePlayBillingTransactions.GooglePlayTransaction googlePlayOrder, DateTime messageTimeStamp) {
        Order order;
        order = new Order(googlePlayOrder.getOrderId(), PAYMENT_AUTHORIZED);
        final String productId = googlePlayOrder.getProductId();
        // derive the package price from product id. This is flakey of course, should modify googleCheckoutBuyChipProducts
        // to return required info.
        final int usdIndex = productId.indexOf(DEFAULT_CURRENCY_IF_UNKNOWN.toLowerCase());
        if (usdIndex >= 0) {
            final String priceInUSD = productId.substring(usdIndex + 3, productId.indexOf("_", usdIndex));
            order.setCurrencyCode(DEFAULT_CURRENCY_IF_UNKNOWN.toUpperCase());
            order.setPrice(new BigDecimal(priceInUSD));
        }
        order.setProductId(productId);
        processedAuthorizedOrder(paymentContext, messageTimeStamp, order);
        return order;
    }


    private boolean isMerchantOrderNumber(String orderNumber) {
        return orderNumber.matches("[0-9]+\\.[0-9]+");
    }

    private Order processOrder(final String orderNumber,
                               final PaymentContext paymentContext,
                               final DateTime messageTimeStamp) {
        final Order order = googleCheckoutApiIntegration.retrieveOrderState(orderNumber);
        final Order.Status status = order.getStatus();
        if (orderIsAuthorized(status)) {
            processedAuthorizedOrder(paymentContext, messageTimeStamp, order);
        } else if (processOrderLater(status)) {
            failPayment(order);
        } else if (cannotProcessOrder(status)) {
            finishPaymentForInvalidOrder(paymentContext, messageTimeStamp, order);
        }
        return order;
    }

    private boolean cannotProcessOrder(final Order.Status status) {
        return INVALID_ORDER_NUMBER == status || CANCELLED == status;
    }

    private boolean processOrderLater(final Order.Status status) {
        return PAYMENT_NOT_AUTHORIZED == status || ERROR == status;
    }

    private boolean orderIsAuthorized(final Order.Status status) {
        return PAYMENT_AUTHORIZED == status;
    }

    private void processedAuthorizedOrder(final PaymentContext paymentContext,
                                          final DateTime messageTimeStamp,
                                          final Order order) {
        final String productId = order.getProductId();
        final String gameType = paymentContext.getGameType();
        final ChipBundle chipBundle = chipBundleResolver.findChipBundleForProductId(gameType, productId);
        ExternalTransactionStatus txnStatus;
        if (chipBundle != null) {
            order.setChips(chipBundle.getChips());
            txnStatus = SUCCESS;
        } else {
            // if we couldn't get the amount of chips to credit, flag txn as a 'failure'. This indicates that the
            // products uploaded to the store are different to those configured serverside.
            txnStatus = FAILURE;
            order.setStatus(UNKNOWN_PRODUCT);
        }
        final ExternalTransaction txn = buildExternalTransaction(paymentContext.getPlayerId(), gameType,
                messageTimeStamp, order, txnStatus, paymentContext.getPromotionId());

        if (FAILURE == txnStatus) {
            logAndFinishFailedTransactionDueToUnknownProduct(paymentContext.getPlayerId(), productId, txn);
        } else {
            creditPlayerWithChips(paymentContext, messageTimeStamp, order, txn);
        }
    }

    /**
     * Attempts to start the payment transaction. If the payment process can proceed, null is returned. This is the case
     * when either the order is new or
     * a previous payment attempted failed, but can be repeated.
     *
     * @param orderNumber order to start transaction for.
     * @return null if this is a new request to process the payment, or if a previous attempt failed but should be
     *         repeated. Otherwise one of the following states will be returned:
     *         The following 3 states are returned when an attempt to process the order has already been made.
     *         <ul>
     *         <li>DELIVERED - the order has already been fulfilled. Indicates a repeated request.</li>
     *         <li>INVALID_ORDER_NUMBER. Indicates repeated request</li>
     *         <li>CANCELLED. Payment not authorized or payment cancelled</li>
     *         </ul>
     *         These are the remaining states that may be returned:
     *         <ul>
     *         <li>IN_PROGESS - processing has already started. Indicates a repeated request.</li>
     *         <li>ERROR - unable to either start a new transaction or unable to retrieve existing state.
     *         Indicates a problem with an underlying service. The client should try again.</li>
     *         </ul>
     */
    private Order.Status startPayment(final String orderNumber) {
        try {
            paymentStateService.startPayment(CASHIER_NAME, orderNumber);
        } catch (PaymentStateException e) {
            final PaymentState paymentState = e.getState();
            switch (paymentState) {
                case Finished:
                    if (isMerchantOrderNumber(orderNumber)) {
                        return DELIVERED;
                    } else {
                        // why is the order finished, either it's DELIVERED, INVALID or was CANCELLED
                        Order order = googleCheckoutApiIntegration.retrieveOrderState(orderNumber);
                        /*
                           Google automatically set the DELIVERED order fulfillment state for digital goods. So we can no longer
                           use this state to determine whether we have credited chips to a successful order. Now we set the DELIVERED
                           state (in the Order) when chips have been successfully credited.
                           The Finished state represents 2 things
                           1 - the order was successful and chips habe been credited
                           2 - the order wasn't successful, chips haven't been credited and there is no point trying again.
                           Really we should spilt this state into 2 states meanings as above. Until then we have the bodge below.
                        */
                        if (order.getStatus() == Order.Status.PAYMENT_AUTHORIZED) {
                            order.setStatus(Order.Status.DELIVERED);
                        }
                        LOG.error("Attempt to start payment for finished order, state={} orderNumber={}"
                                , orderNumber, order.getStatus().name());
                        return order.getStatus();
                    }
                case Started:
                    LOG.error("Attempt to start payment for order being processed, orderNumber={}", orderNumber);
                    return Order.Status.IN_PROGRESS;
                case Unknown:
                    LOG.error("Failed to fetch payment state for order, orderNumber={}", orderNumber);
                    return Order.Status.ERROR;
                case Failed:
                default:
                    LOG.warn("Retrying payment for order {}", orderNumber);
                    return null;
            }
        }
        LOG.debug("Starting payment for order {}", orderNumber);
        return null;
    }

    // log a failed external transaction and finish payment
    private void finishPaymentForInvalidOrder(final PaymentContext paymentContext,
                                              final DateTime messageTimeStamp,
                                              final Order order) {
        LOG.error("Order will not be fulfilled. order status={}, orderNumber={}",
                order.getStatus().name(), order.getOrderNumber());
        final ExternalTransaction externalTransaction = buildExternalTransaction(paymentContext.getPlayerId(),
                paymentContext.getGameType(), messageTimeStamp, order, ExternalTransactionStatus.FAILURE, paymentContext.getPromotionId());
        record(externalTransaction);
        try {
            paymentStateService.finishPayment(CASHIER_NAME, order.getOrderNumber());
        } catch (PaymentStateException e) {
            LOG.error("Failed to change payment state to finished for order {}", order.getOrderNumber());
        }
    }

    private void failPayment(final Order order) {
        LOG.error("Request to fulfill order ignored. Only orders that are authorized but not yet delivered can be "
                + "fulfilled. order status={}, orderNumber={}", order.getStatus().name(), order.getOrderNumber());
        try {
            paymentStateService.failPayment(CASHIER_NAME, order.getOrderNumber());
        } catch (PaymentStateException e) {
            LOG.error("Failed to change payment state to failed for order {}", order.getOrderNumber());
        }
    }

    /**
     * Finds all the Google Play products currently available for given game type.
     *
     * @param gameType fetch products for this game type
     * @return list of product ids
     */
    public List<String> fetchAvailableProducts(final String gameType) {
        Validate.notBlank(gameType, "gameType cannot be null or empty");
        logProductRequest(gameType);
        return chipBundleResolver.getProductIdsFor(gameType);
    }

    private void logProductRequest(final String gameType) {
        LOG.debug("Fetching play products fot gameType={}", gameType);
    }

    private void creditPlayerWithChips(final PaymentContext paymentContext,
                                       final DateTime messageTimeStamp,
                                       final Order order,
                                       final ExternalTransaction txn) {
        try {
            walletService.record(txn);
            order.setStatus(Order.Status.DELIVERED);
            paymentStateService.finishPayment(CASHIER_NAME, order.getOrderNumber());
            communityService.asyncPublishBalance(paymentContext.getPlayerId());
            notifyPlayer(paymentContext, messageTimeStamp, order);
        } catch (WalletServiceException e) {
            handleWalletException(paymentContext, order, e);
        } catch (PaymentStateException e) {
            LOG.error("Failed to update payment state for google checkout order {}, player {}. "
                    + "Chips have been credited.",
                    order.getOrderNumber(), paymentContext.getPlayerId().toPlainString(), e);
        }
    }

    // set payment state to failed so client can try again
    private void handleWalletException(final PaymentContext paymentContext,
                                       final Order order,
                                       final WalletServiceException e) {
        try {
            paymentStateService.failPayment(CASHIER_NAME, order.getOrderNumber());
        } catch (PaymentStateException pe) {
            LOG.error("Failed to update payment state to failed for google checkout order {}, player {}. "
                    + "Chips have been credited.",
                    order.getOrderNumber(), paymentContext.getPlayerId().toPlainString(), e);
        }
        LOG.error("Failed update player balance for google checkout txn. Google checkout order {}, player {}.",
                order.getOrderNumber(), paymentContext.getPlayerId().toPlainString(), e);
    }

    private void logAndFinishFailedTransactionDueToUnknownProduct(final BigDecimal playerId,
                                                                  final String productId,
                                                                  final ExternalTransaction txn) {
        LOG.error("Failed to find PaymentOption for product {}. "
                + "Player {} has not been credited with chips but has been charged.", productId, playerId);
        record(txn);
        try {
            // cannot try again since product is unknown, would expect same result...
            paymentStateService.finishPayment(CASHIER_NAME, txn.getExternalTransactionId());
        } catch (PaymentStateException e) {
            LOG.warn("Failed to update payment state to finished for order {}", txn.getExternalTransactionId());
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
                              final Order order) {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        builder.withEmailAddress(context.getEmailAddress());
        builder.withFirstName(context.getPlayerName());
        builder.withPurchasedChips(order.getChips());
        builder.withCurrency(Currency.getInstance(order.getCurrencyCode()));
        builder.withCost(order.getPrice());
        builder.withPaymentDate(timestamp.toDate());
        builder.withCardNumber("");
        builder.withPaymentId(order.getOrderNumber());
        builder.withPaymentEmailBodyTemplate(PaymentEmailBodyTemplate.GoogleCheckout);
        emailer.quietlySendEmail(builder);
    }

    private ExternalTransaction buildExternalTransaction(final BigDecimal playerId,
                                                         final String gameType,
                                                         final DateTime messageTimeStamp,
                                                         final Order order,
                                                         final ExternalTransactionStatus txnStatus, final Long promotionId) {
        final BigDecimal accountId = playerService.getAccountId(playerId);
        final String internalTransactionId = buildInternalId(messageTimeStamp, accountId, order.getProductId());
        String message = "";
        if (FAILURE == txnStatus) {
            message = buildExternalTxnMessageForFailedTxn(playerId, order);
        }

        // no guarantee what we'll get from google
        BigDecimal amount = BigDecimal.ZERO;
        if (order.getPrice() != null) {
            amount = order.getPrice();
        }
        String currencyCode = "USD";
        if (StringUtils.isNotBlank(order.getCurrencyCode())) {
            currencyCode = order.getCurrencyCode();
        }
        // if building a failed transaction then chips may be null
        BigDecimal chips = order.getChips();
        if (chips == null) {
            chips = BigDecimal.ZERO;
        }
        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalTransactionId)
                .withExternalTransactionId(order.getOrderNumber())
                .withMessage(message, messageTimeStamp)
                .withAmount(Currency.getInstance(currencyCode), amount)
                .withPaymentOption(chips, null)
                .withCreditCardNumber(OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(CASHIER_NAME)
                .withStatus(txnStatus)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withPromotionId(promotionId)
                .withPlatform(ANDROID)
                .build();
    }

    private String buildExternalTxnMessageForFailedTxn(final BigDecimal playerId, final Order order) {
        if (UNKNOWN_PRODUCT == order.getStatus()) {
            return String.format(UNKNOW_PRODUCT_FAILURE_MSG_FORMAT, order.getProductId(), playerId.toPlainString());
        }
        return String.format(DEFAULT_FAILURE_MSG_FORMAT, order.getProductId(), playerId, order.getStatus());
    }

    private String buildInternalId(final DateTime messageTimeStamp,
                                   final BigDecimal accountId,
                                   final String productIdentifier) {
        final DateTime dtLondon = messageTimeStamp.withZone(DateTimeZone.forID("Europe/London"));
        return String.format(INTERNAL_ID_FORMAT, CASHIER_NAME, productIdentifier, accountId.toPlainString(),
                dtLondon.toDate());
    }

    private void logFulfillRequest(final BigDecimal playerId, final String orderNumber) {
        LOG.debug("Request to fulfill google player orders {} for player {}",
                orderNumber, playerId.toPlainString());
    }
}
