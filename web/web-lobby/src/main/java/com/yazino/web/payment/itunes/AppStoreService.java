package com.yazino.web.payment.itunes;

import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.reference.Currency;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.Platform.IOS;

/**
 * Provides an api to the App Store to fulfil a App Store payment.
 */
public class AppStoreService {
    private static final Logger LOG = LoggerFactory.getLogger(AppStoreService.class);

    private final AppStoreConfiguration mConfiguration;
    private final TransactionalOrderProcessor<AppStoreOrder> mOrderProcessor;
    private final BuyChipsPromotionService mChipsPromotionService;

    private AppStoreApiIntegration mAppleAPI = new AppStoreApiIntegration();

    @Autowired
    public AppStoreService(final AppStoreConfiguration configuration,
                           final TransactionalOrderProcessor<AppStoreOrder> orderProcessor,
                           @Qualifier("buyChipsPromotionService") // qualified as there are 2 in context
                           final BuyChipsPromotionService chipsPromotionService) {
        Validate.noNullElements(new Object[]{configuration, orderProcessor, chipsPromotionService});
        mConfiguration = configuration;
        mOrderProcessor = orderProcessor;
        mChipsPromotionService = chipsPromotionService;
        LOG.info("Configured with {}", configuration);
    }

    /**
     * Fulfil the order.
     *
     * @param context, never null, must have a receipt
     * @return an {@link AppStoreOrder} object.
     * @throws java.io.IOException should apple not be contactable
     */
    public AppStoreOrder fulfilOrder(final AppStorePaymentContext context) throws Exception {
        LOG.debug("Fulfilling order with context {}", context);

        final AppStoreOrder order = mAppleAPI.retrieveOrder(context.getReceipt());
        fillOrderDetailsFromContext(context, order);

        final boolean validOrder = order.isValid();
        final boolean matchesContext = order.matchesContext(context);

        if (validOrder && matchesContext) {
            final PaymentOption option = findPaymentOptionForOrder(order);
            order.setPaymentOption(option);
            // done here because all lookup is based on the bundle identifier
            final String gameType = mConfiguration.lookupGameType(context.getGameType());
            order.setGameType(gameType);
            final boolean processed = mOrderProcessor.processOrder(order);
            order.setProcessed(processed);
        } else {
            final String cause;
            if (validOrder) {
                cause = "did not match context";
            } else {
                cause = "was an invalid order, context was";
            }
            LOG.debug("Order {} {} context {}", order, cause, context);
            if (validOrder) {
                LOG.warn("Context {} did not match apple's order {} - possible fraud", context, order);
            }
            fillOrderDetailsFromContextForFailedOrder(context, order);
        }
        return order;
    }

    public List<PaymentOption> paymentOptionsForPlayer(final BigDecimal playerId) {
        final Map<Currency, List<PaymentOption>> options
                = mChipsPromotionService.getBuyChipsPaymentOptionsFor(playerId, IOS);
        List<PaymentOption> usdOptions = options.get(Currency.USD);
        LOG.debug("Found [{}] USD options for player [{}]", usdOptions, playerId);
        return usdOptions; // we only deal in USD for IOS
    }

    public AppStoreConfiguration getConfiguration() {
        return mConfiguration;
    }

    final void setAppleAPI(final AppStoreApiIntegration appleAPI) {
        mAppleAPI = appleAPI;
    }

    final PaymentOption findPaymentOptionForOrder(final AppStoreOrder order) {
        final String internalIdentifier = mConfiguration.findInternalIdentifier(
                order.getGameType(), order.getProductId());
        if (internalIdentifier == null) {
            LOG.warn("Failed to find a payment option for product {} in game {} using config {}",
                    order.getProductId(), order.getGameType(), mConfiguration);
            return null;
        }

        final List<PaymentOption> options = paymentOptionsForPlayer(order.getPlayerId());

        PaymentOption bestMatch = null;

        for (PaymentOption option : options) {
            // a promotion does not have a string identifier so we need to match on its parent's identifier.
            // the internal identifier is something like IOS_USD3
            if (internalIdentifier.startsWith(option.getId())) {
                // check length for best match i.e. fix a bug where internalIdentifier
                // was IOS_USD30 and option was IOS_USD3 so was matching
                if (bestMatch == null || option.getId().length() > bestMatch.getId().length()) {
                    bestMatch = option;
                }
            }
        }

        return bestMatch;
    }

    // set them to what they are in the context so we don't send the client anything useful
    private void fillOrderDetailsFromContextForFailedOrder(final AppStorePaymentContext context,
                                                           final AppStoreOrder order) {
        order.setOrderId(context.getTransactionIdentifier());
        order.setProductId(context.getProductIdentifier());
        order.setPaymentState(PaymentState.Failed);
    }

    private void fillOrderDetailsFromContext(final AppStorePaymentContext context, final AppStoreOrder order) {
        order.setCurrency(context.getCurrency());
        order.setCashAmount(context.getCashAmount());
        order.setPlayerId(context.getPlayerId());
        order.setGameType(context.getGameType());
    }

}
