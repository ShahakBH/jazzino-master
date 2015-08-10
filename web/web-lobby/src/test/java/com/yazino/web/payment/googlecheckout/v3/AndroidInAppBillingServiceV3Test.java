package com.yazino.web.payment.googlecheckout.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.android.AndroidPaymentState;
import com.yazino.platform.payment.android.AndroidPaymentStateDetails;
import com.yazino.platform.payment.android.AndroidPaymentStateException;
import com.yazino.platform.payment.android.AndroidPaymentStateService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.payment.Purchase;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.googlecheckout.AndroidInAppOrderSecurity;
import com.yazino.web.payment.chipbundle.ChipBundle;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.Partner.TANGO;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.web.payment.PurchaseStatus.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class AndroidInAppBillingServiceV3Test {
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(123456);
    public static final String PRODUCT_ID = "slots_usd15_buys_90k_p200";
    public static final BigDecimal DEFAULT_CHIPS = BigDecimal.valueOf(30000);
    public static final BigDecimal PROMO_CHIPS = BigDecimal.valueOf(90000);
    public static final long PROMO_ID = 987L;
    public static final String GAME_TYPE = "SLOTS";
    public static final String INTERNAL_TRANSACTION_ID = "sample internal transaction id";
    public static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(565656);
    public static final BigDecimal PRICE = BigDecimal.valueOf(15);
    public static final Currency CURRENCY = Currency.getInstance("USD");
    public static final DateTime REQUEST_TIME = new DateTime();
    public static final String MESSAGE = "sample message - txn cancelled";
    public static final String ORDER_ID = "orderID";
    public static final String SIGNATURE = "signature";
    public static final String USER_CANCELLED_MESSAGE = "sample message - user cancelled";

    private ObjectMapper jsonMapper;
    private Order order;
    private String orderData;
    private DeveloperPayload devPayload;

    @Mock
    AndroidPromotionServiceV3 promotionService;
    @Mock
    private JavaUUIDSource uuidSource;
    @Mock
    private AndroidPaymentStateService paymentStateService;
    @Mock
    private WalletService walletService;
    @Mock
    private PlayerService playerService;
    @Mock
    private ChipBundleResolver chipBundleResolver;
    @Mock
    private AndroidInAppOrderSecurity security;
    @Mock
    private CreditPurchaseOperation creditPurchaseOperation;

    private AndroidInAppBillingServiceV3 underTest;

    @Before
    public void init() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);
        MockitoAnnotations.initMocks(this);

        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new JodaModule());

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        devPayload = new DeveloperPayload();
        devPayload.setPurchaseId(INTERNAL_TRANSACTION_ID);
        order = new Order();
        String devPayloadAsJson = jsonMapper.writeValueAsString(devPayload);
        order.setPurchaseState(GooglePurchaseOrderState.PURCHASED);
        order.setOrderId(ORDER_ID);
        order.setDeveloperPayload(devPayloadAsJson);
        order.setProductId(PRODUCT_ID);
        order.setPurchaseTime(new DateTime().withZone(DateTimeZone.forID("UTC")));

        orderData = jsonMapper.writeValueAsString(order);

        underTest = new AndroidInAppBillingServiceV3(promotionService,
                uuidSource,
                paymentStateService,
                walletService,
                playerService,
                chipBundleResolver,
                security,
                creditPurchaseOperation);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    private void whenFindChipBundleForAnyProductReturnDefaultBundle() {
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
    }

    @Test
    public void purchaseRequestShouldHaveSTALE_PROMOTIONStatusWhenPromotionIsInvalid() throws PurchaseException {
        givenAndroidStoreProductWithoutPromotion();

        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID);

        assertThat(purchaseRequest.getStatus(), is(STALE_PROMOTION));
        assertThat(purchaseRequest.getErrorMessage(), is("Stale promotion (expired or unknown)"));
    }

    @Test
    public void purchaseRequestShouldHaveINVALID_PRODUCT_IDForUnknownProduct() throws PurchaseException {
        givenAndroidStoreProductWithoutPromotion();

        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);

        assertThat(purchaseRequest.getStatus(), is(FAILED));
        assertThat(purchaseRequest.getErrorMessage(), is("Unknown product id"));
        verifyZeroInteractions(paymentStateService, walletService);
    }

    @Test
    public void purchaseRequestShouldBeCreatedWithUniquePurchaseId() throws PurchaseException {
        when(uuidSource.getNewUUID()).thenReturn(INTERNAL_TRANSACTION_ID);
        givenAndroidStoreProductWithoutPromotion();
        givenChipBundleWithoutPromotion();
        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);

        assertThat(purchaseRequest.getPurchaseId(), is(INTERNAL_TRANSACTION_ID));
    }

    @Test
    public void purchaseRequestShouldBeCreatedWithCREATEDStatus() throws PurchaseException {
        when(uuidSource.getNewUUID()).thenReturn(INTERNAL_TRANSACTION_ID);
        givenAndroidStoreProductWithoutPromotion();
        givenChipBundleWithoutPromotion();
        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, null);

        assertThat(purchaseRequest.getStatus(), is(CREATED));
    }

    @Test
    public void aPaymentStateRecordShouldBeCreatedForAPurchaseRequest() throws AndroidPaymentStateException, PurchaseException {
        givenAndroidStoreProductWithPromotion();
        givenChipBundleWithPromotion();
        when(uuidSource.getNewUUID()).thenReturn(INTERNAL_TRANSACTION_ID);

        underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID);

        verify(paymentStateService).createPurchaseRequest(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);
    }

    @Test
    public void purchaseRequestShouldHaveStateFAILEDWhenPaymentStateRecordCouldNotBeCreated() throws AndroidPaymentStateException, PurchaseException {
        givenAndroidStoreProductWithPromotion();
        givenChipBundleWithPromotion();
        when(uuidSource.getNewUUID()).thenReturn(INTERNAL_TRANSACTION_ID);
        doThrow(new AndroidPaymentStateException("some error"))
                .when(paymentStateService)
                .createPurchaseRequest(PLAYER_ID, GAME_TYPE, INTERNAL_TRANSACTION_ID, PRODUCT_ID, PROMO_ID);

        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID);

        assertThat(purchaseRequest.getStatus(), is(FAILED));
        assertThat(purchaseRequest.getErrorMessage(), is("Could not create new purchase (some error)"));
    }

    @Test
    public void anExternalTransactionRequestShouldBeCreatedForAPurchaseRequest() throws PurchaseException, WalletServiceException {
        givenAndroidStoreProductWithPromotion();
        givenChipBundleWithPromotion();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        when(uuidSource.getNewUUID()).thenReturn(INTERNAL_TRANSACTION_ID);
        givenRequestTime();

        Purchase purchaseRequest = underTest.createPurchaseRequest(PLAYER_ID, GAME_TYPE, PRODUCT_ID, PROMO_ID);

        ExternalTransaction expectedExternalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("productId: " + PRODUCT_ID, REQUEST_TIME)
                .withAmount(CURRENCY, PRICE)
                .withPaymentOption(PROMO_CHIPS, null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.ANDROID)
                .build();
        verify(walletService).record(expectedExternalTransaction);
        assertThat(purchaseRequest.getStatus(), is(CREATED));
    }

    @Test
    public void logFailedTransactionShouldMarkPaymentStateAsFailed() throws AndroidPaymentStateException {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(PROMO_ID);
        expectedDetails.setState(AndroidPaymentState.CREATED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(expectedDetails);

        underTest.logFailedTransaction(INTERNAL_TRANSACTION_ID, "sample message - txn cancelled");

        verify(paymentStateService).markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    @Test
    public void aFAILEDExternalTransactionShouldBeCreatedWhenLoggingAFailedTransaction() throws AndroidPaymentStateException, WalletServiceException {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(PROMO_ID);
        expectedDetails.setState(AndroidPaymentState.CREATED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(expectedDetails);
        givenAndroidStoreProductWithPromotion();
        givenChipBundleWithPromotion();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        givenRequestTime();

        underTest.logFailedTransaction(INTERNAL_TRANSACTION_ID, MESSAGE);

        ExternalTransaction expectedExternalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage(MESSAGE, REQUEST_TIME)
                .withAmount(CURRENCY, PRICE)
                .withPaymentOption(PROMO_CHIPS, null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.ANDROID)
                .build();
        verify(walletService).record(expectedExternalTransaction);
    }

    @Test
    public void logCancelledTransactionShouldMarkPaymentStateAsCancelled() throws AndroidPaymentStateException {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(PROMO_ID);
        expectedDetails.setState(AndroidPaymentState.CANCELLED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(expectedDetails);

        underTest.logUserCancelledTransaction(INTERNAL_TRANSACTION_ID, USER_CANCELLED_MESSAGE);

        verify(paymentStateService).markPurchaseAsUserCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    @Test
    public void aCANCELLEDExternalTransactionShouldBeCreatedWhenLoggingACancelledTransaction() throws AndroidPaymentStateException, WalletServiceException {
        AndroidPaymentStateDetails expectedDetails = new AndroidPaymentStateDetails();
        expectedDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        expectedDetails.setGameType(GAME_TYPE);
        expectedDetails.setPlayerId(PLAYER_ID);
        expectedDetails.setProductId(PRODUCT_ID);
        expectedDetails.setPromoId(PROMO_ID);
        expectedDetails.setState(AndroidPaymentState.CREATED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(expectedDetails);
        givenAndroidStoreProductWithPromotion();
        givenChipBundleWithPromotion();
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
        givenRequestTime();

        underTest.logUserCancelledTransaction(INTERNAL_TRANSACTION_ID, USER_CANCELLED_MESSAGE);

        ExternalTransaction expectedExternalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage(USER_CANCELLED_MESSAGE, REQUEST_TIME)
                .withAmount(CURRENCY, PRICE)
                .withPaymentOption(PROMO_CHIPS, null)
                .withCreditCardNumber("none")
                .withCashierName("GoogleCheckout")
                .withStatus(ExternalTransactionStatus.CANCELLED)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.ANDROID)
                .build();
        verify(walletService).record(expectedExternalTransaction);
    }

    @Test
    public void canDeserializeOrder() throws IOException {
        String orderData = "{\"orderId\":\"12999763169054705758.1363011803587520\""
                + ",\"packageName\":\"air.com.yazino.android.extension.Example\""
                + ",\"productId\":\"slots_usd3_buys_5k\""
                + ",\"purchaseTime\":1374506402000,"
                + "\"purchaseState\":0,"
                + "\"developerPayload\":\"{\\\"purchaseId\\\":\\\"p1374506398141\\\"}\","
                + "\"purchaseToken\":\"ppygeqrrosfydwabjh\"}";

        Order expectedOrder = new Order();
        expectedOrder.setProductId("slots_usd3_buys_5k");
        expectedOrder.setPurchaseState(GooglePurchaseOrderState.PURCHASED);
        expectedOrder.setPackageName("air.com.yazino.android.extension.Example");
        expectedOrder.setOrderId("12999763169054705758.1363011803587520");
        expectedOrder.setPurchaseTime(new DateTime(1374506402000L, DateTimeZone.UTC));
        expectedOrder.setPurchaseToken("ppygeqrrosfydwabjh");
        expectedOrder.setDeveloperPayload("{\"purchaseId\":\"p1374506398141\"}");

        Order order = jsonMapper.readValue(orderData, Order.class);
        DeveloperPayload developerPayload = jsonMapper.readValue(order.getDeveloperPayload(), DeveloperPayload.class);

        assertThat(order, is(expectedOrder));
        assertThat(developerPayload.getPurchaseId(), is("p1374506398141"));
    }

    @Test
    public void creditPurchaseShouldInvokeCreditPurchaseOperationWhenOrderStateIsPURCHASED() throws WalletServiceException, AndroidPaymentStateException, IOException, PurchaseException {
        GooglePurchase expected = new GooglePurchase();
        expected.setCanConsume(true);
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentRecord = newPaymentStateDetails();
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentRecord);
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        GooglePurchase actual = underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);

        verify(creditPurchaseOperation).creditPurchase(paymentRecord, order, chipBundle);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void creditPurchaseForTangoShouldInvokeCreditPurchaseOperationWhenOrderStateIsPURCHASED() throws WalletServiceException, AndroidPaymentStateException, IOException, PurchaseException {

        GooglePurchase expected = new GooglePurchase();
        expected.setCanConsume(true);
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        when(security.verify(GAME_TYPE, orderData, SIGNATURE, TANGO)).thenReturn(true);
        AndroidPaymentStateDetails paymentRecord = newPaymentStateDetails();
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentRecord);
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        GooglePurchase actual = underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, TANGO);

        verify(creditPurchaseOperation).creditPurchase(paymentRecord, order, chipBundle);
        assertThat(actual, equalTo(expected));

    }

    @Test
    public void creditPurchaseFailsIfGameTypeIsMissing() throws PurchaseException {
        try {
            underTest.creditPurchase(null, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("invalid args"));
        }
    }

    @Test
    public void creditPurchaseFailsIfOrderDataIsMissing() throws PurchaseException {
        try {
            underTest.creditPurchase(GAME_TYPE, null, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("invalid args"));
        }
    }

    @Test
    public void creditPurchaseFailsIfSignatureIsMissing() throws PurchaseException {
        try {
            underTest.creditPurchase(GAME_TYPE, orderData, null, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("invalid args"));
        }
    }

    @Test
    public void creditPurchaseFailsIfSignatureCannotBeVerified() throws PurchaseException {
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(false);
        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Signature invalid"));
        }
    }

    @Test
    public void creditPurchaseFailsIfOrderDataIsInvalid() throws PurchaseException {
        String invalidOrderData = "not ok";
        when(security.verify(GAME_TYPE, invalidOrderData, SIGNATURE, YAZINO)).thenReturn(true);
        try {
            underTest.creditPurchase(GAME_TYPE, invalidOrderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Cannot deserialize order data"));
        }
    }

    @Test
    public void creditPurchaseFailsIfDeveloperPayloadIsInvalid() throws IOException, PurchaseException {
        order.setDeveloperPayload("not ok");
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Cannot deserialize developer payload"));
        }
    }

    @Test
    public void creditPurchaseFailsIfInternalTransactionIdNotPresent() throws IOException, PurchaseException {
        devPayload.setPurchaseId(null);
        order.setDeveloperPayload(jsonMapper.writeValueAsString(devPayload));
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("PurchaseId not present"));
        }
    }

    @Test
    public void creditPurchaseFailsIfCouldNotFindChipBundle() throws PurchaseException {
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(null);

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Unknown product id"));
        }
    }

    @Test
    public void creditPurchaseFailsIfPurchaseCannotBeFound() throws PurchaseException {
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        whenFindChipBundleForAnyProductReturnDefaultBundle();
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(null);

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Could not find transaction"));
        }
    }

    @Test
    public void creditPurchaseShouldRespondSuccessfullyWhenPreviouslyMarkedAsSuccessful() throws IOException, PurchaseException, AndroidPaymentStateException {
        GooglePurchase expected = new GooglePurchase();
        expected.setCanConsume(true);
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        order.setPurchaseState(GooglePurchaseOrderState.PURCHASED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentState = newPaymentStateDetails();
        paymentState.setState(AndroidPaymentState.CREDITED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentState);
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        GooglePurchase actual = underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);

        assertThat(actual, equalTo(expected));
        verifyZeroInteractions(creditPurchaseOperation);
    }

    @Test
    public void creditPurchaseShouldOverwriteFailedStatesIfOrderStateIsPURCHASED() throws IOException, PurchaseException, AndroidPaymentStateException {
        GooglePurchase expected = new GooglePurchase();
        expected.setCanConsume(true);
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentRecord = newPaymentStateDetails();
        paymentRecord.setState(AndroidPaymentState.FAILED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentRecord);
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        GooglePurchase actual = underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);

        verify(creditPurchaseOperation).creditPurchase(paymentRecord, order, chipBundle);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void creditPurchaseShouldOverwriteCancelledStatesIfOrderStateIsFAILED() throws IOException, PurchaseException, AndroidPaymentStateException {
        GooglePurchase expected = new GooglePurchase();
        expected.setCanConsume(true);
        expected.setStatus(SUCCESS);
        expected.setCurrencyCode(CURRENCY.toString());
        expected.setPrice(PRICE);
        expected.setChips(DEFAULT_CHIPS);

        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentRecord = newPaymentStateDetails();
        paymentRecord.setState(AndroidPaymentState.CANCELLED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentRecord);
        ChipBundle chipBundle = new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY);
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID)).thenReturn(chipBundle);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        GooglePurchase actual = underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);

        verify(creditPurchaseOperation).creditPurchase(paymentRecord, order, chipBundle);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void creditPurchaseShouldMarkPurchaseAsCancelledWhenOrderStateIsCANCELLED() throws IOException, PurchaseException, AndroidPaymentStateException {
        order.setPurchaseState(GooglePurchaseOrderState.CANCELED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(CANCELLED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting cancelled purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }

        verify(paymentStateService).markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    @Ignore
    @Test
    public void whatToDoIfSwitchBetweenCancelledAndFailed() {
    }

    @Test
    public void creditPurchaseShouldSkipMarkingPurchaseAsCancelledWhenOrderStateAlreadyCANCELLED() throws IOException, PurchaseException, AndroidPaymentStateException {
        order.setPurchaseState(GooglePurchaseOrderState.CANCELED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentState = newPaymentStateDetails();
        paymentState.setState(AndroidPaymentState.CANCELLED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentState);
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(CANCELLED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting cancelled purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }

        verify(paymentStateService, never()).markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    @Test
    public void creditPurchaseShouldMarkPurchaseAsFailedWhenOrderStateIsNeitherOKOrCANCELLED() throws IOException, PurchaseException, AndroidPaymentStateException {
        order.setPurchaseState(GooglePurchaseOrderState.REFUNDED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting failed purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }
    }

    @Test
    public void creditPurchaseShouldSkipMarkingPurchaseAsFailedWhenOrderStateIsAlreadyFailed() throws IOException, PurchaseException, AndroidPaymentStateException {
        order.setPurchaseState(GooglePurchaseOrderState.REFUNDED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        AndroidPaymentStateDetails paymentState = newPaymentStateDetails();
        paymentState.setState(AndroidPaymentState.FAILED);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(paymentState);
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting failed purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }

        verify(paymentStateService, never()).markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);
    }

    @Ignore
    @Test
    public void checkThatClientConsumesAndThrowsAwayFailedPurchases() {

    }

    @Test
    public void creditPurchaseFailsIfPurchaseStateIsCancelled() throws AndroidPaymentStateException, IOException, PurchaseException {
        order.setPurchaseState(GooglePurchaseOrderState.CANCELED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(CANCELLED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting cancelled purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }
    }

    @Test
    public void creditPurchaseFailsIfPurchaseStateIsFailed() throws IOException, PurchaseException {
        order.setPurchaseState(GooglePurchaseOrderState.REFUNDED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(true));
            assertThat(e.getErrorMessage(), equalTo("Not crediting failed purchase (orderId=" + ORDER_ID + "). Purchase state is NOT PURCHASED"));
        }
    }

    @Test
    public void creditPurchaseFailsIfCouldNotMarkPurchaseAsFailed() throws IOException, AndroidPaymentStateException, PurchaseException {
        order.setPurchaseState(GooglePurchaseOrderState.REFUNDED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();
        doThrow(new AndroidPaymentStateException("error"))
                .when(paymentStateService).markPurchaseAsFailed(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Failed to update payment state for orderId=" + ORDER_ID));
        }
    }

    @Test
    public void creditPurchaseFailsIfCouldNotMarkPurchaseAsCancelled() throws IOException, AndroidPaymentStateException, PurchaseException {
        order.setPurchaseState(GooglePurchaseOrderState.CANCELED);
        orderData = jsonMapper.writeValueAsString(order);
        when(security.verify(GAME_TYPE, orderData, SIGNATURE, YAZINO)).thenReturn(true);
        when(paymentStateService.findPaymentStateDetailsFor(INTERNAL_TRANSACTION_ID)).thenReturn(newPaymentStateDetails());
        whenFindChipBundleForAnyProductReturnDefaultBundle();
        doThrow(new AndroidPaymentStateException("error"))
                .when(paymentStateService).markPurchaseAsCancelled(PLAYER_ID, INTERNAL_TRANSACTION_ID);

        try {
            underTest.creditPurchase(GAME_TYPE, orderData, SIGNATURE, YAZINO);
            fail("Expected exception");
        } catch (PurchaseException e) {
            assertThat(e.getStatus(), equalTo(FAILED));
            assertThat(e.canConsume(), equalTo(false));
            assertThat(e.getErrorMessage(), equalTo("Failed to update payment state for orderId=" + ORDER_ID));
        }
    }

    private AndroidPaymentStateDetails newPaymentStateDetails() {
        AndroidPaymentStateDetails paymentDetails = new AndroidPaymentStateDetails();
        paymentDetails.setState(AndroidPaymentState.CREATED);
        paymentDetails.setPlayerId(PLAYER_ID);
        paymentDetails.setInternalTransactionId(INTERNAL_TRANSACTION_ID);
        paymentDetails.setGameType(GAME_TYPE);
        return paymentDetails;
    }

    private void givenChipBundleWithoutPromotion() {
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID))
                .thenReturn(new ChipBundle(PRODUCT_ID, DEFAULT_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY));
    }

    private void givenChipBundleWithPromotion() {
        when(chipBundleResolver.findChipBundleForProductId(GAME_TYPE, PRODUCT_ID))
                .thenReturn(new ChipBundle(PRODUCT_ID, PROMO_CHIPS, DEFAULT_CHIPS, PRICE, CURRENCY));
    }

    private void givenRequestTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(REQUEST_TIME.getMillis());
    }

    private void givenAndroidStoreProductWithoutPromotion() {
        AndroidStoreProducts products = new AndroidStoreProducts();
        products.addProduct(new AndroidStoreProduct(PRODUCT_ID, DEFAULT_CHIPS));
        when(promotionService.getAvailableProducts(PLAYER_ID, ANDROID, GAME_TYPE)).thenReturn(products);
    }

    private void givenAndroidStoreProductWithPromotion() {
        AndroidStoreProducts products = new AndroidStoreProducts();
        products.setPromoId(PROMO_ID);
        AndroidStoreProduct product = new AndroidStoreProduct(PRODUCT_ID, DEFAULT_CHIPS);
//        product.setPromoId(PROMO_ID);
        product.setPromoChips(PROMO_CHIPS);
        products.addProduct(product);
        when(promotionService.getAvailableProducts(PLAYER_ID, Platform.ANDROID, GAME_TYPE)).thenReturn(products);
    }
}
