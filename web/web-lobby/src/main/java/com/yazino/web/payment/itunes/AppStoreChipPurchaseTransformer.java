package com.yazino.web.payment.itunes;

import com.yazino.platform.payment.PaymentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;

/**
 * Transforms an {@link AppStoreOrder} into an {@link AppStoreChipPurchaseResult}.
 */
public class AppStoreChipPurchaseTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(AppStoreChipPurchaseTransformer.class);

    private NumberFormat mNumberFormatter = defaultFormatter();

    public AppStoreChipPurchaseResult transform(final AppStoreOrder order) {
        final AppStoreChipPurchaseResult result = new AppStoreChipPurchaseResult();
        result.setCashAmount(safeBigDecimal(order.getCashAmount()));
        result.setChipAmount(safeBigDecimal(order.getChipAmount()));
        if (order.getCurrency() != null) {
            result.setCurrencyCode(order.getCurrency().getCurrencyCode());
        }
        final String orderId = order.getOrderId();
        result.setTransactionIdentifier(orderId);
        final PaymentState paymentState = order.getPaymentState();
        final boolean validOrder = order.isValid();
        final boolean processed = order.isProcessed();
        final boolean finished = paymentState == PaymentState.Finished;
        if (validOrder && finished) {
            result.setSuccess(true);
            final String formattedChips = mNumberFormatter.format(result.getChipAmount().doubleValue());
            result.setMessage(String.format(
                    "Successfully processed chip purchase, %s chips have been added to your account.", formattedChips));
            if (!processed) {
                LOG.warn("Order {} was valid and finished so had previously been processed", orderId);
            }
        } else {
            result.setSuccess(false);
            result.setError("Chip purchase failed for order: " + orderId);
            String cause;
            if (!validOrder) {
                cause = "Apple could not verify your payment.";
            } else {
                cause = "a problem occurred, your transaction id is " + orderId;
            }
            result.setMessage(String.format("Unable to process chip purchase because %s", cause));
        }
        return result;
    }

    private static BigDecimal safeBigDecimal(final BigDecimal value) {
        if (value != null) {
            return value;
        } else {
            return BigDecimal.ZERO;
        }
    }

    private static NumberFormat defaultFormatter() {
        final NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat;
    }


}
