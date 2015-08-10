package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.service.QuietPlayerEmailer;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;

/**
 * Provides a default implementation of {@link OrderProcessor} which will award chips,
 * log successful and unsuccessful transactions, log promotions if the order is for a
 * promotion and email the player on a successful purchase.
 */
public class ChipAwardingOrderProcessor<T extends Order> implements OrderProcessor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ChipAwardingOrderProcessor.class);

    private final WalletService mWalletService;
    private final CommunityService mCommunityService;
    private final BuyChipsPromotionService mBuyChipsPromotionService;
    private final QuietPlayerEmailer mEmailer;
    private final OrderTransformer<T> mOrderTransformer;

    @Autowired
    public ChipAwardingOrderProcessor(final WalletService walletService,
                                      final CommunityService communityService,
                                      final BuyChipsPromotionService buyChipsPromotionService,
                                      final QuietPlayerEmailer emailer,
                                      final OrderTransformer<T> orderTransformer) {
        Validate.noNullElements(new Object[]{walletService, communityService,
                buyChipsPromotionService, emailer, orderTransformer});
        mWalletService = walletService;
        mCommunityService = communityService;
        mBuyChipsPromotionService = buyChipsPromotionService;
        mEmailer = emailer;
        mOrderTransformer = orderTransformer;
    }

    public boolean processOrder(final T order) throws WalletServiceException {
        final PaymentOption option = order.getPaymentOption();
        if (option == null) {
            throw new WalletServiceException("Could not find a payment option for order %s", order);
        }

        final ExternalTransaction transaction = mOrderTransformer.transform(order);

        switch (transaction.getStatus()) {
            case SUCCESS:
                recordSuccessfulOrder(transaction, order);
                break;
            case FAILURE:
                recordUnsuccessfulOrder(transaction);
                break;
            default:
                LOG.warn("Transaction {} had unexpected status {}",
                        transaction, transaction.getStatus().name());
                return false;
        }
        return true;
    }

    private void recordSuccessfulOrder(final ExternalTransaction transaction,
                                       final Order order) throws WalletServiceException {
        LOG.debug("Recording successful order {}", order);
        mWalletService.record(transaction);
        final PaymentOption option = order.getPaymentOption();
        final PaymentPreferences.PaymentMethod paymentMethod = order.getPaymentMethod();
        final PromotionPaymentOption promotion = findPromotion(option, paymentMethod);
        final BigDecimal playerId = order.getPlayerId();
        if (promotion != null) {
            LOG.debug("Logging promotional order {}, promotion {}", order, promotion);
            mBuyChipsPromotionService.logPlayerReward(playerId, promotion.getPromoId(),
                    paymentMethod, option.getId(), transaction.getMessageTimeStamp());
        }
        mCommunityService.asyncPublishBalance(playerId);
        sendEmailToPlayer(transaction, order);
    }

    private void recordUnsuccessfulOrder(final ExternalTransaction transaction) {
        LOG.debug("Recording unsuccessful transaction {}", transaction);
        try {
            mWalletService.record(transaction);
        } catch (WalletServiceException e) {
            LOG.error("Unable to record transaction: {}", transaction, e);
        }
    }

    private void sendEmailToPlayer(final ExternalTransaction transaction, final Order order) {
        final OrderEmailBuilder builder = new OrderEmailBuilder(order, transaction);
        mEmailer.quietlySendEmail(builder);
    }

    private PromotionPaymentOption findPromotion(final PaymentOption option,
                                                 final PaymentPreferences.PaymentMethod paymentMethod) {
        if (option != null && option.hasPromotion(paymentMethod)) {
            final PromotionPaymentOption promotion = option.getPromotion(paymentMethod);
            final BigDecimal promoChips = promotion.getPromotionChipsPerPurchase();
            if (promoChips.compareTo(option.getNumChipsPerPurchase()) != 0) {
                return promotion;
            }
        }
        return null;
    }

}
