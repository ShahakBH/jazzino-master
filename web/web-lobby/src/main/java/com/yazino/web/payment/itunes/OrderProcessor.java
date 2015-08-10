package com.yazino.web.payment.itunes;

/**
 * Describes a class which processes orders.
 */
public interface OrderProcessor<T extends Order> {

    /**
     * Process the order T.
     * @param order never null
     * @return true if the order was processed as expected, false otherwise
     * @throws Exception should processing fail
     */
    boolean processOrder(T order) throws Exception;
}
