package com.yazino.web.payment.itunes;

import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides a default implementation of {@link OrderProcessor} which manages the
 * order's payment state and records the order if the state is started.
 * It will also attempt to finish and fail payments should the recording of the order fail.
 */
public class TransactionalOrderProcessor<T extends Order> implements OrderProcessor<T> {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalOrderProcessor.class);

    private final PaymentStateService mPaymentStateService;
    private final OrderProcessor<T> mOrderProcessor;

    @Autowired
    public TransactionalOrderProcessor(final PaymentStateService paymentStateService,
                                       final OrderProcessor<T> orderProcessor) {
        Validate.noNullElements(new Object[] {paymentStateService, orderProcessor});
        mPaymentStateService = paymentStateService;
        mOrderProcessor = orderProcessor;
    }

    @Override
    public boolean processOrder(final T order) {
        PaymentState state;
        try {
            state = startOrder(order);
            order.setPaymentState(state);
            switch (state) {
                case Started: return submitOrder(order);
                default: LOG.warn("Order {} had unexpected payment state {} so won't be delegated to {}",
                        new Object[] {order.getOrderId(), order.getPaymentState().name(),
                                mOrderProcessor.getClass().getSimpleName()});
            }
        } catch (PaymentStateException e) {
            state = e.getState();
            order.setPaymentState(state);
            LOG.debug("Failed to start order {} as it had state {}", order.getOrderId(), state.name());
        }
        return false;
    }

    private boolean submitOrder(final T order) {
        boolean processed;
        PaymentState state;
        try {
            processed = mOrderProcessor.processOrder(order);
            state = orderCompleted(order);
        } catch (Exception e) {
            LOG.error("Processing failed for order: {}", order.getOrderId(), e);
            state = orderFailed(order);
            processed = false;
        }
        order.setPaymentState(state);
        return processed;
    }

    private PaymentState startOrder(final T order) throws PaymentStateException {
        LOG.debug("Starting transaction for order {}", order.getOrderId());
        mPaymentStateService.startPayment(order.getCashier(), order.getOrderId());
        return PaymentState.Started;
    }

    private PaymentState orderCompleted(final T order) {
        try {
            mPaymentStateService.finishPayment(order.getCashier(), order.getOrderId());
            LOG.debug("Finishing transaction for {}", order.getOrderId());
            return PaymentState.Finished;
        } catch (PaymentStateException e) {
            LOG.warn("Failed to finish transaction for order {}", order.getOrderId());
            return e.getState();
        }
    }

    private PaymentState orderFailed(final T order) {
        try {
            mPaymentStateService.failPayment(order.getCashier(), order.getOrderId());
            LOG.debug("Failing transaction for order {}", order.getOrderId());
            return PaymentState.Failed;
        } catch (PaymentStateException e) {
            LOG.warn("Failed to fail payment for order {}", order.getOrderId());
            return e.getState();
        }
    }

}
