package com.yazino.web.payment.amazon;

import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static java.lang.String.format;

@Component
public class YazinoPaymentStateService implements YazinoPaymentState {
    private static final Logger LOG = LoggerFactory.getLogger(YazinoPaymentStateService.class);
    private WalletService walletService;
    private PaymentStateService paymentStateService;
    private ExternalTransactionBuilder transactionBuilder;

    @Autowired
    public YazinoPaymentStateService(final WalletService walletService,
                                     final PaymentStateService paymentStateService,
                                     final ExternalTransactionBuilder transactionBuilder) {
        this.walletService = walletService;
        this.paymentStateService = paymentStateService;
        this.transactionBuilder = transactionBuilder;
    }

    @Override
    public boolean startPayment(final PaymentContext paymentContext, final VerifiedOrder verifiedOrder, String cashierName) {
        try {
            paymentStateService.startPayment(cashierName, verifiedOrder.getOrderId());
        } catch (PaymentStateException e) {
            LOG.error("Failed to update payment state to Started for amazon order {}, player {}. Chips have NOT been credited.",
                    verifiedOrder.getOrderId(), paymentContext.getPlayerId(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean recordPayment(final PaymentContext paymentContext, final VerifiedOrder order, String cashierName, final Purchase purchaseRequest) {
        try {
            walletService.record(transactionBuilder.build("Amazon", Platform.AMAZON, SUCCESS, paymentContext, paymentContext.getPromotionId(), purchaseRequest));
        } catch (WalletServiceException e) {
            final String orderId = order.getOrderId();
            final BigDecimal playerId = paymentContext.getPlayerId();
            try {
                paymentStateService.failPayment(cashierName, orderId, false);
            } catch (PaymentStateException pe) {
                LOG.error("Failed to update payment state to PaymentCompletion for amazon order: {}, player: {}. Chips have been credited.",
                        orderId, playerId, e);
            }
            LOG.error("Failed to update player balance for amazon order. orderId: {}, player: {}.", orderId, playerId, e);
            return false;
        }
        return true;
    }

    @Override
    public void finishPayment(final PaymentContext paymentContext, final VerifiedOrder verifiedOrder, String cashierName) {
        try {
            paymentStateService.finishPayment(cashierName, verifiedOrder.getOrderId());
        } catch (PaymentStateException e) {
            LOG.error("Failed to update payment state to Finished for amazon order {}, player {}. Chips have been credited.",
                    verifiedOrder.getOrderId(), paymentContext.getPlayerId(), e);
        }
    }

    @Override
    public void logFailure(PaymentContext paymentContext,
                           VerifiedOrder order,
                           String cashierName,
                           Platform platform,
                           String reasonForFailure,
                           final Purchase purchase) {
        final BigDecimal playerId = paymentContext.getPlayerId();
        final String productId = order.getProductId();
        Long promotionId = paymentContext.getPromotionId();
        final String message = format("Failure for playerId=%s with productId=%s and promotionId=%s on platform %s. Reason for failure is %s",
                playerId, productId, promotionId, platform.name(), reasonForFailure);
        LOG.error(message);
        final ExternalTransaction externalTransaction = transactionBuilder.build(cashierName, platform, FAILURE,
                paymentContext, promotionId, purchase);
        record(externalTransaction);
        failPayment(order, cashierName, externalTransaction);
    }

    private void record(final ExternalTransaction txn) {
        try {
            walletService.record(txn);
        } catch (WalletServiceException e) {
            LOG.error("Unable to record transaction: {}", txn, e);
        }
    }

    private void failPayment(final VerifiedOrder order, final String cashierName, final ExternalTransaction externalTransaction) {
        try {
            paymentStateService.failPayment(cashierName, externalTransaction.getExternalTransactionId(), false);
        } catch (PaymentStateException e) {
            LOG.warn("Failed to update payment state to Failed for order {}", order.getOrderId());
        }
    }
}
