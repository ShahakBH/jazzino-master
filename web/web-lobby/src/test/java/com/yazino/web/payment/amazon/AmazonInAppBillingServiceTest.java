package com.yazino.web.payment.amazon;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.googlecheckout.VerifiedOrder;
import com.yazino.web.payment.googlecheckout.VerifiedOrderBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.AMAZON;
import static com.yazino.web.payment.PurchaseStatus.FAILED;
import static com.yazino.web.payment.PurchaseStatus.SUCCESS;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class AmazonInAppBillingServiceTest {
    public static final String USER_ID = "user-id";
    public static final String GAME_TYPE = "SLOTS";
    private static final String PURCHASE_TOKEN = "purchase-token";
    private static final BigDecimal DEFAULT_CHIPS = TEN;
    private static final BigDecimal PROMOTED_CHIPS = BigDecimal.valueOf(50L);
    private static final BigDecimal PRICE = ONE;
    private static final String CURRENCY = "USD";
    public static final String PRODUCT_ID = "product-id";
    private static final String ORDER_ID = "order-data";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final String PLAYER_NAME = "jack";
    private static final String PLAYER_EMAIL = "jack@here.com";
    private static final String MESSAGE = "message";
    private static final String CASHIER_NAME = "Amazon";
    public static final Long PROMOTION_ID = 123345L;

    @Mock
    private AmazonReceiptVerificationService receiptVerificationServer;
    @Mock
    private CommunityService communityService;
    @Mock
    private YazinoPaymentState yazinoPaymentState;
    @Mock
    private PlayerNotifier playerNotifier;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ChipBundleResolver chipBundleResolver;
    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;


    private AmazonInAppBillingService underTest;

    @Before
    public void init() {

        MockitoAnnotations.initMocks(this);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000000L);
        underTest = new AmazonInAppBillingService(receiptVerificationServer,
                communityService,
                yazinoPaymentState,
                playerNotifier,
                chipBundleResolver, buyChipsPromotionService);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(new ChipBundle(PRODUCT_ID,
                PROMOTED_CHIPS,
                DEFAULT_CHIPS,
                PRICE,
                java.util.Currency.getInstance(CURRENCY)));
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnSuccessWhenAValidPayment() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenReturn(VerificationResult.VALID);
        when(yazinoPaymentState.startPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString())).thenReturn(true);
        when(yazinoPaymentState.recordPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString(), any(Purchase.class))).thenReturn(true);

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(SUCCESS, purchase.getStatus());
        verify(playerNotifier, times(1)).emailPlayer(any(PaymentContext.class), any(VerifiedOrder.class), any(PaymentEmailBodyTemplate.class));
        verify(communityService, times(1)).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldRecordPromotionsWhenSuccessfulPaymentHasPromotionId() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenReturn(VerificationResult.VALID);
        when(yazinoPaymentState.startPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString())).thenReturn(true);
        when(yazinoPaymentState.recordPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString(), any(Purchase.class))).thenReturn(true);

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(SUCCESS, purchase.getStatus());
        verify(playerNotifier, times(1)).emailPlayer(any(PaymentContext.class), any(VerifiedOrder.class), any(PaymentEmailBodyTemplate.class));
        verify(communityService, times(1)).asyncPublishBalance(PLAYER_ID);
        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, PROMOTION_ID, DEFAULT_CHIPS, PROMOTED_CHIPS, PaymentPreferences.PaymentMethod.AMAZON, new DateTime());
    }

    @Test
    public void shouldFailPurchaseWhenAmazonVerificationFails() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenReturn(VerificationResult.TOKEN_EXPIRED);

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(FAILED, purchase.getStatus());
        verifyZeroInteractions(communityService);
        verifyZeroInteractions(playerNotifier);
        verify(yazinoPaymentState, times(1)).logFailure(any(PaymentContext.class), any(VerifiedOrder.class), anyString(), any(Platform.class), anyString(),
                any(Purchase.class));
    }

    @Test
    public void shouldFailPurchaseWhenStartPaymentFails() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenReturn(VerificationResult.VALID);
        when(yazinoPaymentState.startPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString())).thenReturn(false);

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(FAILED, purchase.getStatus());
        verifyZeroInteractions(communityService);
        verifyZeroInteractions(playerNotifier);
    }

    @Test
    public void shouldFailPurchaseWhenPaymentRecordFails() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenReturn(VerificationResult.VALID);
        when(yazinoPaymentState.startPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString())).thenReturn(true);
        when(yazinoPaymentState.recordPayment(any(PaymentContext.class), any(VerifiedOrder.class), anyString(), any(Purchase.class))).thenReturn(false);

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(FAILED, purchase.getStatus());
        verifyZeroInteractions(communityService);
    }

    @Test
    public void shouldHandleIOException() throws IOException {
        when(receiptVerificationServer.verify(USER_ID, PURCHASE_TOKEN)).thenThrow(new IOException());

        Purchase purchase = underTest.creditPurchase(getPaymentContext(), USER_ID, PURCHASE_TOKEN, PRODUCT_ID, ORDER_ID);

        assertEquals(FAILED, purchase.getStatus());
        verifyZeroInteractions(communityService);
    }

    @Test
    public void shouldLogFailedTransaction() {
        final VerifiedOrder order = buildVerifiedOrder(ORDER_ID, PRODUCT_ID, PROMOTED_CHIPS);
        underTest.logFailedTransaction(getPaymentContext(), PRODUCT_ID, ORDER_ID, MESSAGE);

        verify(yazinoPaymentState, times(1)).logFailure(buildPaymentContext(), order, CASHIER_NAME, AMAZON, MESSAGE, buildPurchase(order, ORDER_ID
        ));
    }

    private PaymentContext buildPaymentContext() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, PROMOTION_ID, Partner.YAZINO);
    }

    private VerifiedOrder buildVerifiedOrder(String orderId, String productId, final BigDecimal chips) {
        return new VerifiedOrderBuilder().withOrderId(orderId).withProductId(productId).withCurrencyCode(CURRENCY)
                .withPrice(PRICE).withChips(chips).buildVerifiedOrder();
    }

    private PaymentContext getPaymentContext() {
        return new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL, null, Long.valueOf(PROMOTION_ID), Partner.YAZINO);
    }

    private Purchase buildPurchase(VerifiedOrder order, final String externalId) {
        final Purchase purchase = new Purchase();
        purchase.setChips(order.getChips());
        purchase.setCurrencyCode(order.getCurrencyCode());
        purchase.setPrice(order.getPrice());
        purchase.setPurchaseId(order.getOrderId());
        purchase.setExternalId(externalId);
        purchase.setProductId(order.getProductId());
        return purchase;
    }

}
