package com.yazino.web.payment.itunes;

import com.yazino.platform.payment.PaymentState;
import org.junit.Test;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.*;

public class AppStoreChipPurchaseTransformerTest {

    private final AppStoreChipPurchaseTransformer mTransformer = new AppStoreChipPurchaseTransformer();

    @Test
    public void shouldHaveFailedFlagIfStatusCodeIsZeroAndPaymentStateIsStarted() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        order.setPaymentState(PaymentState.Started);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertFalse(result.isSuccess());
    }

    @Test
    public void shouldHaveErrorWhenStatusCodeIsZeroAndPaymentStateIsStarted() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        order.setPaymentState(PaymentState.Started);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertNotNull(result.getError());
    }

    @Test
    public void shouldNotHaveSuccessFlagIfStatusCodeIsNonZero() throws Exception {
        AppStoreOrder order = new AppStoreOrder(1);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertFalse(result.isSuccess());
    }

    @Test
    public void shouldHaveErrorIfStatusCodeIsNonZero() throws Exception {
        AppStoreOrder order = new AppStoreOrder(1);
        order.setCurrency(Currency.getInstance("GBP"));
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertNotNull(result.getError());
    }

    @Test
    public void shouldHaveCorrectErrorMessageWhenNonZeroStatus() throws Exception {
        AppStoreOrder order = toUnsuccessfulOrder();
        order.setOrderId("12345");
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals("Chip purchase failed for order: 12345", result.getError());
    }

    @Test
    public void shouldHaveSuccessFlagWhenPaymentStateIsFinishedAndIsProcessed() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Finished, true, 100);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertTrue(result.isSuccess());
    }

    @Test
    public void shouldHaveSuccessFlagWhenPaymentStateIsFinishedAndIsNotProcessed() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Finished, false, 100);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertTrue(result.isSuccess());
    }

    @Test
    public void shouldHaveCorrectErrorWhenOrderIsUnknownButNotProcessed() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Unknown, false, 100);
        order.setOrderId("123");
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals("Chip purchase failed for order: 123", result.getError());
    }

    @Test
    public void shouldHaveCorrectErrorWhenOrderIsFailedButNotProcessed() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Failed, false, 100);
        order.setOrderId("123");
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals("Chip purchase failed for order: 123",result.getError());
    }

    @Test
    public void shouldHaveCorrectErrorWhenOrderIsUnknownAndProcessed() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Unknown, true, 100);
        order.setOrderId("123");
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals("Chip purchase failed for order: 123", result.getError());
    }

    @Test
    public void shouldHaveFailedFlagWhenPaymentStateIsFailed() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        order.setPaymentState(PaymentState.Failed);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertFalse(result.isSuccess());
    }

    @Test
    public void shouldHaveCashAmount() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        BigDecimal cashAmount = BigDecimal.TEN;
        order.setCashAmount(cashAmount);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals(cashAmount, result.getCashAmount());
    }

    @Test
    public void shouldHaveChipAmount() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        BigDecimal chipAmount = BigDecimal.valueOf(1999);
        PaymentOption option = new PaymentOption();
        option.setNumChipsPerPurchase(chipAmount);
        order.setPaymentOption(option);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals(chipAmount, result.getChipAmount());
    }

    @Test
    public void shouldHaveCurrencyCode() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        Currency currency = Currency.getInstance("JPY");
        order.setCurrency(currency);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals(currency.getCurrencyCode(), result.getCurrencyCode());
    }

    @Test
    public void shouldHaveTransactionIdentifier() throws Exception {
        AppStoreOrder order = new AppStoreOrder(0);
        String transactionIdentifier = "907698456345";
        order.setOrderId(transactionIdentifier);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertEquals(transactionIdentifier, result.getTransactionIdentifier());
    }

    @Test
    public void shouldHaveCorrectMessageWhenPaymentIsSuccessful() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Finished, true, 100000);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertTrue(result.isSuccess());
        assertEquals("Successfully processed chip purchase, 100,000 chips have been added to your account.", result.getMessage());
    }

    @Test
    public void shouldHaveCorrectMessageWhenOrderHasBeenProcessee() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Finished, true, 100000);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertTrue(result.isSuccess());
        assertEquals("Successfully processed chip purchase, 100,000 chips have been added to your account.", result.getMessage());
    }

    @Test
    public void shouldHaveCorrectMessageWhenChipsAlreadyAwarded() throws Exception {
        AppStoreOrder order = toSuccessfulOrder(PaymentState.Finished, false, 100000);
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertTrue(result.isSuccess());
        assertEquals("Successfully processed chip purchase, 100,000 chips have been added to your account.", result.getMessage());
    }

    @Test
    public void shouldHaveCorrectMessageWhenInvalidOrder() throws Exception {
        AppStoreOrder order = toUnsuccessfulOrder();
        AppStoreChipPurchaseResult result = mTransformer.transform(order);
        assertFalse(result.isSuccess());
        assertEquals("Unable to process chip purchase because Apple could not verify your payment.", result.getMessage());
    }

    private static AppStoreOrder toSuccessfulOrder(PaymentState state, boolean processed, int chips) {
        AppStoreOrder order = new AppStoreOrder(0);
        order.setPaymentState(state);
        order.setProcessed(processed);
        PaymentOption option = new PaymentOption();
        option.setNumChipsPerPurchase(BigDecimal.valueOf(chips));
        order.setPaymentOption(option);
        return order;
    }
    
    private static AppStoreOrder toUnsuccessfulOrder() {
        AppStoreOrder order = new AppStoreOrder(1);
        order.setPaymentState(PaymentState.Failed);
        order.setProcessed(false);
        return order;
    }

}
