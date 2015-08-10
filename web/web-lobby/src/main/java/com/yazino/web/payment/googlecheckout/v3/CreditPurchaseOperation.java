package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.android.AndroidPaymentStateDetails;
import com.yazino.platform.payment.android.AndroidPaymentStateException;
import com.yazino.platform.payment.android.AndroidPaymentStateService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static com.yazino.web.payment.PurchaseStatus.FAILED;

@Component
public class CreditPurchaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(CreditPurchaseOperation.class);

    private final AndroidPaymentStateService paymentStateService;
    private final WalletService walletService;
    private final CommunityService communityService;
    private final PlayerService playerService;
    private final PlayerProfileService playerProfileService;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final QuietPlayerEmailer emailer;

    @Autowired
    public CreditPurchaseOperation(@Qualifier("safeBuyChipsPromotionService") BuyChipsPromotionService buyChipsPromotionService,
                                   AndroidPaymentStateService paymentStateService,
                                   WalletService walletService,
                                   CommunityService communityService,
                                   PlayerService playerService,
                                   PlayerProfileService playerProfileService,
                                   QuietPlayerEmailer emailer) {
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.paymentStateService = paymentStateService;
        this.walletService = walletService;
        this.communityService = communityService;
        this.playerService = playerService;
        this.playerProfileService = playerProfileService;
        this.emailer = emailer;
    }

    public void creditPurchase(AndroidPaymentStateDetails paymentRecord, Order order, ChipBundle bundle) throws PurchaseException {
        lockPayment(paymentRecord);
        creditPlayer(order, bundle, paymentRecord);
    }

    private void lockPayment(AndroidPaymentStateDetails paymentDetails) throws PurchaseException {
        try {
            paymentStateService.createCreditPurchaseLock(paymentDetails.getPlayerId(), paymentDetails.getInternalTransactionId());
        } catch (AndroidPaymentStateException e) {
            throw new PurchaseException(FAILED, false, "Failed to credit purchase. Could not mark payment state as CREDITING", e);
        }
    }

    private void creditPlayer(Order order, ChipBundle bundle, AndroidPaymentStateDetails paymentRecord) throws PurchaseException {
        final BigDecimal playerId = paymentRecord.getPlayerId();
        try {
            final Long promoId = paymentRecord.getPromoId();
            final ExternalTransaction txn = buildExternalTransaction(paymentRecord, bundle, "", new DateTime(), ExternalTransactionStatus.SUCCESS, order.getOrderId());
            walletService.record(txn);
            String internalTransactionId = paymentRecord.getInternalTransactionId();
            try {
                paymentStateService.markPurchaseAsCredited(playerId, internalTransactionId);
            } catch (AndroidPaymentStateException e) {
                // log error but continue as the user has been credited chips and so we should this as successful
                LOG.error("Failed to update payment state to CREDITED for android purchase, internalTransactionId {}, player {}. Chips have been credited.",
                        internalTransactionId, playerId, e);
            }
            if (promoId != null) {
                logPromotion(order.getPurchaseTime(), bundle, promoId, playerId);
            }
            communityService.asyncPublishBalance(playerId);
            notifyPlayer(paymentRecord, order, bundle);
        } catch (WalletServiceException e) {
            handleWalletException(paymentRecord, e);
            throw new PurchaseException(FAILED, false, "Failed to credit player with chips", e);
        }
    }

    // TODO duplicated in service
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

    private void handleWalletException(AndroidPaymentStateDetails paymentStateDetails,
                                       final WalletServiceException e) {
        final BigDecimal playerId = paymentStateDetails.getPlayerId();
        String internalTransactionId = paymentStateDetails.getInternalTransactionId();
        try {
            paymentStateService.markPurchaseAsFailed(playerId, internalTransactionId);
        } catch (AndroidPaymentStateException e1) {
            LOG.error("Failed to update payment state to FAILED for android purchase, internalTransactionId {}, player {}. Chips have been credited."
                    , internalTransactionId, playerId, e1);
        }
        LOG.error("Failed to update player balance for android purchase, internalTransactionId {}, player {}."
                , internalTransactionId, playerId, e);
    }


    private void logPromotion(DateTime messageTimeStamp, ChipBundle bundle, Long promoId, BigDecimal playerId) {
        buyChipsPromotionService.logPlayerReward(playerId, promoId, bundle.getDefaultChips(), bundle.getChips(), GOOGLE_CHECKOUT, messageTimeStamp);
    }

    private void notifyPlayer(AndroidPaymentStateDetails paymentStateDetails,
                              Order order,
                              ChipBundle chipBundle) {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        PlayerProfile profile = playerProfileService.findByPlayerId(paymentStateDetails.getPlayerId());

        builder.withEmailAddress(profile.getEmailAddress());
        builder.withFirstName(profile.getFirstName());
        builder.withPurchasedChips(chipBundle.getChips());
        builder.withCurrency(chipBundle.getCurrency());
        builder.withCost(chipBundle.getPrice());
        builder.withPaymentDate(order.getPurchaseTime().toDate());
        builder.withCardNumber("");
        builder.withPaymentId(order.getOrderId());
        builder.withPaymentEmailBodyTemplate(PaymentEmailBodyTemplate.GoogleCheckout);
        emailer.quietlySendEmail(builder);
    }

}
