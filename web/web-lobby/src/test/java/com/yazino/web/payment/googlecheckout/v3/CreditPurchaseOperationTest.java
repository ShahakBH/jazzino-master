package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.android.AndroidPaymentState;
import com.yazino.platform.payment.android.AndroidPaymentStateDetails;
import com.yazino.platform.payment.android.AndroidPaymentStateException;
import com.yazino.platform.payment.android.AndroidPaymentStateService;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT;
import static com.yazino.web.payment.PurchaseStatus.FAILED;
import static com.yazino.web.payment.PurchaseStatus.SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class CreditPurchaseOperationTest {

    public static final String GAME_TYPE = "sample-game-type";
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final String PRODUCT_ID = "slots_usd15_buys_90k_p200";
    public static final BigDecimal DEFAULT_CHIPS = BigDecimal.valueOf(30000);
    public static final BigDecimal PROMO_CHIPS = BigDecimal.valueOf(90000);
    public static final long PROMO_ID = 987L;
    public static final String INTERNAL_TRANSACTION_ID = "sample internal transaction id";
    public static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(565656);
    public static final BigDecimal PRICE = BigDecimal.valueOf(15);
    public static final Currency CURRENCY = Currency.getInstance("USD");
    public static final DateTime REQUEST_TIME = new DateTime();
    public static final String MESSAGE = "sample message - txn cancelled";
    public static final PlayerProfile PLAYER_PROFILE = new PlayerProfile("email", "displayName", "realName", Gender.MALE, "us", "fistName", "lastName", DateTime.now(), "ref", "prov", "rpx", "extId", true);
    public static final String ORDER_ID = "orderID";
    private static final DateTime PURCHASE_TIME = new DateTime().minusDays(2).withZone(DateTimeZone.forID("UTC"));


    private BuyChipsPromotionService buyChipsPromotionService = mock(BuyChipsPromotionService.class);
    private CommunityService communityService = mock(CommunityService.class);
    private AndroidPaymentStateService paymentStateService = mock(AndroidPaymentStateService.class);
    private WalletService walletService = mock(WalletService.class);
    private PlayerService playerService = mock(PlayerService.class);
    private String sender = "from@yazino.com";
    private QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);
    private PlayerProfileService playerProfileService = mock(PlayerProfileService.class);

    private CreditPurchaseOperation underTest;
    private Order order;


    @Before
    public void setUp() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        underTest = new CreditPurchaseOperation(buyChipsPromotionService,
                paymentStateService, walletService, communityService, playerService, playerProfileService, emailer);

        DeveloperPayload devPayload = new DeveloperPayload();
        devPayload.setPurchaseId(INTERNAL_TRANSACTION_ID);
        ObjectMapper jsonMapper = new ObjectMapper();
        String devPayloadAsJson = jsonMapper.writeValueAsString(devPayload);
        order = new Order();
        order.setPurchaseState(GooglePurchaseOrderState.PURCHASED);
        order.setOrderId(ORDER_ID);
        order.setDeveloperPayload(devPayloadAsJson);
        order.setProductId(PRODUCT_ID);
        DateTime purchaseTime = PURCHASE_TIME;
        order.setPurchaseTime(purchaseTime);

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(PLAYER_PROFILE);
    }

    @Test
    public void shouldlogExternalTransaction() throws WalletServiceException, AndroidPaymentStateException, IOException, PurchaseException {
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        ExternalTransaction et = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(order.getOrderId())
                .withMessage("", new DateTime())
                .withAmount(CURRENCY, PRICE)
                .withPaymentOption(DEFAULT_CHIPS, null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(null)
                .withPlatform(Platform.ANDROID)
                .build();
        verify(walletService).record(et);
    }

    @Test
    public void shouldMarkPurchaseAsCredited() throws IOException, PurchaseException, AndroidPaymentStateException {
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        verify(paymentStateService).markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }


    @Test
    public void shouldCreditChipsBeforeMarkingTransactionAsCredited() throws Exception {
        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        ExternalTransaction et = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(order.getOrderId())
                .withMessage("", new DateTime())
                .withAmount(CURRENCY, PRICE)
                .withPaymentOption(DEFAULT_CHIPS, null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(null)
                .withPlatform(Platform.ANDROID)
                .build();
        InOrder inOrder = inOrder(walletService, paymentStateService);
        inOrder.verify(walletService).record(et);
        inOrder.verify(paymentStateService).markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }


    @Test
    public void shouldLogPromotionIfPaymentRecordAssociatedWithPromotion() throws PurchaseException {

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, PROMO_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        paymentRecord.setPromoId(PROMO_ID);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        verify(buyChipsPromotionService).logPlayerReward(eq(PLAYER_ID), eq(PROMO_ID), eq(DEFAULT_CHIPS), eq(PROMO_CHIPS), eq(GOOGLE_CHECKOUT), any(DateTime.class));
    }

    @Test
    public void shouldNotLogPromotionIfPaymentRecordNotAssociatedWithPromotion() throws PurchaseException {

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        paymentRecord.setPromoId(null);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        verifyZeroInteractions(buyChipsPromotionService);
    }

    @Test
    public void creditPurchaseSucceedsEventIfCouldNotMarkPaymentStateAsSuccess() throws AndroidPaymentStateException, PurchaseException {
        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        doThrow(new AndroidPaymentStateException("error"))
                .when(paymentStateService).markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        underTest.creditPurchase(paymentRecord, order, chipBundle);
    }

    @Test
    public void shouldThrowPurchaseExceptionIfCannotObtainPurchaseLock() throws AndroidPaymentStateException, PurchaseException {
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        doThrow(new AndroidPaymentStateException("error"))
                .when(paymentStateService).createCreditPurchaseLock(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        try {
            underTest.creditPurchase(paymentRecord, order, chipBundle);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.getErrorMessage(), equalTo("Failed to credit purchase. Could not mark payment state as CREDITING"));
        }
    }

    @Test
    public void shouldMarkPurchaseAsFailedIfCouldNotCreditPlayer() throws AndroidPaymentStateException, WalletServiceException, PurchaseException {
        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        doThrow(new WalletServiceException("error"))
                .when(walletService).record(any(ExternalTransaction.class));

        try {
            underTest.creditPurchase(paymentRecord, order, chipBundle);
            fail("Expected exception");
        } catch (PurchaseException e) {
            verify(paymentStateService).markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);
        }
    }

    @Test
    public void shouldThrowPurchaseExceptionIfCouldNotGiveChipsToPlayer() throws AndroidPaymentStateException, WalletServiceException, PurchaseException {
        Purchase expected = new Purchase();
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        doThrow(new WalletServiceException("error"))
                .when(walletService).record(any(ExternalTransaction.class));

        try {
            underTest.creditPurchase(paymentRecord, order, chipBundle);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.getErrorMessage(), equalTo("Failed to credit player with chips"));
        }
    }

    @Test
    public void shouldOverwriteFailedStateIfOrderStateIsPURCHASED() throws IOException, PurchaseException, AndroidPaymentStateException {
        AndroidPaymentStateDetails paymentRecord = createPaymentRecord();
        paymentRecord.setState(AndroidPaymentState.FAILED);

        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);

        underTest.creditPurchase(paymentRecord, order, chipBundle);

        verify(paymentStateService).markPurchaseAsCredited(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    private AndroidPaymentStateDetails createPaymentRecord() {
        AndroidPaymentStateDetails record = new AndroidPaymentStateDetails();
        record.setState(AndroidPaymentState.CREATED);
        record.setPlayerId(PLAYER_ID);
        record.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        record.setGameType(GAME_TYPE);
        return record;
    }

}
