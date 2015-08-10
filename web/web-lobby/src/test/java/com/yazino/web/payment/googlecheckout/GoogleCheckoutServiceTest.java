package com.yazino.web.payment.googlecheckout;

import com.yazino.email.EmailException;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.platform.payment.PaymentStateService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.service.QuietPlayerEmailer;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static com.yazino.platform.account.ExternalTransactionType.DEPOSIT;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.GoogleCheckout;
import static com.yazino.web.payment.googlecheckout.GoogleCheckoutService.*;
import static com.yazino.web.payment.googlecheckout.Order.Status.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class GoogleCheckoutServiceTest {
    private static BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;

    private static final String ORDER_NUMBER = "123";
    private static final String GAME_TYPE = "TEXAS_HOLDEM";
    public static final String PLAYER_EMAIL_ADDRESS = "player@email.com";
    public static final String PLAYER_NAME = "Micky Deloney";
    public static final PaymentContext PAYMENT_CONTEXT =
            new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL_ADDRESS, null, 123l, Partner.YAZINO);
    public static final String CURRENCY_CODE = "USD";
    public static final BigDecimal PRICE = BigDecimal.valueOf(12);
    public static final String MERCHANT_ORDER_NUMBER = "1234523432.1232434645";
    public static final String MERCHANT_ORDER_PRODUCT_ID = "TEXAS_HOLDEM_usd3_buys_5k";
    public static final String MERCHANT_ORDER_NUMBER2 = "1234523432.8976555";
    public static final String MERCHANT_ORDER_PRODUCT_ID2 = "TEXAS_HOLDEM_usd5_buys_10k";
    public static final BigDecimal MERCHANT_ORDER_CHIPS = new BigDecimal(5000);
    public static final BigDecimal MERCHANT_ORDER_PRICE = BigDecimal.valueOf(3);
    public static final BigDecimal MERCHANT_ORDER_CHIPS2 = new BigDecimal(10000);
    public static final BigDecimal MERCHANT_ORDER_PRICE2 = BigDecimal.valueOf(5);
    public static final ChipBundle MERCHANT_ORDER_CHIP_BUNDLE = new ChipBundle(MERCHANT_ORDER_PRODUCT_ID, MERCHANT_ORDER_CHIPS, MERCHANT_ORDER_CHIPS, MERCHANT_ORDER_PRICE, Currency.getInstance(CURRENCY_CODE));
    public static final ChipBundle MERCHANT_ORDER_CHIP_BUNDLE2 = new ChipBundle(MERCHANT_ORDER_PRODUCT_ID2, MERCHANT_ORDER_CHIPS2, MERCHANT_ORDER_CHIPS2, MERCHANT_ORDER_PRICE2, Currency.getInstance(CURRENCY_CODE));


    @Mock
    private GoogleCheckoutApiIntegration googleCheckoutApiIntegration;

    @Mock
    private WalletService walletService;

    @Mock
    private CommunityService communityService;

    @Mock
    private QuietPlayerEmailer emailer;

    @Mock
    private PlayerService playerService;

    @Mock
    private ChipBundleResolver chipBundleResolver;

    @Mock
    private PaymentStateService paymentStateService;

    @Mock
    private AndroidInAppOrderSecurity androidInAppOrderSecurity;

    private GoogleCheckoutService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 5, 24, 9, 45, 5, 693).toDate().getTime());

        underTest = new GoogleCheckoutService(googleCheckoutApiIntegration, walletService, playerService,
                communityService, emailer, chipBundleResolver, paymentStateService, androidInAppOrderSecurity);

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
    }

    @After
    public void resetJodaDateTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void fulfillBuyChipsOrderShouldThrowExceptionWhenPaymentContextIsNull() {
        underTest.fulfillBuyChipsOrder(null, "orderJson");
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void fulfillLegacyBuyChipsOrderShouldThrowExceptionWhenOrderNumberIsNull() {
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fulfillLegacyBuyChipsOrderShouldThrowExceptionWhenOrderNumberIsEmpty() {
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, "");
    }

    @Test
    public void shouldNotCreditChipsWhenOrderStateIsERROR() throws WalletServiceException {
        // given an unauthorized order (i.e. new)
        Order expectedOrder = new Order(ORDER_NUMBER, ERROR);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then wallet service should not post a transaction
        verify(walletService, never()).postTransaction(
                Matchers.<BigDecimal>anyObject(), Matchers.<BigDecimal>anyObject(),
                Matchers.anyString(), Matchers.anyString(), Matchers.any(TransactionContext.class));
        // and order state is returned
        assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldNotCreditChipsWhenOrderStateIsINVALID_ORDER_NUMBER() throws WalletServiceException {
        // given an unauthorized order (i.e. new)
        Order expectedOrder = new Order(ORDER_NUMBER, INVALID_ORDER_NUMBER);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then wallet service should not post a transaction
        verify(walletService, never()).postTransaction(
                Matchers.<BigDecimal>anyObject(), Matchers.<BigDecimal>anyObject(),
                Matchers.anyString(), Matchers.anyString(), Matchers.any(TransactionContext.class));
        // but it should log failed txn
        verify(walletService).record(buildFailedExternalTransaction(order));
        // and order state is returned
        assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldNotCreditChipsWhenOrderStateIsPAYMENT_NOT_AUTHORIZED() throws WalletServiceException {
        // given an unauthorized order (i.e. new)
        Order expectedOrder = new Order(ORDER_NUMBER, PAYMENT_NOT_AUTHORIZED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then wallet service should not post a transaction
        verify(walletService, never()).postTransaction(
                Matchers.<BigDecimal>anyObject(), Matchers.<BigDecimal>anyObject(),
                Matchers.anyString(), Matchers.anyString(), Matchers.any(TransactionContext.class));
        // and order state is returned
        assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldNotCreditChipsWhenOrderStateIsDELIVERED() {
        // given a delivered order
        Order expectedOrder = new Order(ORDER_NUMBER, DELIVERED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order again
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then wallet service should not post a transaction
        verifyZeroInteractions(walletService);
        // and order state is returned
        assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldReturnUnknownProductWhenChipPackageForProductIsNotFound() throws WalletServiceException {
        // given an authorized order
        String productId = "unknown_product";
        Order authorizedOrder = buildOrderWithProduct(productId, PAYMENT_AUTHORIZED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(authorizedOrder);
        // and given no payment option is found for the product
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, productId)).thenReturn(null);

        // when handling request to fulfill buy chip order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then the order's state is UNKNOWN_PRODUCT
        assertThat(order.getStatus(), is(UNKNOWN_PRODUCT));

        // and the txn was logged as a failed txn
        final ExternalTransaction externalTransaction = buildFailedExternalTransaction(order);
        verify(walletService).record(externalTransaction);

        // and chips were not credited
        verifyZeroInteractions(walletService, communityService, emailer);
    }

    @Test
    public void shouldCreditChipsWhenOrderNumberIsAMerchantOrderNumber() throws WalletServiceException {
        SetUpUnfulfilledOrderWithMerchantOrderNumber unfulfilledOrderWithMerchantOrderNumber = new SetUpUnfulfilledOrderWithMerchantOrderNumber().invoke();
        String orderJson = unfulfilledOrderWithMerchantOrderNumber.getOrderJson();
        Order order = unfulfilledOrderWithMerchantOrderNumber.getOrder();
        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderJson);

        final ExternalTransaction externalTransaction = buildSuccessfulExternalTransaction(order);
        verify(walletService).record(externalTransaction);
    }

    @Test
    public void shouldPublishPlayerBalanceWhenFulfillingAnOrderWithMerchantOrderNumber() throws WalletServiceException {
        SetUpUnfulfilledOrderWithMerchantOrderNumber unfulfilledOrderWithMerchantOrderNumber = new SetUpUnfulfilledOrderWithMerchantOrderNumber().invoke();
        String orderJson = unfulfilledOrderWithMerchantOrderNumber.getOrderJson();

        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderJson);

        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldSendEmailToPlayerWhenFulfillingAnOrderWithMerchantOrderNumber() throws WalletServiceException, EmailException {
        SetUpUnfulfilledOrderWithMerchantOrderNumber unfulfilledOrderWithMerchantOrderNumber = new SetUpUnfulfilledOrderWithMerchantOrderNumber().invoke();
        String orderJson = unfulfilledOrderWithMerchantOrderNumber.getOrderJson();

        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderJson);

        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());
        BoughtChipsEmailBuilder builder = captor.getValue();

        assertEmailBuilder(builder, PLAYER_EMAIL_ADDRESS, PLAYER_NAME, MERCHANT_ORDER_CHIPS,
                Currency.getInstance(CURRENCY_CODE), MERCHANT_ORDER_PRICE, builder.getPaymentId());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenJsonIsNull() throws PaymentStateException {
        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, null);
    }

    @Test
    public void shouldCreditChipsForAllGooglePlayTransactions() throws WalletServiceException {
        SetUpUnfulfilledOrders unfulfilledOrders = new SetUpUnfulfilledOrders().invoke();
        String orderJson = unfulfilledOrders.getOrderJson();
        final List<Order> orders = unfulfilledOrders.getOrders();
        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderJson);

        for (Order order : orders) {
            final ExternalTransaction externalTransaction = buildSuccessfulExternalTransaction(order);
            verify(walletService).record(externalTransaction);
        }
    }

    @Test
    public void shouldSendOneEmailForEachTransaction() throws WalletServiceException, EmailException {
        SetUpUnfulfilledOrders unfulfilledOrders = new SetUpUnfulfilledOrders().invoke();
        String orderJson = unfulfilledOrders.getOrderJson();

        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderJson);

        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer, times(2)).quietlySendEmail(captor.capture());
        List<BoughtChipsEmailBuilder> builders = captor.getAllValues();
        assertEmailBuilder(builders.get(0), PLAYER_EMAIL_ADDRESS, PLAYER_NAME, MERCHANT_ORDER_CHIPS,
                Currency.getInstance(CURRENCY_CODE), MERCHANT_ORDER_PRICE, builders.get(0).getPaymentId());
        assertEmailBuilder(builders.get(1), PLAYER_EMAIL_ADDRESS, PLAYER_NAME, MERCHANT_ORDER_CHIPS2,
                Currency.getInstance(CURRENCY_CODE), MERCHANT_ORDER_PRICE2, builders.get(1).getPaymentId());
    }

    @Test
    public void shouldSetOrderStateToDeliveredWhenFulfillingAFinishedTransactionForOrderWithMerchantOrderNumber() throws PaymentStateException {
        SetUpFulfilledOrderWithMerchantOrderNumber orderDetails = new SetUpFulfilledOrderWithMerchantOrderNumber().withPaymentState(PaymentState.Finished);

        final List<Order> actualOrders = underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, orderDetails.getOrderJson());

        assertThat(actualOrders.get(0), is(orderDetails.getOrder()));

        verifyZeroInteractions(walletService);
    }

    @SuppressWarnings("NullableProblems")
    private Order buildOrderWithProduct(String productId, Order.Status orderStatus) {
        Order order = new Order(ORDER_NUMBER, orderStatus);
        order.setProductId(productId);
        order.setCurrencyCode(CURRENCY_CODE);
        order.setPrice(PRICE);
        order.setChips(null);
        return order;
    }

    private ExternalTransaction buildFailedExternalTransaction(Order order) {
        String message;
        if (UNKNOWN_PRODUCT == order.getStatus()) {
            message = String.format(UNKNOW_PRODUCT_FAILURE_MSG_FORMAT, order.getProductId(), PLAYER_ID);
        } else {
            message = String.format(GoogleCheckoutService.DEFAULT_FAILURE_MSG_FORMAT, order.getProductId(), PLAYER_ID, order.getStatus());
        }
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("GoogleCheckout_" + order.getProductId() + "_1_20120524T094505693")
                .withExternalTransactionId(order.getOrderNumber())
                .withMessage(message, new DateTime())
                .withAmount(StringUtils.isBlank(order.getCurrencyCode()) ? Currency.getInstance("USD") : Currency.getInstance(order.getCurrencyCode()), order.getPrice() == null ? BigDecimal.ZERO : order.getPrice())
                .withPaymentOption(order.getChips() == null ? BigDecimal.ZERO : order.getChips(), null)
                .withCreditCardNumber(OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(CASHIER_NAME)
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(123l)
                .withPlatform(Platform.ANDROID)
                .build();
    }

    @Test
    public void shouldAddChipsToOrderWhenProductIdIsKnownToService() {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when handling request to fulfill the buy chip order again
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then chipBundle are added to the order
        assertThat(order.getChips(), is(BigDecimal.valueOf(98000)));
    }

    @Test
    public void orderStatusShouldBeDeliveredAfterFulfillingAnAuthorizedOrder() throws WalletServiceException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when handling request to fulfill the buy chip order again
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then order's status is DELIVERED
        assertThat(order.getStatus(), is(DELIVERED));
    }

    @Test
    public void shouldCreditChipsWhenFulfillingAnAuthorizedOrder() throws WalletServiceException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when successfully fulfilling an order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then chipBundle are credited
        final ExternalTransaction externalTransaction = buildSuccessfulExternalTransaction(order);
        verify(walletService).record(externalTransaction);
    }

    @Test
    public void shouldPublishPlayerBalanceWhenFulfillingAnAuthorizedOrder() throws WalletServiceException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when successfully fulfilling an order
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then player's balance is published
        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldSendEmailToPlayerWhenFulfillingAnAuthorizedOrder() throws WalletServiceException, EmailException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when successfully fulfilling an order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then the player is sent a confirmation email
        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());
        BoughtChipsEmailBuilder builder = captor.getValue();

        assertEmailBuilder(builder, PLAYER_EMAIL_ADDRESS, PLAYER_NAME, new BigDecimal(98000),
                Currency.getInstance(CURRENCY_CODE), PRICE, builder.getPaymentId());

    }

    private static void assertEmailBuilder(BoughtChipsEmailBuilder builder,
                                           String email,
                                           String playerName,
                                           BigDecimal chips,
                                           Currency currency,
                                           BigDecimal cost,
                                           String transactionId) {
        assertEquals(email, builder.getEmailAddress());
        assertEquals(playerName, builder.getFirstName());
        assertEquals(chips, builder.getPurchasedChips());
        assertEquals(currency, builder.getCurrency());
        assertEquals(cost, builder.getCost());
        assertEquals("", builder.getCardNumber());
        assertEquals(transactionId, builder.getPaymentId());
        assertEquals(GoogleCheckout, builder.getPaymentEmailBodyTemplate());

    }

    @Test
    public void chipsShouldBeAddedToOrderWhenFulfillingAnAuthorizedOrder() throws WalletServiceException, EmailException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when successfully fulfilling an order
        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then the order has the chip amount set
        assertThat(order.getChips(), is(BigDecimal.valueOf(98000)));
    }

    // note that chipBundle ARE NOT added to the order
    private Order setUpUnfulfilledAuthorizedOrderWithKnownProduct(ChipBundle chipBundle) {
        String productId = "POKER_USD10_98K";
        Order currentOrder = buildOrderWithProduct(productId, PAYMENT_AUTHORIZED);
        Mockito.when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(currentOrder);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, productId)).thenReturn(chipBundle);
        return currentOrder;
    }

    private ExternalTransaction buildSuccessfulExternalTransaction(Order order) {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("GoogleCheckout_" + order.getProductId() + "_1_20120524T094505693")
                .withExternalTransactionId(order.getOrderNumber())
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance(order.getCurrencyCode()), order.getPrice())
                .withPaymentOption(order.getChips(), null)
                .withCreditCardNumber(OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(CASHIER_NAME)
                .withStatus(SUCCESS)
                .withType(DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(123l)
                .withPlatform(Platform.ANDROID)
                .build();
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void fetchAvailableProductsShouldThrowExceptionWhenOrderNumberIsNull() {
        underTest.fetchAvailableProducts(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchAvailableProductsShouldThrowExceptionWhenOrderNumberIsEmpty() {
        underTest.fetchAvailableProducts("");
    }

    @Test
    public void shouldFetchProductsForGameType() {
        when(chipBundleResolver.getProductIdsFor(GAME_TYPE)).thenReturn(Arrays.asList("p1", "p2"));

        final List<String> productIds = underTest.fetchAvailableProducts(GAME_TYPE);

        assertThat(productIds, is(Arrays.asList("p1", "p2")));
    }

    @Test
    public void shouldStartPaymentWhenFulfillingAnOrder() throws PaymentStateException {
        SetUpUnfulfilledOrderWithMerchantOrderNumber unfulfilledOrderWithMerchantOrderNumber = new SetUpUnfulfilledOrderWithMerchantOrderNumber().invoke();

        underTest.fulfillBuyChipsOrder(PAYMENT_CONTEXT, unfulfilledOrderWithMerchantOrderNumber.getOrderJson());

        verify(paymentStateService).startPayment(CASHIER_NAME, MERCHANT_ORDER_NUMBER);
    }

    @Test
    public void shouldReturnOrderWithDeliveredStateWhenPaymentStateIsFinishedAndOrderIsDelivered() throws PaymentStateException {
        Order expectedOrder = new Order(ORDER_NUMBER, DELIVERED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);
        doThrow(new PaymentStateException(PaymentState.Finished))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER_NUMBER);

        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        Assert.assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldReturnOrderWithInvalidOrderNumberStateWhenPaymentStateIsFinishedAndOrderNumberIsInvalid() throws PaymentStateException {
        Order expectedOrder = new Order(ORDER_NUMBER, INVALID_ORDER_NUMBER);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);
        doThrow(new PaymentStateException(PaymentState.Finished))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER_NUMBER);

        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        Assert.assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldReturnOrderWithInProgressStateWhenPaymentStateIsStarted() throws PaymentStateException {
        Order expectedOrder = new Order(ORDER_NUMBER, IN_PROGRESS);

        doThrow(new PaymentStateException(PaymentState.Started))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER_NUMBER);

        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        Assert.assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldReturnOrderWithErrorStateWhenPaymentStateIsUnknown() throws PaymentStateException {
        Order expectedOrder = new Order(ORDER_NUMBER, ERROR);

        doThrow(new PaymentStateException(PaymentState.Unknown))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER_NUMBER);

        final Order order = underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        Assert.assertThat(order, is(expectedOrder));
    }

    @Test
    public void shouldSetPaymentStateToFailedWhenWalletFailsToUpdateBalance() throws WalletServiceException, PaymentStateException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);
        // and that wallet service will error
        doThrow(new WalletServiceException("who knows"))
                .when(walletService)
                .record(Matchers.<ExternalTransaction>anyObject());

        // when trying to fulfill an order
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then the payment state should be failed (i.e. can retry)
        verify(paymentStateService).failPayment(CASHIER_NAME, ORDER_NUMBER);

        verify(walletService).record(Matchers.<ExternalTransaction>anyObject());
    }

    @Test
    public void shouldSetPaymentStateToFinishedWhenChipPackageForProductIsNotFound() throws WalletServiceException, PaymentStateException {
        // given an authorized order
        String productId = "unknown_product";
        Order authorizedOrder = buildOrderWithProduct(productId, PAYMENT_AUTHORIZED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(authorizedOrder);
        // and given no payment option is found for the product

        // when handling request to fulfill buy chip order
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then cannot complete order since service cannot match associated product id to a chip package
        verify(paymentStateService).finishPayment(CASHIER_NAME, ORDER_NUMBER);
        verify(chipBundleResolver).findChipBundleForProductId(GAME_TYPE, productId);
    }

    @Test
    public void shouldChangePaymentStateToFinishedWhenOrderIsFulfilled() throws PaymentStateException {
        // given a unfulfilled, authorized order, with known product
        ChipBundle chipBundle = new ChipBundle("product id", BigDecimal.valueOf(98000), BigDecimal.valueOf(98000), BigDecimal.TEN, Currency.getInstance("USD"));
        setUpUnfulfilledAuthorizedOrderWithKnownProduct(chipBundle);

        // when successfully fulfilling an order
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        // then payment state should be updated
        verify(paymentStateService).finishPayment(GoogleCheckoutService.CASHIER_NAME, ORDER_NUMBER);
    }

    @Test
    public void shouldChangePaymentStateToFinishedWhenOrderIsCancelled() throws PaymentStateException {
        // given a delivered order
        Order expectedOrder = new Order(ORDER_NUMBER, CANCELLED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order again
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        verify(paymentStateService).finishPayment(CASHIER_NAME, ORDER_NUMBER);
    }

    @Test
    public void shouldLogFailedTxnWhenWhenOrderIsCancelled() throws WalletServiceException {
        // given a delivered order
        Order expectedOrder = new Order(ORDER_NUMBER, CANCELLED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order again
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        verify(walletService).record(buildFailedExternalTransaction(expectedOrder));
    }

    @Test
    public void shouldChangePaymentStateToFailedWhenPaymentIsNotAuthorized() throws PaymentStateException {
        // given a delivered order
        Order expectedOrder = new Order(ORDER_NUMBER, PAYMENT_NOT_AUTHORIZED);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order again
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        verify(paymentStateService).failPayment(CASHIER_NAME, ORDER_NUMBER);
    }

    @Test
    public void shouldChangePaymentStateToFailedOrderIfProcessingFails() throws PaymentStateException {
        // given a delivered order
        Order expectedOrder = new Order(ORDER_NUMBER, ERROR);
        when(googleCheckoutApiIntegration.retrieveOrderState(ORDER_NUMBER)).thenReturn(expectedOrder);

        // when handling request to fulfill buy chip order again
        underTest.fulfillLegacyBuyChipsOrder(PAYMENT_CONTEXT, ORDER_NUMBER);

        verify(paymentStateService).failPayment(CASHIER_NAME, ORDER_NUMBER);
    }

    private class SetUpUnfulfilledOrderWithMerchantOrderNumber {
        private String orderJson;
        private Order order;

        public String getOrderJson() {
            return orderJson;
        }

        public Order getOrder() {
            return order;
        }

        public SetUpUnfulfilledOrderWithMerchantOrderNumber invoke() {
            when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, MERCHANT_ORDER_PRODUCT_ID)).thenReturn(MERCHANT_ORDER_CHIP_BUNDLE);
            orderJson = "{\"nonce\":-125345462236698780,\"orders\":[{\"notificationId\":\"3288054527729\",\"orderId\":\""
                    + MERCHANT_ORDER_NUMBER + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
                    + MERCHANT_ORDER_PRODUCT_ID
                    + "\",\"purchaseTime\":1354771860000,\"purchaseState\":0,\"purchaseToken\":\"xnhxsjkiyekdhquchwshhjwy\"}]}";

            order = new Order(MERCHANT_ORDER_NUMBER, PAYMENT_AUTHORIZED);
            order.setCurrencyCode(CURRENCY_CODE);
            order.setPrice(MERCHANT_ORDER_PRICE);
            order.setChips(MERCHANT_ORDER_CHIPS);
            order.setProductId(MERCHANT_ORDER_PRODUCT_ID);
            return this;
        }
    }

    private class SetUpUnfulfilledOrders {
        private String orderJson;
        private List<Order> orders;

        public String getOrderJson() {
            return orderJson;
        }

        public List<Order> getOrders() {
            return orders;
        }

        public SetUpUnfulfilledOrders invoke() {
            when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, MERCHANT_ORDER_PRODUCT_ID)).thenReturn(MERCHANT_ORDER_CHIP_BUNDLE);
            when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, MERCHANT_ORDER_PRODUCT_ID2)).thenReturn(MERCHANT_ORDER_CHIP_BUNDLE2);
            orderJson = "{\"nonce\":-125345462236698780,\"orders\":[{\"notificationId\":\"3288054527729\",\"orderId\":\""
                    + MERCHANT_ORDER_NUMBER + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
                    + MERCHANT_ORDER_PRODUCT_ID
                    + "\",\"purchaseTime\":1354771860000,\"purchaseState\":0,\"purchaseToken\":\"xnhxsjkiyekdhquchwshhjwy\"}," +
                    "{\"notificationId\":\"32880545277123\",\"orderId\":\""
                    + MERCHANT_ORDER_NUMBER2 + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
                    + MERCHANT_ORDER_PRODUCT_ID2
                    + "\",\"purchaseTime\":13676718784932,\"purchaseState\":0,\"purchaseToken\":\"hgfshdgf\"}]}";

            Order order1 = new Order(MERCHANT_ORDER_NUMBER, PAYMENT_AUTHORIZED);
            order1.setCurrencyCode(CURRENCY_CODE);
            order1.setPrice(MERCHANT_ORDER_PRICE);
            order1.setChips(MERCHANT_ORDER_CHIPS);
            order1.setProductId(MERCHANT_ORDER_PRODUCT_ID);
            Order order2 = new Order(MERCHANT_ORDER_NUMBER, PAYMENT_AUTHORIZED);
            order2.setCurrencyCode(CURRENCY_CODE);
            order2.setPrice(MERCHANT_ORDER_PRICE);
            order2.setChips(MERCHANT_ORDER_CHIPS);
            order2.setProductId(MERCHANT_ORDER_PRODUCT_ID);
            orders = Arrays.asList(order1, order2);
            return this;
        }
    }

    private class SetUpFulfilledOrderWithMerchantOrderNumber {
        private String orderJson;
        private Order order;

        public String getOrderJson() {
            return orderJson;
        }

        public Order getOrder() {
            return order;
        }

        public SetUpFulfilledOrderWithMerchantOrderNumber withPaymentState(PaymentState paymentState) throws PaymentStateException {
            when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, MERCHANT_ORDER_PRODUCT_ID)).thenReturn(MERCHANT_ORDER_CHIP_BUNDLE);
            doThrow(new PaymentStateException(paymentState)).when(paymentStateService).startPayment(CASHIER_NAME, MERCHANT_ORDER_NUMBER);

            orderJson = "{\"nonce\":-125345462236698780,\"orders\":[{\"notificationId\":\"3288054527729\",\"orderId\":\""
                    + MERCHANT_ORDER_NUMBER + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
                    + MERCHANT_ORDER_PRODUCT_ID
                    + "\",\"purchaseTime\":1354771860000,\"purchaseState\":0,\"purchaseToken\":\"xnhxsjkiyekdhquchwshhjwy\"}]}";

            order = new Order(MERCHANT_ORDER_NUMBER, DELIVERED);

            return this;
        }
    }
}
