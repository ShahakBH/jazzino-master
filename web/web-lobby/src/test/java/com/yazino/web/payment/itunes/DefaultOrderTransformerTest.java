package com.yazino.web.payment.itunes;

import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentState;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultOrderTransformerTest {

    private final long mTime = 1344266120055L;
    private final TimeSource mTimeSource = new SettableTimeSource(mTime);

    private final PlayerService mPlayerService = mock(PlayerService.class);
    private final DefaultOrderTransformer<Order> mTransformer = new DefaultOrderTransformer<Order>(mPlayerService);

    private final BigDecimal mAccountID = BigDecimal.valueOf(123);
    private final BigDecimal mPlayerID = BigDecimal.valueOf(999);
    private final String mTransactionIdentifier = "1000000054050596";
    private final String mProductIdentifier = "USD3_BUYS_5K";
    private final String mMessage = "{\"a\"=\"b\"}";
    private final Currency mCurrency = Currency.getInstance("USD");
    private final BigDecimal mCashAmount = BigDecimal.valueOf(8757353);
    private final BigDecimal mChipAmount = BigDecimal.valueOf(100000);
    private final String mCashierName = "TestCashier";
    private final String mGameType = "TestGame";

    private final Order mOrder = mock(Order.class);

    @Before
    public void setup() {
        mTransformer.setTimeSource(mTimeSource);
        when(mPlayerService.getAccountId(mPlayerID)).thenReturn(mAccountID);
        when(mOrder.getPlayerId()).thenReturn(mPlayerID);
        when(mOrder.getOrderId()).thenReturn(mTransactionIdentifier);
        when(mOrder.getProductId()).thenReturn(mProductIdentifier);
        when(mOrder.getCashier()).thenReturn(mCashierName);
        when(mOrder.getMessage()).thenReturn(mMessage);
        when(mOrder.getCurrency()).thenReturn(mCurrency);
        when(mOrder.getCashAmount()).thenReturn(mCashAmount);
        when(mOrder.getPaymentMethod()).thenReturn(PaymentPreferences.PaymentMethod.CREDITCARD);
        when(mOrder.getChipAmount()).thenReturn(mChipAmount);
        when(mOrder.getGameType()).thenReturn(mGameType);
        final PaymentOption option = new PaymentOption();
        option.addPromotionPaymentOption(new PromotionPaymentOption(
                PaymentPreferences.PaymentMethod.ITUNES,
                666l,
                BigDecimal.valueOf(100000l),
                "your mum",
                "your mum"));
        when(mOrder.getPaymentOption()).thenReturn(option);
    }

    @Test
    public void shouldHaveCorrectAccountId() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mAccountID, transaction.getAccountId());
    }

    @Test
    public void shouldHaveCorrectInternalTransactionId() throws Exception {
        DefaultOrderTransformer.setAtomicLong(new AtomicLong(0));
        String expected = mCashierName + "_" + mProductIdentifier + "_123_20120806T161520_0";
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(expected, transaction.getInternalTransactionId());
    }

    @Test
    public void shouldHaveCorrectExternalTransactionId() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mTransactionIdentifier, transaction.getExternalTransactionId());
    }

    @Test
    public void shouldHaveCorrectCreditCardObscuredMessage() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mMessage, transaction.getCreditCardObscuredMessage());
    }

    @Test
    public void shouldHaveCorrectMessageTimestamp() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mTime, transaction.getMessageTimeStamp().getMillis());
    }

    @Test
    public void shouldHaveCorrectCurrency() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mCurrency, transaction.getCurrency());
    }

    @Test
    public void shouldHaveCorrectCashAmount() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mCashAmount, transaction.getAmountCash());
    }

    @Test
    public void shouldHaveCorrectChipsAmount() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mChipAmount, transaction.getAmountChips());
    }

    @Test
    public void shouldHaveCorrectCashierName() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mCashierName, transaction.getCashierName());
    }

    @Test
    public void shouldHaveFailedStateWhenStateIsFailed() throws Exception {
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Failed);
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(ExternalTransactionStatus.FAILURE, transaction.getStatus());
    }

    @Test
    public void shouldHaveFailedStateWhenStateIsUnknown() throws Exception {
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Unknown);
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(ExternalTransactionStatus.FAILURE, transaction.getStatus());
    }

    @Test
    public void shouldHaveFailedStateWhenStateIsFinished() throws Exception {
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Finished);
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(ExternalTransactionStatus.FAILURE, transaction.getStatus());
    }

    @Test
    public void shouldHaveSuccessStatusWhenPaymentStateIsStarted() throws Exception {
        when(mOrder.getPaymentState()).thenReturn(PaymentState.Started);
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(ExternalTransactionStatus.SUCCESS, transaction.getStatus());
    }

    @Test
    public void shouldHaveCorrectExternalTransactionType() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(ExternalTransactionType.DEPOSIT, transaction.getType());
    }


    @Test
    public void shouldHaveCorrectGameType() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(mGameType, transaction.getGameType());
    }

    @Test
    public void shouldHaveCorrectObscuredCreditCardNumber() throws Exception {
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals("x-x-x", transaction.getObscuredCreditCardNumber());
    }

    @Test
    public void promoIdShouldBeAccessibleFromPaymentOption(){
        ExternalTransaction transaction = mTransformer.transform(mOrder);
        assertEquals(new Long(666), transaction.getPromoId());

    }
}
