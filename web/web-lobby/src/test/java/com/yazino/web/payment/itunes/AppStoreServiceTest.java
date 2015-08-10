package com.yazino.web.payment.itunes;

import com.yazino.mobile.yaps.config.TypedMapBean;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.payment.PaymentState;
import com.yazino.platform.reference.Currency;
import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.yazino.platform.Partner.YAZINO;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AppStoreServiceTest {

    private final AppStoreConfiguration mConfiguration = new AppStoreConfiguration();
    private final AppStoreApiIntegration mApiIntegration = mock(AppStoreApiIntegration.class);
    private final TransactionalOrderProcessor<AppStoreOrder> mOrderProcessor = mock(TransactionalOrderProcessor.class);
    private final BuyChipsPromotionService mChipsPromotionService = mock(BuyChipsPromotionService.class);

    private final AppStoreService mService = new AppStoreService(mConfiguration, mOrderProcessor, mChipsPromotionService);

    private final BigDecimal mPlayerId = BigDecimal.valueOf(123);
    private final String mReceipt = "1789fhjbkdsbghjkfs vgruegibfihwcfgrwgcfyuwgyuguwghi";
    private final String mGameType = "SLOTS";
    private final BigDecimal mCashAmount = BigDecimal.valueOf(9999);
    private final java.util.Currency mCurrency = java.util.Currency.getInstance("USD");
    private final String mTransactionIdentifier = "6796796786786";
    private final String mProductIdentifier = "USD8_BUYS_100K";
    private Partner partnerId= YAZINO;

    @Before
    public void setup() {
        mService.setAppleAPI(mApiIntegration);
    }

    @Test
    public void shouldNotProcessOrderIfOrderStatusCodeIsNotZero() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder order = new AppStoreOrder(1);
        order.setOrderId(mTransactionIdentifier);
        order.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(order);
        mService.fulfilOrder(context);
        verify(mApiIntegration).retrieveOrder(mReceipt);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldNotProcessOrderIfTransactionIdentifiersDontMatch() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder order = new AppStoreOrder(0);
        order.setOrderId(mTransactionIdentifier + "foo");
        order.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(order);
        mService.fulfilOrder(context);
        verify(mApiIntegration).retrieveOrder(mReceipt);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldNotProcessOrderIfProductIdentifiersDontMatch() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder order = new AppStoreOrder(0);
        order.setOrderId(mTransactionIdentifier);
        order.setProductId(mProductIdentifier + "foo");
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(order);
        mService.fulfilOrder(context);
        verify(mApiIntegration).retrieveOrder(mReceipt);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldNotProcessOrderIfBothTransactionAndProductIdentifiersDontMatch() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder order = new AppStoreOrder(0);
        order.setOrderId(mTransactionIdentifier+"bar");
        order.setProductId(mProductIdentifier + "foo");
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(order);
        mService.fulfilOrder(context);
        verify(mApiIntegration).retrieveOrder(mReceipt);
        verifyZeroInteractions(mOrderProcessor);
    }

    @Test
    public void shouldProcessOrderWhenStatusCodeIsZeroAndContextMatchesOrder() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder order = new AppStoreOrder(0);
        order.setOrderId(mTransactionIdentifier);
        order.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(order);
        mService.fulfilOrder(context);
        verify(mApiIntegration).retrieveOrder(mReceipt);
        verify(mOrderProcessor).processOrder(order);
    }

    @Test
    public void shouldReturnOrderWithCurrency() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier);
        success.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mCurrency, actual.getCurrency());
    }

    @Test
    public void shouldReturnOrderWithCashAmount() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier);
        success.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mCashAmount, actual.getCashAmount());
    }

    @Test
    public void shouldReturnOrderWithStandardGameType() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier);
        success.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mGameType, actual.getGameType());
    }

    @Test
    public void shouldReturnOrderWithMappedGameType() throws Exception {
        mConfiguration.setGameBundleMappings(new TypedMapBean<>(AppStoreTestUtils.toMap("com.yazino.WheelDeal", mGameType, "yazino.Blackjack", "BJ")));
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, "com.yazino.WheelDeal", mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier);
        success.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mGameType, actual.getGameType());
    }

    @Test
    public void shouldReturnOrderWithPlayerId() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier);
        success.setProductId(mProductIdentifier);
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mPlayerId, actual.getPlayerId());
    }

    @Test
    public void shouldReturnOrderWithDetailsFromContextWhenOrderOkButContextDoesntMatch() throws Exception {
        AppStorePaymentContext context = new AppStorePaymentContext(mPlayerId, null, mGameType, mReceipt, mCashAmount, mCurrency, mTransactionIdentifier, mProductIdentifier,
                partnerId);
        AppStoreOrder success = new AppStoreOrder(0);
        success.setOrderId(mTransactionIdentifier+"123");
        success.setProductId(mProductIdentifier+"ABC");
        when(mApiIntegration.retrieveOrder(mReceipt)).thenReturn(success);
        AppStoreOrder actual = mService.fulfilOrder(context);
        assertEquals(mTransactionIdentifier, actual.getOrderId());
        assertEquals(mProductIdentifier, actual.getProductId());
        assertEquals(PaymentState.Failed, actual.getPaymentState());
    }

    @Test
    public void shouldFindCorrectPaymentOptionForStandardProduct() throws Exception {
        Map<String, String> slotsStandard = AppStoreTestUtils.toMap("IOS_USD3", "USD3_BUYS_5K", "IOS_USD8", "USD8_BUYS_15K", "IOS_USD15", "USD15_BUYS_30K", "IOS_USD30", "USD30_BUYS_70K", "IOS_USD70", "USD70_BUYS_200K", "IOS_USD90", "USD90_BUYS_300K_2");
        Map<String, String> slotsPromotion = AppStoreTestUtils.toMap("IOS_USD3_X10", "USD3_BUYS_5.5K_P10", "IOS_USD3_X25", "USD3_BUYS_6.25K_P25", "IOS_USD3_X50", "USD3_BUYS_7.5K_P50", "IOS_USD3_X100", "USD3_BUYS_10K_P100", "IOS_USD8_X10", "USD8_BUYS_16.5K_P10", "IOS_USD8_X25", "USD8_BUYS_18.75K_P25", "IOS_USD8_X50", "USD8_BUYS_22.5K_P50", "IOS_USD8_X100", "USD8_BUYS_30K_P100", "IOS_USD15_X10", "USD15_BUYS_33K_P10", "IOS_USD15_X25", "USD15_BUYS_37.5K_P25", "IOS_USD15_X50", "USD15_BUYS_45K_P50", "IOS_USD15_X100", "USD15_BUYS_60K_P100", "IOS_USD30_X10", "USD30_BUYS_77K_P10", "IOS_USD30_X25", "USD30_BUYS_87.5K_P25", "IOS_USD30_X50", "USD30_BUYS_105K_P50", "IOS_USD30_X100", "USD30_BUYS_140K_P100", "IOS_USD70_X10", "USD70_BUYS_220K_P10", "IOS_USD70_X25", "USD70_BUYS_250K_P25", "IOS_USD70_X50", "USD70_BUYS_300K_P50", "IOS_USD70_X100", "USD70_BUYS_400K_P100", "IOS_USD90_X10", "USD90_BUYS_330K_P10", "IOS_USD90_X25", "USD90_BUYS_375K_P25", "IOS_USD90_X50", "USD90_BUYS_450K_P50", "IOS_USD90_X100", "USD90_BUYS_600K_P100");
        mConfiguration.setStandardPackageMappings(AppStoreTestUtils.toMap("SLOTS", slotsStandard));
        mConfiguration.setPromotionPackageMappings(AppStoreTestUtils.toMap("SLOTS", slotsPromotion));
        mConfiguration.afterPropertiesSet();

        PaymentOption option15 = AppStoreTestUtils.toOption(15, 30000, 30000);
        PaymentOption option8 = AppStoreTestUtils.toOption(8, 15000, 16500);
        PaymentOption option30 = AppStoreTestUtils.toOption(30, 70000, 70000);
        PaymentOption option90 = AppStoreTestUtils.toOption(90, 300000, 300000);
        PaymentOption option70 = AppStoreTestUtils.toOption(70, 200000, 200000);
        PaymentOption option3 = AppStoreTestUtils.toOption(3, 5000, 5500);
        List<PaymentOption> options = Arrays.asList(option3, option15, option8, option30, option90, option70);

        Map<Currency, List<PaymentOption>> allOptions = AppStoreTestUtils.toMap(Currency.USD, options);
        when(mChipsPromotionService.getBuyChipsPaymentOptionsFor(any(BigDecimal.class), eq(Platform.IOS))).thenReturn(allOptions);

        assertEquals(option3, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD3_BUYS_5K")));
        assertEquals(option8, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD8_BUYS_15K")));
        assertEquals(option15, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD15_BUYS_30K")));
        assertEquals(option30, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD30_BUYS_70K")));
        assertEquals(option70, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD70_BUYS_200K")));
        assertEquals(option90, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD90_BUYS_300K_2")));
    }

    @Test
    public void shouldFindCorrectPaymentOptionForPromotionalProduct() throws Exception {
        Map<String, String> slotsStandard = AppStoreTestUtils.toMap("IOS_USD3", "USD3_BUYS_5K", "IOS_USD8", "USD8_BUYS_15K", "IOS_USD15", "USD15_BUYS_30K", "IOS_USD30", "USD30_BUYS_70K", "IOS_USD70", "USD70_BUYS_200K", "IOS_USD90", "USD90_BUYS_300K_2");
        Map<String, String> slotsPromotion = AppStoreTestUtils.toMap("IOS_USD3_X10", "USD3_BUYS_5.5K_P10", "IOS_USD3_X25", "USD3_BUYS_6.25K_P25", "IOS_USD3_X50", "USD3_BUYS_7.5K_P50", "IOS_USD3_X100", "USD3_BUYS_10K_P100", "IOS_USD8_X10", "USD8_BUYS_16.5K_P10", "IOS_USD8_X25", "USD8_BUYS_18.75K_P25", "IOS_USD8_X50", "USD8_BUYS_22.5K_P50", "IOS_USD8_X100", "USD8_BUYS_30K_P100", "IOS_USD15_X10", "USD15_BUYS_33K_P10", "IOS_USD15_X25", "USD15_BUYS_37.5K_P25", "IOS_USD15_X50", "USD15_BUYS_45K_P50", "IOS_USD15_X100", "USD15_BUYS_60K_P100", "IOS_USD30_X10", "USD30_BUYS_77K_P10", "IOS_USD30_X25", "USD30_BUYS_87.5K_P25", "IOS_USD30_X50", "USD30_BUYS_105K_P50", "IOS_USD30_X100", "USD30_BUYS_140K_P100", "IOS_USD70_X10", "USD70_BUYS_220K_P10", "IOS_USD70_X25", "USD70_BUYS_250K_P25", "IOS_USD70_X50", "USD70_BUYS_300K_P50", "IOS_USD70_X100", "USD70_BUYS_400K_P100", "IOS_USD90_X10", "USD90_BUYS_330K_P10", "IOS_USD90_X25", "USD90_BUYS_375K_P25", "IOS_USD90_X50", "USD90_BUYS_450K_P50", "IOS_USD90_X100", "USD90_BUYS_600K_P100");
        mConfiguration.setStandardPackageMappings(AppStoreTestUtils.toMap("SLOTS", slotsStandard));
        mConfiguration.setPromotionPackageMappings(AppStoreTestUtils.toMap("SLOTS", slotsPromotion));
        mConfiguration.afterPropertiesSet();

        PaymentOption option15 = AppStoreTestUtils.toOption(15, 30000, 30000);
        PaymentOption option8 = AppStoreTestUtils.toOption(8, 15000, 16500);
        PaymentOption option30 = AppStoreTestUtils.toOption(30, 70000, 70000);
        PaymentOption option90 = AppStoreTestUtils.toOption(90, 300000, 300000);
        PaymentOption option70 = AppStoreTestUtils.toOption(70, 200000, 200000);
        PaymentOption option3 = AppStoreTestUtils.toOption(3, 5000, 5500);
        List<PaymentOption> options = Arrays.asList(option3, option15, option8, option30, option90, option70);

        Map<Currency, List<PaymentOption>> allOptions = AppStoreTestUtils.toMap(Currency.USD, options);
        when(mChipsPromotionService.getBuyChipsPaymentOptionsFor(any(BigDecimal.class), eq(Platform.IOS))).thenReturn(allOptions);

        assertEquals(option3, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD3_BUYS_6.25K_P25")));
        assertEquals(option8, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD8_BUYS_16.5K_P10")));
        assertEquals(option15, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD15_BUYS_37.5K_P25")));
        assertEquals(option30, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD30_BUYS_105K_P50")));
        assertEquals(option70, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD70_BUYS_250K_P25")));
        assertEquals(option90, mService.findPaymentOptionForOrder(toOrder("SLOTS", "USD90_BUYS_600K_P100")));
    }


    private static AppStoreOrder toOrder(String gameType, String product) {
        AppStoreOrder order = new AppStoreOrder(0);
        order.setGameType(gameType);
        order.setProductId(product);
        order.setPlayerId(BigDecimal.TEN);
        return order;
    }

}
