package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.payment.googlecheckout.VerifiedOrderBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.web.payment.PurchaseStatus.FAILED;
import static com.yazino.web.payment.PurchaseStatus.SUCCESS;
import static java.lang.String.format;
import static org.springframework.util.Assert.notNull;

@Component
public class AmazonInAppBillingService implements InAppBillingService {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonInAppBillingService.class);
    public static final String INVALID_AMAZON_RECEIPT_MSG_FORMAT = "Invalid Amazon receipt - %s";
    private static final String CASHIER_NAME = "Amazon";
    private YazinoPaymentState yazinoPaymentState;
    private AmazonReceiptVerificationService receiptVerificationService;
    private CommunityService communityService;
    private PlayerNotifier playerNotifier;
    private final ChipBundleResolver chipBundleResolver;
    private final BuyChipsPromotionService buyChipsPromotionService;

    @Autowired
    public AmazonInAppBillingService(final AmazonReceiptVerificationService receiptVerificationService,
                                     final CommunityService communityService,
                                     final YazinoPaymentState yazinoPaymentState,
                                     final PlayerNotifier playerNotifier,
                                     final ChipBundleResolver chipBundleResolver,
                                     @Qualifier("safeBuyChipsPromotionService") final BuyChipsPromotionService buyChipsPromotionService) {
        this.yazinoPaymentState = yazinoPaymentState;
        this.receiptVerificationService = receiptVerificationService;
        this.communityService = communityService;
        this.playerNotifier = playerNotifier;
        this.chipBundleResolver = chipBundleResolver;
        this.buyChipsPromotionService = buyChipsPromotionService;
    }

    @Override
    public Purchase creditPurchase(final PaymentContext paymentContext,
                                   final String userId,
                                   final String externalId,
                                   final String productId,
                                   final String internalId) {
        notNull(paymentContext, "Payment Context cannot be null");
        notNull(userId, "User Id cannot be null");
        notNull(externalId, "Purchase token cannot be null");


        ChipBundle chipBundle = chipBundleResolver.findChipBundleForProductId(paymentContext.getGameType(), productId);
        VerifiedOrder order = buildVerifiedOrder(internalId, productId, chipBundle);
        Purchase purchase = buildPurchase(order, externalId);


        if (!isPurchaseValid(paymentContext, order, userId, externalId)) {
            LOG.error("Amazon payment failed for user {} and token {}. Receipt verification failed", userId, externalId);
            purchase.setStatus(FAILED);
            return purchase;
        }
        if (!yazinoPaymentState.startPayment(paymentContext, order, CASHIER_NAME)) {
            LOG.error("Amazon payment failed for user {} and token {}. Could not add Payment Started entry", userId, externalId);
            purchase.setStatus(FAILED);
            return purchase;
        }
        // walletService.record is hidden in here
        if (!yazinoPaymentState.recordPayment(paymentContext, order, CASHIER_NAME, purchase)) {
            LOG.error("Amazon payment failed for user {} and token {}. Could not record payment", userId, externalId);
            purchase.setStatus(FAILED);
            return purchase;
        }

        communityService.asyncPublishBalance(paymentContext.getPlayerId());
        playerNotifier.emailPlayer(paymentContext, order, PaymentEmailBodyTemplate.Amazon);
        yazinoPaymentState.finishPayment(paymentContext, order, CASHIER_NAME);
        purchase.setStatus(SUCCESS);

        if (paymentContext.getPromotionId() != null) {
            logPromotion(new DateTime(), chipBundle, paymentContext.getPromotionId(), paymentContext.getPlayerId());
        }

        return purchase;
    }

    private void logPromotion(DateTime messageTimeStamp, ChipBundle bundle, Long promoId, BigDecimal playerId) {
        buyChipsPromotionService.logPlayerReward(playerId, promoId, bundle.getDefaultChips(), bundle.getChips(),
                                                 PaymentPreferences.PaymentMethod.AMAZON, messageTimeStamp);
    }

    @Override
    public void logFailedTransaction(final PaymentContext paymentContext, final String productId, final String internalId, final String reasonForFailure) {
        ChipBundle chipBundle = chipBundleResolver.findChipBundleForProductId(paymentContext.getGameType(), productId);
        VerifiedOrder order = buildVerifiedOrder(internalId, productId, chipBundle);
        Purchase purchase = buildPurchase(order, internalId);

        yazinoPaymentState.logFailure(paymentContext, order, CASHIER_NAME, Platform.AMAZON, reasonForFailure, purchase);
    }

    private Purchase buildPurchase(VerifiedOrder order, final String externalId) {
        final Purchase purchase = new Purchase();
        purchase.setChips(order.getChips());
        purchase.setCurrencyCode(order.getCurrencyCode());
        purchase.setPrice(order.getPrice());
        purchase.setPurchaseId(order.getOrderId());
        purchase.setExternalId(externalId);
        purchase.setProductId(order.getProductId());
        return purchase;
    }

    private boolean isPurchaseValid(final PaymentContext paymentContext,
                                    final VerifiedOrder verifiedOrder,
                                    final String userId,
                                    final String externalId) {
        try {
            final VerificationResult verificationResult = receiptVerificationService.verify(userId, externalId);
            if (!verificationResult.isValid()) {
                Purchase purchase = buildPurchase(verifiedOrder, externalId);
                yazinoPaymentState.logFailure(paymentContext, verifiedOrder, CASHIER_NAME, Platform.AMAZON,
                                              format(INVALID_AMAZON_RECEIPT_MSG_FORMAT, verificationResult.name()), purchase);
                return false;
            }
        } catch (IOException e) {
            LOG.error("Failed to credit purchase due to IO exception when communicating with Amazon receipt verification. Error follows:", e);
            return false;
        }
        return true;
    }


    private VerifiedOrder buildVerifiedOrder(String internalId, String productId, ChipBundle chipBundle) {
        return new VerifiedOrderBuilder().withOrderId(internalId).withProductId(productId).withCurrencyCode(chipBundle.getCurrency().getCurrencyCode())
                .withPrice(chipBundle.getPrice()).withChips(chipBundle.getChips()).buildVerifiedOrder();
    }
}
