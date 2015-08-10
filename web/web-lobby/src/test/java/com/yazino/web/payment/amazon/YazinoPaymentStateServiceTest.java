package com.yazino.web.payment.amazon;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.payment.googlecheckout.VerifiedOrderBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.AMAZON;
import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

public class YazinoPaymentStateServiceTest {
    private static final Platform PLATFORM = AMAZON;
    public static final String GAME_TYPE = "SLOTS";
    private static final BigDecimal CHIPS = TEN;
    private static final BigDecimal PRICE = ONE;
    private static final String CURRENCY = "USD";
    public static final String PRODUCT_ID = "product-id";
    private static final String INTERNAL_ID = "order-data";
    private static final String EXTERNAL_ID = "purchase-token";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final String PLAYER_NAME = "jack";
    private static final String PLAYER_EMAIL = "jack@here.com";
    private static final String CASHIER_NAME = "Amazon";
    public static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;
    public static final long PROMOTION_ID = 10L;
    private Partner partnerId = YAZINO;

    @Mock
    private WalletService walletService;
    @Mock
    private PaymentStateService paymentStateService;

    @Mock
    private ExternalTransactionBuilder transactionBuilder;
    private YazinoPaymentStateService underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new YazinoPaymentStateService(walletService, paymentStateService, transactionBuilder);
    }

    @Test
    public void shouldDoStartPayment() throws PaymentStateException {
        assertTrue(underTest.startPayment(buildPaymentContext(), buildVerifiedOrder(), CASHIER_NAME));

        verify(paymentStateService, times(1)).startPayment(CASHIER_NAME, INTERNAL_ID);
    }

    @Test
    public void shouldHandlePaymentExceptionWhenStartPaymentFails() throws PaymentStateException {
        willThrow(new PaymentStateException(PaymentState.Failed)).given(paymentStateService).startPayment(CASHIER_NAME, INTERNAL_ID);

        assertFalse(underTest.startPayment(buildPaymentContext(), buildVerifiedOrder(), CASHIER_NAME));

        verify(paymentStateService, times(1)).startPayment(CASHIER_NAME, INTERNAL_ID);
    }

    @Test
    public void shouldRecordFinishPayment() throws PaymentStateException {
        underTest.finishPayment(buildPaymentContext(), buildVerifiedOrder(), CASHIER_NAME);

        verify(paymentStateService, times(1)).finishPayment(CASHIER_NAME, INTERNAL_ID);
    }

    @Test
    public void shouldHandlePaymentExceptionWhenFinishPaymentFails() throws PaymentStateException {
        willThrow(new PaymentStateException(PaymentState.Failed)).given(paymentStateService).finishPayment(CASHIER_NAME, INTERNAL_ID);
        underTest.finishPayment(buildPaymentContext(), buildVerifiedOrder(), CASHIER_NAME);

        verify(paymentStateService, times(1)).finishPayment(CASHIER_NAME, INTERNAL_ID);
    }

    @Test
    public void shouldCallWalletServiceAndRecord() throws WalletServiceException {
        final PaymentContext paymentContext = buildPaymentContext();
        final VerifiedOrder order = buildVerifiedOrder();
        final Purchase purchaseRequest = buildPurchase(buildVerifiedOrder(), EXTERNAL_ID);
        final ExternalTransaction externalTransaction = buildExternalTransaction();
        when(transactionBuilder.build(CASHIER_NAME, PLATFORM, SUCCESS, paymentContext, null, purchaseRequest)).thenReturn(externalTransaction);

        underTest.recordPayment(paymentContext, order, CASHIER_NAME, purchaseRequest);

        verify(walletService, times(1)).record(externalTransaction);
    }

    @Test
    public void shouldCallWalletServiceAndRecordWithPromotionId() throws WalletServiceException {
        final PaymentContext paymentContext = buildPaymentContextWithPromotion();
        final VerifiedOrder order = buildVerifiedOrder();
        final Purchase purchaseRequest = buildPurchase(buildVerifiedOrder(), EXTERNAL_ID);
        final ExternalTransaction externalTransaction = buildExternalTransaction();
        when(transactionBuilder.build(CASHIER_NAME, PLATFORM, SUCCESS, paymentContext, PROMOTION_ID, purchaseRequest)).thenReturn(externalTransaction);

        underTest.recordPayment(paymentContext, order, CASHIER_NAME, purchaseRequest);

        verify(walletService, times(1)).record(externalTransaction);
    }

    @Test
    public void shouldSetStateToFailedWhenWalletServiceFails() throws Exception {
        final PaymentContext paymentContext = buildPaymentContext();
        final VerifiedOrder order = buildVerifiedOrder();
        final Purchase purchaseRequest = buildPurchase(buildVerifiedOrder(), EXTERNAL_ID);

        when(walletService.record(any(ExternalTransaction.class))).thenThrow(new WalletServiceException(""));

        underTest.recordPayment(paymentContext, order, CASHIER_NAME, purchaseRequest);

        verify(paymentStateService, times(1)).failPayment(CASHIER_NAME, INTERNAL_ID, false);
    }

    @Test
    public void shouldHandlePaymentExceptionWhenPaymentStateServiceFails() throws Exception {
        final PaymentContext paymentContext = buildPaymentContext();
        final VerifiedOrder order = buildVerifiedOrder();
        final Purchase purchaseRequest = buildPurchase(buildVerifiedOrder(), EXTERNAL_ID);

        when(walletService.record(any(ExternalTransaction.class))).thenThrow(new WalletServiceException(""));
        willThrow(new PaymentStateException(PaymentState.Failed)).given(paymentStateService).failPayment(CASHIER_NAME, INTERNAL_ID, false);

        assertFalse(underTest.recordPayment(paymentContext, order, CASHIER_NAME, purchaseRequest));
    }

    @Test
    public void shouldLogFailure() throws WalletServiceException, PaymentStateException {
        final ExternalTransaction externalTransaction = buildExternalTransaction();
        final PaymentContext paymentContext = buildPaymentContext();
        final VerifiedOrder order = buildVerifiedOrder();
        when(transactionBuilder.build(anyString(), any(Platform.class), any(ExternalTransactionStatus.class),
                any(PaymentContext.class), anyLong(), any(Purchase.class))).thenReturn(externalTransaction);

        final Purchase purchase = buildPurchase(buildVerifiedOrder(), EXTERNAL_ID);
        underTest.logFailure(paymentContext, order, CASHIER_NAME, PLATFORM, "", purchase);

        verify(transactionBuilder, times(1)).build(CASHIER_NAME, PLATFORM, FAILURE, paymentContext,
                null, purchase);
        verify(walletService, times(1)).record(any(ExternalTransaction.class));
        verify(paymentStateService, times(1)).failPayment(CASHIER_NAME, null, false);
    }


    private PaymentContext buildPaymentContext() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, null, partnerId);
    }

    private PaymentContext buildPaymentContextWithPromotion() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, PROMOTION_ID, partnerId);
    }

    private VerifiedOrder buildVerifiedOrder() {
        return new VerifiedOrderBuilder().withOrderId(INTERNAL_ID).withProductId(PRODUCT_ID).withCurrencyCode(CURRENCY)
                .withPrice(PRICE).withChips(CHIPS).buildVerifiedOrder();
    }

    private ExternalTransaction buildExternalTransaction() {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID).build();
    }

    private Purchase buildPurchase(VerifiedOrder order, final String externalId) {
        final Purchase purchase = new Purchase();
        purchase.setChips(order.getChips());
        purchase.setCurrencyCode(order.getCurrencyCode());
        purchase.setPrice(order.getPrice());
        purchase.setPurchaseId(order.getOrderId());
        purchase.setExternalId(externalId);
        return purchase;
    }
}
