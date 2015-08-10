package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.junit.Test;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;

import static com.yazino.web.payment.itunes.AppStoreTestUtils.toMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ChipAwardingOrderProcessorTest {

    private final WalletService mWalletService = mock(WalletService.class);
    private final CommunityService mCommunityService = mock(CommunityService.class);
    private final BuyChipsPromotionService mChipsService = mock(BuyChipsPromotionService.class);
    private final QuietPlayerEmailer mEmailer = mock(QuietPlayerEmailer.class);
    private final OrderTransformer<Order> mTransformer = mock(OrderTransformer.class);

    private final ChipAwardingOrderProcessor<Order> mProcessor =
            new ChipAwardingOrderProcessor<Order>(mWalletService, mCommunityService, mChipsService, mEmailer, mTransformer);

    @Test
    public void shouldPostExternalTransactionOnSuccessfulOrder() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.SUCCESS);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertTrue(processed);
        verify(mWalletService).record(transaction);
    }

    @Test
    public void shouldLogExternalTransactionOnFailedOrder() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.FAILURE);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertTrue(processed);
        verify(mWalletService).record(transaction);
    }

    @Test
    public void shouldLogPromotionOnSuccesfulOrder() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.SUCCESS);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        PromotionPaymentOption promotion = new PromotionPaymentOption(PaymentPreferences.PaymentMethod.ITUNES, 123L, BigDecimal.valueOf(20), null, null);
        paymentOption.setPromotions(toMap(PaymentPreferences.PaymentMethod.ITUNES, promotion));
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertTrue(processed);
        verify(mChipsService).logPlayerReward(any(BigDecimal.class), any(Long.class), any(PaymentPreferences.PaymentMethod.class), anyString(), any(DateTime.class));
    }

    @Test
    public void shouldPublishBalanceOnSuccessfulOrder() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.SUCCESS);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        order.setPlayerId(BigDecimal.ONE);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertTrue(processed);
        verify(mCommunityService).asyncPublishBalance(BigDecimal.ONE);
    }

    @Test
    public void shouldSendEmailOnSuccesfulOrder() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.SUCCESS);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertTrue(processed);
        verify(mEmailer).quietlySendEmail(any(OrderEmailBuilder.class));
    }

    @Test
    public void shouldReturnFalseWhenExternalTransactionStatusIsNotSuccessOrFailure() throws Exception {
        ExternalTransaction transaction = buildTransaction(ExternalTransactionStatus.REQUEST);
        when(mTransformer.transform(any(Order.class))).thenReturn(transaction);
        Order order = new Order("Test", PaymentPreferences.PaymentMethod.ITUNES);
        PaymentOption paymentOption = buildPaymentOption("IOS_USD8", BigDecimal.TEN);
        order.setPaymentOption(paymentOption);
        boolean processed = mProcessor.processOrder(order);
        assertFalse(processed);
    }

    private static ExternalTransaction buildTransaction(ExternalTransactionStatus status) {
        ExternalTransaction transaction = mock(ExternalTransaction.class);
        when(transaction.getStatus()).thenReturn(status);
        return transaction;
    }


    private static PaymentOption buildPaymentOption(String id, BigDecimal chips) {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setNumChipsPerPurchase(BigDecimal.TEN);
        paymentOption.setId("IOS_USD8");
        return paymentOption;
    }




}
