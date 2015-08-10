package com.yazino.web.service;

import com.yazino.bi.payment.PaymentOptionService;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.reference.Currency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import strata.server.lobby.api.promotion.InGameMessage;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static org.apache.commons.lang3.Validate.notNull;

@Service("safeBuyChipsPromotionService")
public class SafeBuyChipsPromotionService implements BuyChipsPromotionService {
    private static final Logger LOG = LoggerFactory.getLogger(SafeBuyChipsPromotionService.class);

    private final BuyChipsPromotionService delegate;
    private final PaymentOptionService paymentOptionService;

    @Autowired
    public SafeBuyChipsPromotionService(@Qualifier("buyChipsPromotionService")
                                        final BuyChipsPromotionService delegate,
                                        final PaymentOptionService paymentOptionService) {
        notNull(delegate, "delegate is null");
        notNull(paymentOptionService, "paymentOptionService is null");

        this.delegate = delegate;
        this.paymentOptionService = paymentOptionService;
    }

    @Override
    public Map<Currency, List<PaymentOption>> getBuyChipsPaymentOptionsFor(
            final BigDecimal playerId,
            final Platform platform) {
        try {
            return delegate.getBuyChipsPaymentOptionsFor(playerId, platform);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return paymentOptionService.getAllDefaults(platform);
        }
    }

    @Override
    public InGameMessage getInGameMessageFor(final BigDecimal playerId) {
        try {
            return delegate.getInGameMessageFor(playerId);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return null;
        }
    }

    @Override
    public InGameMessage getInGameMessageFor(final BigDecimal playerId, Platform platform) {
        try {
            return delegate.getInGameMessageFor(playerId, platform);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return null;
        }
    }

    @Override
    public PaymentOption getPaymentOptionFor(final BigDecimal playerId,
                                             final Long promotionId,
                                             final PaymentPreferences.PaymentMethod paymentMethod,
                                             final String paymentOptionId) {
        try {
            return delegate.getPaymentOptionFor(playerId, promotionId, paymentMethod, paymentOptionId);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return null;
        }
    }

    @Override
    public PaymentOption getDefaultPaymentOptionFor(final String paymentOptionId, Platform platform) {
        try {
            return delegate.getDefaultPaymentOptionFor(paymentOptionId, platform);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return paymentOptionService.getDefault(paymentOptionId, WEB);
        }
    }

    @Override
    public PaymentOption getDefaultFacebookPaymentOptionFor(String paymentOptionId) {
        try {
            return delegate.getDefaultFacebookPaymentOptionFor(paymentOptionId);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed. Returning default value", e);
            return paymentOptionService.getDefault(paymentOptionId, FACEBOOK_CANVAS);
        }
    }

    @Override
    public void logPlayerReward(final BigDecimal playerId,
                                final Long promotionId,
                                final PaymentPreferences.PaymentMethod paymentMethod,
                                final String paymentOptionId, final DateTime awardDate) {
        try {
            delegate.logPlayerReward(playerId, promotionId, paymentMethod, paymentOptionId, awardDate);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed.");
        }
    }

    @Override
    public void logPlayerReward(BigDecimal playerId, Long promotionId, BigDecimal defaultChips, BigDecimal promoChips,
                                PaymentPreferences.PaymentMethod paymentMethod, DateTime awardDate) {
        try {
            delegate.logPlayerReward(playerId, promotionId, defaultChips, promoChips, paymentMethod, awardDate);
        } catch (Exception e) {
            LOG.error("Delegate invocation failed.", e);
        }
    }

    @Override
    public Boolean hasPromotion(final BigDecimal playerId, final Platform platform) {
        try {
            return delegate.hasPromotion(playerId, platform);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }
}
