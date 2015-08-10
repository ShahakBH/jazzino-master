package com.yazino.web.payment.itunes;

import com.yazino.platform.Partner;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppStoreOrderTest {

    private final AppStoreOrder mOrder = new AppStoreOrder(0);

    @Test
    public void shouldReturnValidWhenStatusCodeIsZero() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        assertTrue(order.isValid());
    }

    @Test
    public void shouldReturnInValidWhenStatusCodeIsNotZero() throws Exception {
        assertFalse(new AppStoreOrder(-1).isValid());
        assertFalse(new AppStoreOrder(5).isValid());
        assertFalse(new AppStoreOrder(2001).isValid());
        assertFalse(new AppStoreOrder(Integer.MAX_VALUE).isValid());
        assertFalse(new AppStoreOrder(Integer.MIN_VALUE).isValid());
    }

    @Test
    public void shouldNotMatchContextWhenOrdersProductIdentifierIsNull() throws Exception {
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers("USD_123", transaction);
        mOrder.setProductId(null);
        mOrder.setOrderId(transaction);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenOrdersTransactionIdentifierIsNull() throws Exception {
        String product = "USD_123";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, "323213");
        mOrder.setProductId(product);
        mOrder.setOrderId(null);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenContextsProductIdentifierIsNull() throws Exception {
        String product = "USD_123";
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(null, transaction);
        mOrder.setProductId(product);
        mOrder.setOrderId(transaction);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenContextsTransactionIdentifierIsNull() throws Exception {
        String product = "USD_123";
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, null);
        mOrder.setProductId(product);
        mOrder.setOrderId(transaction);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenBothProductIdentifiersAreNull() throws Exception {
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(null, transaction);
        mOrder.setProductId(null);
        mOrder.setOrderId(transaction);
        assertFalse(mOrder.matchesContext(context));
    }
    
    @Test
    public void shouldNotMatchContextWhenBothTransactionIdentifiersAreNull() throws Exception {
        String product = "USD_123";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, null);
        mOrder.setProductId(product);
        mOrder.setOrderId(null);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenProductIdentifiersDontMatch() throws Exception {
        String product = "USD_123";
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, transaction);
        mOrder.setProductId(product+"123");
        mOrder.setOrderId(transaction);
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldNotMatchContextWhenTransactionIdentifiersDontMatch() throws Exception {
        String product = "USD_123";
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, transaction);
        mOrder.setProductId(product);
        mOrder.setOrderId(transaction+"123");
        assertFalse(mOrder.matchesContext(context));
    }

    @Test
    public void shouldMatchContextWhenTransactionAndProductIdentifiersMatch() throws Exception {
        String product = "USD_123";
        String transaction = "323213";
        AppStorePaymentContext context = contextWithProductAndTransactionIdentifiers(product, transaction);
        mOrder.setProductId(product);
        mOrder.setOrderId(transaction);
        assertTrue(mOrder.matchesContext(context));
    }

    private static AppStorePaymentContext contextWithProductAndTransactionIdentifiers(String product, String transaction) {
        return new AppStorePaymentContext(BigDecimal.TEN, null, "SLOTS", "545423532ffdsfdsfds", BigDecimal.valueOf(199), Currency.getInstance("USD"), transaction, product,
                Partner.YAZINO);
    }

}
