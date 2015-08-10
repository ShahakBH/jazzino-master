package com.yazino.web.payment.googlecheckout;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.platform.Partner;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.account.ExternalTransactionStatus.SUCCESS;
import static com.yazino.platform.account.ExternalTransactionType.DEPOSIT;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.GoogleCheckout;
import static com.yazino.web.payment.googlecheckout.AndroidInAppBillingService.CASHIER_NAME;
import static com.yazino.web.payment.googlecheckout.AndroidInAppBillingService.OBSCURED_CREDITCARD_NUMBER;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class AndroidInAppBillingServiceTest {
    public static final String GAME_TYPE = "SLOTS";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final String PLAYER_NAME = "Player Name";
    public static final String PLAYER_EMAIL_ADDRESS = "player@somewhere.com";
    private static final Partner PARTNER_ID= Partner.YAZINO;
    public static final PaymentContext PAYMENT_CONTEXT = new PaymentContext(PLAYER_ID, null, PLAYER_NAME, GAME_TYPE, PLAYER_EMAIL_ADDRESS, null, 2345l,
            PARTNER_ID);
    public static final String ORDER1_ID = "12999763169054705758.1344173743070417";
    public static final String ORDER1_PRODUCT_ID = "slots_usd3_buys_5k";
    public static final long ORDER1_PURCHASE_TIME = 1365555687000L;
    public static final String CURRENCY_CODE = "USD";
    public static final BigDecimal ORDER1_CHIPS = BigDecimal.valueOf(5000);
    public static final BigDecimal ORDER1_PRICE = BigDecimal.valueOf(3);
    public static final String ORDER2_ID = "12999763169054705758.1344173743079876";
    public static final String ORDER2_PRODUCT_ID = "slots_usd10_buys_20k";
    public static final long ORDER2_PURCHASE_TIME = 1365555745000L;
    public static final BigDecimal ORDER2_CHIPS = BigDecimal.valueOf(20000);
    public static final BigDecimal ORDER2_PRICE = BigDecimal.valueOf(10);
    public static final String ORDER_JSON = "{\"nonce\":1818223156688188385,\"orders\":[{\"notificationId\":\"8698862048128790553\",\"orderId\":\"" + ORDER1_ID + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\"" + ORDER1_PRODUCT_ID + "\",\"purchaseTime\":" + ORDER1_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"}]}";
    public static final String SIGNATURE = "BpTfDtW4xxe7Uvw55BvmCK8S4evoI+6+2PpqU1Sqj/JwPHpvVuSKfa05jMYQQeNy4iFmTehi5eAn9VVwNFqL2bimLCouS6FnT71GRP/BAmSRO3Z1e8SRS+pPV1om0CI/fqLGF2rBgKBBFi/n67HFffwxmGofrMEETRvB5L5IQTYBTgzWufy7ZwR7EIsgOnhi9Oi/0Y2acrKdm2OOSR849eIcPL8rdEfwrOMiMLF1ZYbdYnqacTBLX9m5cLgWZWqGvAB7TU+jzWaQD5faVD+51rsAuBI9B6A600SRPsZTFxmSzPlPSQizzAFwhU/JkpmKZeBqJ4xkBSNY7bqmvsKG+w==";
    public static final String ORDER_JSON_WITH_TWO_ORDERS = "{\"nonce\":1818223156688188385,\"orders\":[{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + ORDER1_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + ORDER1_PRODUCT_ID
            + "\",\"purchaseTime\":" + ORDER1_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"},{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + ORDER2_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + ORDER2_PRODUCT_ID
            + "\",\"purchaseTime\":" + ORDER2_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"dhyzzzztyrcgutlzjeuixjv\"}]}";
    public static final String ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED = "{\"nonce\":1818223156688188385,\"orders\":[{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + ORDER1_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + ORDER1_PRODUCT_ID
            + "\",\"purchaseTime\":1355763527000,\"purchaseState\":1,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"},{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + ORDER2_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + ORDER2_PRODUCT_ID
            + "\",\"purchaseTime\":" + ORDER2_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"dhyzzzztyrcgutlzjeuixjv\"}]}";
    public static final String PROMO_ORDER_ID = "12999763169054705758.1344173743070417";
    public static final String PROMO_ORDER_PRODUCT_ID = "slots_usd3_buys_15k_p200";
    public static final long PROMO_ORDER_PURCHASE_TIME = 1365555687000L;
    public static final String PROMO_ORDER2_ID = "12999763169054705758.1344173743088417";
    public static final String PROMO_ORDER2_PRODUCT_ID = "slots_usd3_buys_15k_p200";
    public static final long PROMO_ORDER2_PURCHASE_TIME = 1365555745000L;
    public static final BigDecimal PROMO_ORDER_DEFAULT_CHIPS = BigDecimal.valueOf(5000);
    public static final BigDecimal PROMO_ORDER_PROMO_CHIPS = BigDecimal.valueOf(15000);
    public static final BigDecimal PROMO_ORDER_PRICE = BigDecimal.valueOf(3);
    public static final String ORDER_JSON_WITH_2PROMOTIONS_1NON_PROMOTION = "{\"nonce\":1818223156688188385,\"orders\":["
            + "{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + PROMO_ORDER_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + PROMO_ORDER_PRODUCT_ID
            + "\",\"purchaseTime\":" + PROMO_ORDER_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"},"
            + "{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + PROMO_ORDER2_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + PROMO_ORDER2_PRODUCT_ID
            + "\",\"purchaseTime\":" + PROMO_ORDER2_PURCHASE_TIME + ",\"purchaseState\":0,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"},"
            + "{\"notificationId\":\"8698862048128790553\",\"orderId\":\""
            + ORDER2_ID
            + "\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\""
            + ORDER2_PRODUCT_ID
            + "\",\"purchaseTime\":1355767527000,\"purchaseState\":0,\"purchaseToken\":\"dhyzzzztyrcgutlzjeuixjv\"}]}";

    public static final long PROMO_ID = 2345;

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

    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;

    private AndroidInAppBillingService underTest;

    @Mock private YazinoConfiguration yazinoConfiguration;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 5, 24, 9, 45, 5, 693).toDate().getTime());

        underTest = new AndroidInAppBillingService(androidInAppOrderSecurity, paymentStateService, chipBundleResolver, walletService, playerService, communityService, emailer, buyChipsPromotionService,
                "from@your.mum",
                yazinoConfiguration);
        when(yazinoConfiguration.getString(anyString(),anyString())).thenReturn("PLAY_FO_SHIZZLE");
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenPaymentContextIsNUll() {
        underTest.verifyAndCompleteTransactions(null, ORDER_JSON, SIGNATURE, "{}");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenOrderJSONIsNUll() {
        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, null, SIGNATURE, "{}");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenSignatureIsNUll() {
        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, null, "{}");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionWhenPromoIdsIsNUll() {
        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, null);
    }

    @Test
    public void shouldReturnAnEmptyListWhenOrderJSONIsNotSignedCorrectly() {
        setUpMocksForOrder1(false);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        assertTrue(verifiedOrders.isEmpty());
    }

    private void setUpMocksForOrder1(boolean verified) {
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON, SIGNATURE, PARTNER_ID)).thenReturn(verified);
        ChipBundle bundle = new ChipBundle("slots_usd3_buys_5k", ORDER1_CHIPS, ORDER1_CHIPS, ORDER1_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER1_PRODUCT_ID)).thenReturn(bundle);
    }

    @Test
    public void orderShouldHaveDELIVEREDStateWhenOrderJSONIsSignedCorrectlyAndOrderIsCompleted() {
        setUpMocksForOrder1(true);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.DELIVERED);
        final List<VerifiedOrder> expectedOrders = Arrays.asList(expectedOrder);

        assertThat(verifiedOrders, is(expectedOrders));
    }

    @Test
    public void multipleOrdersAreSignedIncorrectly_shouldReturnEmptyList() {
        setMocksForTwoOrders(false);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        assertTrue(verifiedOrders.isEmpty());
    }

    private void setMocksForTwoOrders(boolean verified) {
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, PARTNER_ID)).thenReturn(verified);
        ChipBundle bundle1 = new ChipBundle(ORDER1_PRODUCT_ID, ORDER1_CHIPS, ORDER1_CHIPS, ORDER1_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER1_PRODUCT_ID)).thenReturn(bundle1);
        ChipBundle bundle2 = new ChipBundle(ORDER2_PRODUCT_ID, ORDER2_CHIPS, ORDER2_CHIPS, ORDER2_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER2_PRODUCT_ID)).thenReturn(bundle2);
    }

    @Test
    public void multipleOrdersShouldBeProcessed() {
        setMocksForTwoOrders(true);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.DELIVERED);
        VerifiedOrder expectedOrder2 = createExpectedOrder2(OrderStatus.DELIVERED);
        final List<VerifiedOrder> expectedOrders = Arrays.asList(expectedOrder, expectedOrder2);

        assertThat(verifiedOrders, is(expectedOrders));
    }

    @Test
    public void onlyOrdersWithPurchasedStateOfPurchasedShouldBeDelivered() {
        setUpMocksWithOrder1CanceledOrder2Purchased();

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.ERROR);
        VerifiedOrder expectedOrder2 = createExpectedOrder2(OrderStatus.DELIVERED);
        final List<VerifiedOrder> expectedOrders = Arrays.asList(expectedOrder, expectedOrder2);

        assertThat(verifiedOrders, is(expectedOrders));
    }

    private void setUpMocksWithOrder1CanceledOrder2Purchased() {
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, PARTNER_ID)).thenReturn(true);
        ChipBundle bundle1 = new ChipBundle(ORDER1_PRODUCT_ID, ORDER1_CHIPS, ORDER1_CHIPS, ORDER1_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER1_PRODUCT_ID)).thenReturn(bundle1);
        ChipBundle bundle2 = new ChipBundle(ORDER2_PRODUCT_ID, ORDER2_CHIPS, ORDER2_CHIPS, ORDER2_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER2_PRODUCT_ID)).thenReturn(bundle2);
    }

    private VerifiedOrder createExpectedOrder1(OrderStatus orderStatus) {
        return new VerifiedOrderBuilder()
                .withOrderId(ORDER1_ID)
                .withStatus(orderStatus)
                .withProductId(ORDER1_PRODUCT_ID)
                .withChips(ORDER1_CHIPS)
                .withDefaultChips(ORDER1_CHIPS)
                .withCurrencyCode(CURRENCY_CODE)
                .withPrice(ORDER1_PRICE)
                .buildVerifiedOrder();
    }

    private VerifiedOrder createPromoOrder() {
        return new VerifiedOrderBuilder()
                .withOrderId(PROMO_ORDER_ID)
                .withStatus(OrderStatus.DELIVERED)
                .withProductId(PROMO_ORDER_PRODUCT_ID)
                .withChips(PROMO_ORDER_PROMO_CHIPS)
                .withDefaultChips(PROMO_ORDER_DEFAULT_CHIPS)
                .withCurrencyCode(CURRENCY_CODE)
                .withPrice(PROMO_ORDER_PRICE)
                .buildVerifiedOrder();
    }

    private VerifiedOrder createExpectedOrder2(OrderStatus orderStatus) {
        return new VerifiedOrderBuilder()
                .withOrderId(ORDER2_ID)
                .withStatus(orderStatus)
                .withProductId(ORDER2_PRODUCT_ID)
                .withChips(ORDER2_CHIPS)
                .withDefaultChips(ORDER2_CHIPS)
                .withCurrencyCode(CURRENCY_CODE)
                .withPrice(ORDER2_PRICE)
                .buildVerifiedOrder();
    }

    @Test
    public void shouldStartPaymentWhenCompletingAnOrder() throws PaymentStateException {
        setUpMocksForOrder1(true);
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, PARTNER_ID)).thenReturn(true);

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        verify(paymentStateService).startPayment(CASHIER_NAME, ORDER2_ID);
    }

    @Test
    public void shouldReturnOrderWithErrorStateWhenPaymentStateIsStarted() throws PaymentStateException {
        setUpMocksForOrder1(true);
        doThrow(new PaymentStateException(PaymentState.Started))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        final VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.ERROR);
        Assert.assertThat(verifiedOrders, is(Arrays.asList(expectedOrder)));
    }

    @Test
    public void shouldReturnOrderWithErrorStateWhenPaymentStateIsUnknown() throws PaymentStateException {
        setUpMocksForOrder1(true);
        doThrow(new PaymentStateException(PaymentState.Unknown))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        final VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.ERROR);
        Assert.assertThat(verifiedOrders, is(Arrays.asList(expectedOrder)));
    }

    @Test
    public void shouldReturnOrderWithErrorStateWhenPaymentStateIsFailed() throws PaymentStateException {
        setUpMocksForOrder1(true);
        doThrow(new PaymentStateException(PaymentState.Failed))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        final VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.ERROR);
        Assert.assertThat(verifiedOrders, is(Arrays.asList(expectedOrder)));
    }

    @Test
    public void shouldReturnOrderWithDeliveredStateWhenPaymentStateIsFinishedAndChipsShouldNotBeCredited() throws PaymentStateException {
        // given an order that has already been processed successfully
        setUpMocksForOrder1(true);
        doThrow(new PaymentStateException(PaymentState.Finished))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID);

        // when trying to reprocess this order
        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        // then the order details should be returned and NO chips should be credited
        final VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.DELIVERED);
        assertThat(verifiedOrders, is(Arrays.asList(expectedOrder)));
        verifyZeroInteractions(walletService);
    }

    @Test
    public void shouldReturnOrderWithErrorStateWhenPaymentStateIsFinishedFailed() throws PaymentStateException {
        setUpMocksForOrder1(true);
        doThrow(new PaymentStateException(PaymentState.FinishedFailed))
                .when(paymentStateService)
                .startPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID);

        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        final VerifiedOrder expectedOrder = createExpectedOrder1(OrderStatus.ERROR);
        Assert.assertThat(verifiedOrders, is(Arrays.asList(expectedOrder)));
    }

    @Test
    public void shouldOnlyCreditChipsForTransactionsWithPurchaseStateOfPurchased() throws WalletServiceException {
        // given 2 transactions, the first canceled and the second purchased
        setUpMocksWithOrder1CanceledOrder2Purchased();

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        // then chips should be credited for the second transaction only
        final VerifiedOrder expectedOrder2 = createExpectedOrder2(OrderStatus.DELIVERED);
        final ExternalTransaction externalTransaction =
                buildSuccessfulExternalTransactions(new DateTime(ORDER2_PURCHASE_TIME), expectedOrder2, null);
        verify(walletService).record(externalTransaction);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldSetPaymentStateToFinishedFailedWhenWalletFailsToUpdateBalance() throws WalletServiceException, PaymentStateException {
        // given a purchased transaction
        setUpMocksForOrder1(true);
        // and that wallet service will error
        doThrow(new WalletServiceException("who knows why"))
                .when(walletService)
                .record(Matchers.<ExternalTransaction>anyObject());

        // when completing the order
        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        // then the payment state should be updated to finishedFailed
        verify(paymentStateService).failPayment(GoogleCheckoutService.CASHIER_NAME, ORDER1_ID, false);

        verify(walletService).record(Matchers.<ExternalTransaction>anyObject());
    }

    @Test
    public void shouldCreditChipsForAllTransactionsWithPurchaseStateOfPurchased() throws WalletServiceException {
        // given 2 transactions than can be verified and have purchase state of 0
        setMocksForTwoOrders(true);

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        // then chips should be credited for both transactions
        final VerifiedOrder expectedOrder1 = createExpectedOrder1(OrderStatus.DELIVERED);
        final VerifiedOrder expectedOrder2 = createExpectedOrder2(OrderStatus.DELIVERED);
        final ExternalTransaction[] externalTransaction = {
                buildSuccessfulExternalTransactions(new DateTime(ORDER1_PURCHASE_TIME), expectedOrder1, null),
                buildSuccessfulExternalTransactions(new DateTime(ORDER2_PURCHASE_TIME), expectedOrder2, null)
        };
        for (ExternalTransaction transaction : externalTransaction) {
            verify(walletService).record(transaction);
        }
    }

    @Test
    public void shouldSetPaymentStateToFinishedForEachSuccessfulTransaction() throws WalletServiceException, PaymentStateException {
        // given 2 transactions than can be verified and have purchase state of 0
        setMocksForTwoOrders(true);

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        verify(paymentStateService).finishPayment(CASHIER_NAME, ORDER1_ID);
        verify(paymentStateService).finishPayment(CASHIER_NAME, ORDER2_ID);
    }

    @Test
    public void shouldOnlySetPaymentStateToFinishedWhenPurchaseStateIsPurchased() throws PaymentStateException {
        // given 2 transactions, the first canceled and the second purchased
        setUpMocksWithOrder1CanceledOrder2Purchased();

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        verify(paymentStateService, never()).finishPayment(CASHIER_NAME, ORDER1_ID);
        verify(paymentStateService).finishPayment(CASHIER_NAME, ORDER2_ID);
    }

    @Test
    public void shouldPublishPlayerBalanceForEachSuccessfulTransaction() {
        // given 2 transactions than can be verified and have purchase state of 0
        setMocksForTwoOrders(true);

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        verify(communityService, times(2)).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldNotPublishPlayerBalanceForPurchasesWithNonPurchasedState() {
        // given 2 transactions, the first canceled and the second purchased
        setUpMocksWithOrder1CanceledOrder2Purchased();

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldSendOneEmailForEachTransaction() throws EmailException {
        // given 2 transactions than can be verified and have purchase state of 0
        setMocksForTwoOrders(true);

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS, SIGNATURE, "{}");

        // then one email for each transaction should be sent to the player
        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer, times(2)).quietlySendEmail(captor.capture());
        List<BoughtChipsEmailBuilder> builders = captor.getAllValues();
        assertEmailBuilder(builders.get(0), PLAYER_EMAIL_ADDRESS, PLAYER_NAME, ORDER1_CHIPS,
                Currency.getInstance(CURRENCY_CODE), ORDER1_PRICE, builders.get(0).getPaymentId());
        assertEmailBuilder(builders.get(1), PLAYER_EMAIL_ADDRESS, PLAYER_NAME, ORDER2_CHIPS,
                Currency.getInstance(CURRENCY_CODE), ORDER2_PRICE, builders.get(1).getPaymentId());
    }

    @Test
    public void shouldOnlySendAnEmailForTransactionsWithPurchasedStateOfPurchased() {
        // given 2 transactions, the first canceled and the second purchased
        setUpMocksWithOrder1CanceledOrder2Purchased();

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_TWO_ORDERS_ORDER1_IS_CANCELED, SIGNATURE, "{}");

        // then only one email for the second transaction should be sent
        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer, times(1)).quietlySendEmail(captor.capture());
        List<BoughtChipsEmailBuilder> builders = captor.getAllValues();
        assertEmailBuilder(builders.get(0), PLAYER_EMAIL_ADDRESS, PLAYER_NAME, ORDER2_CHIPS,
                Currency.getInstance(CURRENCY_CODE), ORDER2_PRICE, builders.get(0).getPaymentId());
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
    public void whenProductIdIsUnknown_shouldLogFailedExternalTransactionAndSetPaymentStateToFinishedFailed() throws WalletServiceException, PaymentStateException {
        // given an order with an unknown product
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON, SIGNATURE, PARTNER_ID)).thenReturn(true);
        ChipBundle bundle = new ChipBundle("slots_usd3_buys_5k", ORDER1_CHIPS, ORDER1_CHIPS, ORDER1_PRICE, Currency.getInstance(CURRENCY_CODE));

        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON, SIGNATURE, "{}");

        // then a failed external transaction should be posted
        VerifiedOrder order = new VerifiedOrderBuilder().withOrderId(ORDER1_ID).withProductId(ORDER1_PRODUCT_ID).withStatus(OrderStatus.ERROR).buildVerifiedOrder();
        final ExternalTransaction txn = buildFailedExternalTransaction(new DateTime(ORDER1_PURCHASE_TIME), order);
        verify(walletService).record(txn);

        // and payment state should be set to FinishedFailed
        verify(paymentStateService).failPayment(CASHIER_NAME, order.getOrderId(), false);
    }

    private String buildInternalIdDateString(DateTime purchaseTime) {
        final DateTime dtLondon = purchaseTime.withZone(DateTimeZone.forID("Europe/London"));
        return String.format("%1$tY%<tm%<tdT%<tH%<tM%<tS%<tL", dtLondon.toDate());
    }

    private ExternalTransaction buildSuccessfulExternalTransactions(DateTime purchaseTime, VerifiedOrder order, Long promoId) {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("GoogleCheckout_" + order.getProductId() + "_1_" + buildInternalIdDateString(purchaseTime))
                .withExternalTransactionId(order.getOrderId())
                .withMessage("", purchaseTime.toDateTime(DateTimeZone.UTC))
                .withAmount(Currency.getInstance(order.getCurrencyCode()), order.getPrice())
                .withPaymentOption(order.getChips(), null)
                .withCreditCardNumber(OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(CASHIER_NAME)
                .withStatus(SUCCESS)
                .withType(DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(promoId)
                .withPlatform(ANDROID)
                .build();
    }

    private ExternalTransaction buildFailedExternalTransaction(DateTime purchaseTime, VerifiedOrder order) {
        String message = String.format("Failed to credit chips for productId=%s, playerId=%s.\n"
                + "The transaction has been authorized/charged by Google Play but we cannot credit the player "
                + "with chips as we do not recognise the productId.", order.getProductId(), PLAYER_ID);
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId("GoogleCheckout_" + order.getProductId() + "_1_" + buildInternalIdDateString(purchaseTime))
                .withExternalTransactionId(order.getOrderId())
                .withMessage(message, purchaseTime.toDateTime(DateTimeZone.UTC))
                .withAmount(Currency.getInstance(CURRENCY_CODE), BigDecimal.ZERO)
                .withPaymentOption(BigDecimal.ZERO, null)
                .withCreditCardNumber(GoogleCheckoutService.OBSCURED_CREDITCARD_NUMBER)
                .withCashierName(GoogleCheckoutService.CASHIER_NAME)
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(null)
                .withPlatform(ANDROID)
                .build();
    }

    @Test
    public void shouldLogThePromotionWhenProductPurchasedWasPromoted() throws WalletServiceException {
        // given a transaction for a promoted product
        setupPromotionOrderMocks();

        // and that order1 was promoted
        String promoIds = "{\"" + PROMO_ORDER_ID + "\": \"" + PROMO_ID + "\"}";

        // when completing the orders
        underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_2PROMOTIONS_1NON_PROMOTION, SIGNATURE, promoIds);

        verify(walletService).record(eq(buildSuccessfulExternalTransactions(new DateTime(PROMO_ORDER_PURCHASE_TIME),
                createPromoOrder(),
                PROMO_ID)));
        // then log the promotion id for promoted product
        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, PROMO_ID, PROMO_ORDER_DEFAULT_CHIPS, PROMO_ORDER_PROMO_CHIPS, GOOGLE_CHECKOUT, new DateTime(PROMO_ORDER_PURCHASE_TIME).toDateTime(DateTimeZone.UTC));
    }

    @Test
    public void shouldOnlyLogThePromotionForPromotedProducts() {
        // given transactions for a promoted products and a non promoted product
        setupPromotionOrderMocks();

        // and that order1 was promoted
        String promoIds = "{\"" + PROMO_ORDER_ID + "\": \"" + PROMO_ID + "\", \"" + PROMO_ORDER2_ID + "\": \"" + PROMO_ID + "\"}";

        // when completing the orders
        final List<VerifiedOrder> verifiedOrders = underTest.verifyAndCompleteTransactions(PAYMENT_CONTEXT, ORDER_JSON_WITH_2PROMOTIONS_1NON_PROMOTION, SIGNATURE, promoIds);

        // then promoted orders are logged with purchase time
        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, PROMO_ID, PROMO_ORDER_DEFAULT_CHIPS, PROMO_ORDER_PROMO_CHIPS, GOOGLE_CHECKOUT, new DateTime(PROMO_ORDER_PURCHASE_TIME).toDateTime(DateTimeZone.UTC));
        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, PROMO_ID, PROMO_ORDER_DEFAULT_CHIPS, PROMO_ORDER_PROMO_CHIPS, GOOGLE_CHECKOUT, new DateTime(PROMO_ORDER2_PURCHASE_TIME).toDateTime(DateTimeZone.UTC));

        // and we do not attempt to log promo code for other order
        verifyNoMoreInteractions(buyChipsPromotionService);

        // and verify that the second, non promoted order, was processed
        final VerifiedOrder expectedOrder = createExpectedOrder2(OrderStatus.DELIVERED);
        assertThat(verifiedOrders, hasItem(expectedOrder));

    }

    private void setupPromotionOrderMocks() {
        when(androidInAppOrderSecurity.verify(GAME_TYPE, ORDER_JSON_WITH_2PROMOTIONS_1NON_PROMOTION, SIGNATURE, PARTNER_ID)).thenReturn(true);
        ChipBundle bundle1 = new ChipBundle(PROMO_ORDER_PRODUCT_ID, PROMO_ORDER_PROMO_CHIPS, PROMO_ORDER_DEFAULT_CHIPS, PROMO_ORDER_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PROMO_ORDER_PRODUCT_ID)).thenReturn(bundle1);
        ChipBundle bundle2 = new ChipBundle(ORDER2_PRODUCT_ID, ORDER2_CHIPS, ORDER2_CHIPS, ORDER2_PRICE, Currency.getInstance(CURRENCY_CODE));
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, ORDER2_PRODUCT_ID)).thenReturn(bundle2);
    }
}
