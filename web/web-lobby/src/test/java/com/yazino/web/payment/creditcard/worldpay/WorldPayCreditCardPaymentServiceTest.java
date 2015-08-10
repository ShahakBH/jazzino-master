package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.MessageCode;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.PaymentTrustAuthorisationMessage;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.payment.CustomerData;
import com.yazino.web.payment.CustomerDataBuilder;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.creditcard.PurchaseOutcome;
import com.yazino.web.payment.creditcard.PurchaseRequest;
import com.yazino.web.payment.creditcard.PurchaseResult;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Currency;

import static com.yazino.platform.Platform.WEB;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.left;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorldPayCreditCardPaymentServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final Long PROMO_ID = 123l;
    private static final BigDecimal ACCOUNT_ID = valueOf(348);
    private static final BigDecimal SESSION_ID = valueOf(3484235);
    private static final String VISA_CARD = "4121011111111111";
    private static final String VISA_CARD_OBSCURED = "4121XXXXXXXX1111";
    private static final String EXPIRATION_MONTH = "06";
    private static final String EXPIRATION_YEAR = "11";
    private static final String CARD_HOLDER_NAME = "aTester";
    private static final String CVC_2 = "123";
    private static final Currency GBP = Currency.getInstance("GBP");
    private static final BigDecimal CASH_AMOUNT = valueOf(10);
    private static final String EMAIL_ADDRESS = "aCardHolder@example.com";
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String INTERNAL_TRANSACTION_ID = Long.toHexString(123456789012345678L).toUpperCase();
    private static final String EXTERNAL_TRANSACTION_ID = "503034900";
    private static final int CURRENT_TIME = 12345345;
    private static final int CHIPS_AMOUNT = 1000;
    private static final String MERCHANT_ID = "WorldPay";
    public static final BigDecimal PROMOTION_CHIPS = new BigDecimal("56780");
    private static final DateTime DATE_TIME = new DateTime();
    private static final String CARD_ID = "12345";

    @Mock
    private STLink stLink;
    @Mock
    private TransactionIdGenerator transactionIdGenerator;
    @Mock
    private WalletService walletService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private WorldPayCreditCardPaymentService underTest;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        underTest = new WorldPayCreditCardPaymentService(stLink, transactionIdGenerator, walletService, playerProfileService, yazinoConfiguration);

        when(transactionIdGenerator.generateNumericTransactionId()).thenReturn(Long.parseLong(INTERNAL_TRANSACTION_ID, 16));

        when(yazinoConfiguration.getBoolean("payment.worldpay.stlink.riskguardian.enabled", true)).thenReturn(true);
        when(yazinoConfiguration.getList(eq("payment.worldpay.stlink.auto-block.message-codes"), anyList())).thenReturn(asList((Object) 2401));

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_TIME);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void allPurchaseRequestsAreAccepted() {
        assertThat(underTest.accepts(aPurchaseRequest()), is(true));
    }

    @Test
    public void customerAndCreditCardDetailsAreCorrectlyPassedToRiskGuardian() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest());

        final ArgumentCaptor<NVPMessage> messageCaptor = ArgumentCaptor.forClass(NVPMessage.class);
        verify(stLink, times(2)).send(messageCaptor.capture());
        final NVPResponse parsedMessage = new NVPResponse("unused",
                messageCaptor.getAllValues().get(0).toObscuredMessage().replaceAll("StringIn=", ""));
        assertThat(parsedMessage.get("Amount").orNull(), is(equalTo(CASH_AMOUNT.toPlainString())));
        assertThat(parsedMessage.get("CurrencyId").orNull(), is(equalTo(Integer.toString(GBP.getNumericCode()))));
        assertThat(parsedMessage.get("TRXSource").orNull(), is(equalTo("10")));
        assertThat(parsedMessage.get("MOP").orNull(), is(equalTo("CC")));
        assertThat(parsedMessage.get("AcctName").orNull(), is(equalTo(CARD_HOLDER_NAME)));
        assertThat(parsedMessage.get("AcctNumber").orNull(), is(equalTo(VISA_CARD_OBSCURED)));
        assertThat(parsedMessage.get("CVN").orNull(), is(equalTo(CVC_2)));
        assertThat(parsedMessage.get("CountryCode").orNull(), is(equalTo("UK")));
        assertThat(parsedMessage.get("Email").orNull(), is(equalTo(EMAIL_ADDRESS)));
        assertThat(parsedMessage.get("ExpDate").orNull(), is(equalTo("062011")));
        assertThat(parsedMessage.get("REMOTE_ADDR").orNull(), is(equalTo("10.9.8.16")));
    }

    @Test
    public void customerAndCreditCardDetailsAreCorrectlyPassedToPaymentTrust() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest());

        final ArgumentCaptor<NVPMessage> messageCaptor = ArgumentCaptor.forClass(NVPMessage.class);
        verify(stLink, times(2)).send(messageCaptor.capture());
        final NVPResponse parsedMessage = new NVPResponse("unused",
                messageCaptor.getAllValues().get(1).toObscuredMessage().replaceAll("StringIn=", ""));
        assertThat(parsedMessage.get("Amount").orNull(), is(equalTo(CASH_AMOUNT.toPlainString())));
        assertThat(parsedMessage.get("CurrencyId").orNull(), is(equalTo(Integer.toString(GBP.getNumericCode()))));
        assertThat(parsedMessage.get("TRXSource").orNull(), is(equalTo("4")));
        assertThat(parsedMessage.get("MOP").orNull(), is(equalTo("CC")));
        assertThat(parsedMessage.get("AcctName").orNull(), is(equalTo(CARD_HOLDER_NAME)));
        assertThat(parsedMessage.get("AcctNumber").orNull(), is(equalTo(VISA_CARD_OBSCURED)));
        assertThat(parsedMessage.get("CVN").orNull(), is(equalTo(CVC_2)));
        assertThat(parsedMessage.get("CountryCode").orNull(), is(equalTo("UK")));
        assertThat(parsedMessage.get("Email").orNull(), is(equalTo(EMAIL_ADDRESS)));
        assertThat(parsedMessage.get("ExpDate").orNull(), is(equalTo("062011")));
        assertThat(parsedMessage.get("REMOTE_ADDR").orNull(), is(equalTo("10.9.8.16")));
    }

    @Test
    public void a4DigitExpiryDateIsTrimmedTwo2DigitsBeforePassingToTheSTLink() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());
        final CustomerData customerData = new CustomerDataBuilder()
                .withAmount(CASH_AMOUNT)
                .withCurrency(GBP)
                .withTransactionCountry("UK")
                .withCreditCardNumber(VISA_CARD)
                .withCvc2(CVC_2)
                .withExpirationMonth(EXPIRATION_MONTH)
                .withExpirationYear("20" + EXPIRATION_YEAR)
                .withCardHolderName(CARD_HOLDER_NAME)
                .withCustomerIPAddress(aHost())
                .withEmailAddress(EMAIL_ADDRESS)
                .withGameType(GAME_TYPE).build();
        final PurchaseRequest purchaseRequest = new PurchaseRequest(customerData, ACCOUNT_ID, paymentOption(false), DATE_TIME, PLAYER_ID, SESSION_ID, PROMO_ID);

        underTest.purchase(purchaseRequest);

        final ArgumentCaptor<NVPMessage> messageCaptor = ArgumentCaptor.forClass(NVPMessage.class);
        verify(stLink, times(2)).send(messageCaptor.capture());
        final NVPResponse parsedMessage = new NVPResponse("unused", messageCaptor.getAllValues().get(1).toObscuredMessage());
        assertThat(parsedMessage.get("ExpDate").orNull(), is(equalTo("062011")));
    }

    @Test
    public void a1DigitExpiryDateIsPaddedWithOnesBeforePassingToSTLink() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());
        final CustomerData customerData = new CustomerDataBuilder()
                .withAmount(CASH_AMOUNT)
                .withCurrency(GBP)
                .withTransactionCountry("UK")
                .withCreditCardNumber(VISA_CARD)
                .withCvc2(CVC_2)
                .withExpirationMonth(EXPIRATION_MONTH)
                .withExpirationYear("1")
                .withCardHolderName(CARD_HOLDER_NAME)
                .withCustomerIPAddress(aHost())
                .withEmailAddress(EMAIL_ADDRESS)
                .withGameType(GAME_TYPE).build();
        final PurchaseRequest purchaseRequest = new PurchaseRequest(customerData, ACCOUNT_ID, paymentOption(false), DATE_TIME, PLAYER_ID, SESSION_ID, PROMO_ID);

        underTest.purchase(purchaseRequest);

        final ArgumentCaptor<NVPMessage> messageCaptor = ArgumentCaptor.forClass(NVPMessage.class);
        verify(stLink, times(2)).send(messageCaptor.capture());
        final NVPResponse parsedMessage = new NVPResponse("unused", messageCaptor.getAllValues().get(1).toObscuredMessage());
        assertThat(parsedMessage.get("ExpDate").orNull(), is(equalTo("062011")));
    }

    @Test
    public void aSystemErrorOutcomeIsReturnedWhenRiskGuardianThrowsAnException() {
        final RuntimeException anException = new RuntimeException("anExceptionFromRiskGuardian");
        when(stLink.send(any(NVPMessage.class))).thenThrow(anException);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(anException))));
    }

    @Test
    public void aSystemErrorOutcomeIsReturnedWhenPaymentTrustThrowsAnException() {
        final RuntimeException anException = new RuntimeException("anExceptionFromPaymentTrust");
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenThrow(anException);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(anException))));
    }

    @Test
    public void anApprovedResponseReturnsTheCorrectOutcome() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.APPROVED, anApprovedResponse(), PurchaseOutcome.APPROVED))));
    }

    @Test
    public void anApprovedResponseReturnsTheCorrectOutcomeWithPromotionChips() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest(true));

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.APPROVED, anApprovedResponse(), PurchaseOutcome.APPROVED, true))));
    }

    @Test
    public void anDeclinedResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.NOT_AUTHORISED);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.NOT_AUTHORISED, response, PurchaseOutcome.DECLINED))));
    }

    @Test
    public void anDeclinedResponseReturnsTheCorrectOutcomeWithPromotionChips() {
        final NVPResponse response = errorResponse(MessageCode.NOT_AUTHORISED);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest(true));

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.NOT_AUTHORISED, response, PurchaseOutcome.DECLINED, true))));
    }

    @Test
    public void aReferredResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.CALL_BANK);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.CALL_BANK, response, PurchaseOutcome.REFERRED))));
    }

    @Test
    public void aReferredResponseReturnsTheCorrectOutcomeWithPromotionChips() {
        final NVPResponse response = errorResponse(MessageCode.CALL_BANK);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest(true));

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.CALL_BANK, response, PurchaseOutcome.REFERRED, true))));
    }

    @Test
    public void anInvalidAccountNumberResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.INVALID_CARD_NUMBER);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.INVALID_CARD_NUMBER, response, PurchaseOutcome.INVALID_ACCOUNT))));
    }

    @Test
    public void anInvalidExpiryDateResponseIsReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.INVALID_CREDIT_CARD_EXPIRY);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.INVALID_CREDIT_CARD_EXPIRY, response, PurchaseOutcome.INVALID_EXPIRY))));
    }

    @Test
    public void anInsufficientFundsResponseIsReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.INSUFFICIENT_FUNDS);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.INSUFFICIENT_FUNDS, response, PurchaseOutcome.INSUFFICIENT_FUNDS))));
    }

    @Test
    public void aTransactionLimitExceededResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.OVER_CREDIT_LIMIT);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.OVER_CREDIT_LIMIT, response, PurchaseOutcome.EXCEEDS_TRANSACTION_LIMIT))));
    }

    @Test
    public void aFailedCSVCheckResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.INVALID_CVN_VALUE);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.INVALID_CVN_VALUE, response, PurchaseOutcome.CSC_CHECK_FAILED))));
    }

    @Test
    public void aFailedRiskGuardianResponseReturnsTheCorrectOutcome() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aFailingRiskGuardianResponse());

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.OKAY, aFailingRiskGuardianResponse(), PurchaseOutcome.RISK_FAILED))));
    }

    @Test
    public void aFraudDeclinedByMerchantResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.CARD_STOLEN);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.CARD_STOLEN, response, PurchaseOutcome.LOST_OR_STOLEN_CARD))));
    }

    @Test
    public void aSystemFailureResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.SYSTEM_ERROR);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.SYSTEM_ERROR, response, PurchaseOutcome.SYSTEM_FAILURE))));
    }

    @Test
    public void anUnknownFailureResponseReturnsTheCorrectOutcome() {
        final NVPResponse response = errorResponse(MessageCode.BANK_TIMEOUT_ERROR);

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.BANK_TIMEOUT_ERROR, response, PurchaseOutcome.UNKNOWN))));
    }

    @Test
    public void aRiskGuardianRejectionPostsAnExternalTransactionToTheWallet()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aFailingRiskGuardianResponse());

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("", DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(WEB)
                .build());
        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withMessage(dump(aFailingRiskGuardianResponse()), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(WEB)
                .withFailureReason("RiskGuardian rejection")
                .build());
    }

    @Test
    public void anErrorResponseWithAMessageCodeInTheBlockListBlocksThePlayersAccount()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(errorResponse(MessageCode.FRAUD_SUSPECTED));

        underTest.purchase(aPurchaseRequest());

        verify(playerProfileService).updateStatus(PLAYER_ID, PlayerProfileStatus.BLOCKED, "system",
                "Blocked on WorldPay response 2401:Fraud suspected.");
    }

    @Test
    public void anErrorResponseWithAMessageCodeInTheBlockListReturnsTheBlockedOutcome()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(errorResponse(MessageCode.FRAUD_SUSPECTED));

        final PurchaseResult result = underTest.purchase(aPurchaseRequest());

        assertThat(result.getOutcome(), is(equalTo(PurchaseOutcome.PLAYER_BLOCKED)));
    }

    @Test
    public void anApprovedTransactionPostsAnExternalTransactionToTheWallet()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("", DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(WEB)
                .build());
        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withMessage(dump(anApprovedResponse()), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.AUTHORISED)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(WEB)
                .build());
    }

    @Test
    public void anApprovedTransactionWillNotBlockThePlayer()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest());

        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void aTransactionPostsAnExternalTransactionRequestToTheWallet()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("", DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .build());
    }

    @Test
    public void anApprovedTransactionPostsAnExternalTransactionToTheWalletWithPromotionChips()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        underTest.purchase(aPurchaseRequest(true));

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withMessage(dump(anApprovedResponse()), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(PROMOTION_CHIPS, "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.AUTHORISED)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .build());
    }

    @Test
    public void aDeclinedTransactionPostsAnExternalTransactionToTheWallet()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(aDeclinedResponse());

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withMessage(dump(aDeclinedResponse()), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .withFailureReason("Transaction NOT Authorized.")
                .build());
    }

    @Test
    public void aDeclinedTransactionWillNotBlockThePlayer()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(aDeclinedResponse());

        underTest.purchase(aPurchaseRequest());

        verifyZeroInteractions(playerProfileService);
    }

    @Test
    public void aDeclinedTransactionPostsAnExternalTransactionWithPromotionChipsToTheWallet()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(aDeclinedResponse());

        underTest.purchase(aPurchaseRequest(true));

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withMessage(dump(aDeclinedResponse()), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(PROMOTION_CHIPS, "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .withFailureReason("Transaction NOT Authorized.")
                .build());
    }

    @Test
    public void whenRiskGuardianThrowsAnExceptionAnExternalTransactionIsLoggedToTheWallet()
            throws WalletServiceException {
        final RuntimeException anException = new RuntimeException("anExceptionFromRiskGuardian");
        when(stLink.send(any(NVPMessage.class))).thenThrow(anException);

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage(dump(anException), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.ERROR)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .withFailureReason(anException.getMessage())
                .build());
    }

    @Test
    public void whenPaymentTrustThrowsAnExceptionAnExternalTransactionIsLoggedToTheWallet()
            throws WalletServiceException {
        final RuntimeException anException = new RuntimeException("anExceptionFromPaymentTrust");
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenThrow(anException);

        underTest.purchase(aPurchaseRequest());

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(INTERNAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage(dump(anException), DATE_TIME)
                .withAmount(GBP, CASH_AMOUNT)
                .withPaymentOption(valueOf(CHIPS_AMOUNT), "test-id")
                .withCreditCardNumber(VISA_CARD_OBSCURED)
                .withCashierName("WorldPay")
                .withStatus(ExternalTransactionStatus.ERROR)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .withFailureReason(anException.getMessage())
                .build());
    }

    @Test
    public void whenPostingATransactionThrowsAnExceptionItIsIgnored()
            throws WalletServiceException {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(aDeclinedResponse());
        final RuntimeException anException = new RuntimeException("anExceptionFromTheWallet");
        doThrow(anException).when(walletService).record(any(ExternalTransaction.class));

        final PurchaseResult purchase = underTest.purchase(aPurchaseRequest());

        assertThat(purchase, is(equalTo(aPurchaseResultWith(
                MessageCode.NOT_AUTHORISED, aDeclinedResponse(), PurchaseOutcome.DECLINED))));
    }

    @Test
    public void whenPaymentIsMadeWithACardIdTheNVPMessageShouldNotContainCreditCardInformation() {
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(anApprovedResponse());

        final PurchaseRequest purchaseRequest = aPurchaseRequestWithCardId();
        final CustomerData customerData = purchaseRequest.getCustomerData();
        underTest.purchase(purchaseRequest);

        verify(stLink, times(1)).send(new PaymentTrustAuthorisationMessage()
                .withValue("OrderNumber", left(INTERNAL_TRANSACTION_ID, 35))
                .withValue("Email", customerData.getEmailAddress())
                .withValue("CurrencyId", customerData.getCurrency().getNumericCode())
                .withValue("Amount", purchaseRequest.getPaymentOption().getAmountRealMoneyPerPurchase())
                .withValue("CountryCode", customerData.getTransactionCountryISO3166())
                .withValue("REMOTE_ADDR", customerData.getCustomerIPAddress().getHostAddress())
                .withValue("CustomerId", purchaseRequest.getPlayerId())
                .withValue("CardId", customerData.getCardId()));
    }

    private PurchaseResult aPurchaseResultWith(final MessageCode expectedCode,
                                               final NVPResponse nvpResponse,
                                               final PurchaseOutcome purchaseOutcome) {
        return aPurchaseResultWith(expectedCode, nvpResponse, purchaseOutcome, false);
    }

    private PurchaseResult aPurchaseResultWith(final MessageCode expectedCode,
                                               final NVPResponse nvpResponse,
                                               final PurchaseOutcome purchaseOutcome,
                                               final boolean withPromotion) {
        return new PurchaseResult(MERCHANT_ID,
                purchaseOutcome,
                EMAIL_ADDRESS,
                "PT".equals(nvpResponse.get("TransactionType").or("PT")) ? expectedCode.getDescription() : "",
                GBP,
                CASH_AMOUNT,
                withPromotion ? PROMOTION_CHIPS : valueOf(CHIPS_AMOUNT),
                VISA_CARD_OBSCURED,
                INTERNAL_TRANSACTION_ID,
                EXTERNAL_TRANSACTION_ID,
                dump(nvpResponse));
    }

    private PurchaseResult aPurchaseResultWith(final Throwable t) {
        return new PurchaseResult(MERCHANT_ID,
                PurchaseOutcome.SYSTEM_FAILURE,
                EMAIL_ADDRESS,
                "Transaction Failed.",
                GBP,
                CASH_AMOUNT,
                valueOf(CHIPS_AMOUNT),
                VISA_CARD_OBSCURED,
                INTERNAL_TRANSACTION_ID,
                null,
                dump(t));
    }

    private String dump(final Throwable exception) {
        final StringWriter exceptionOutput = new StringWriter();
        exception.printStackTrace(new PrintWriter(exceptionOutput));
        return exceptionOutput.toString();
    }

    private PurchaseRequest aPurchaseRequest() {
        return aPurchaseRequest(false);
    }

    private PurchaseRequest aPurchaseRequest(boolean withPromotion) {
        return new PurchaseRequest(customerData(), ACCOUNT_ID, paymentOption(withPromotion), DATE_TIME, PLAYER_ID,
                SESSION_ID, PROMO_ID);
    }

    private PurchaseRequest aPurchaseRequestWithCardId() {
        return aPurchaseRequestWithCardId(false);
    }


    private PurchaseRequest aPurchaseRequestWithCardId(boolean withPromotion) {
        return new PurchaseRequest(customerDataWithCardId(), ACCOUNT_ID, paymentOption(withPromotion), DATE_TIME, PLAYER_ID,
                SESSION_ID, PROMO_ID);
    }

    private CustomerData customerData() {
        return new CustomerDataBuilder()
                .withAmount(CASH_AMOUNT)
                .withCurrency(GBP)
                .withTransactionCountry("UK")
                .withCreditCardNumber(VISA_CARD)
                .withCvc2(CVC_2)
                .withExpirationMonth(EXPIRATION_MONTH)
                .withExpirationYear(EXPIRATION_YEAR)
                .withCardHolderName(CARD_HOLDER_NAME)
                .withCustomerIPAddress(aHost())
                .withEmailAddress(EMAIL_ADDRESS)
                .withGameType(GAME_TYPE).build();
    }

    private CustomerData customerDataWithCardId() {
        return new CustomerDataBuilder()
                .withCardId(CARD_ID)
                .withAmount(CASH_AMOUNT)
                .withExpirationMonth(EXPIRATION_MONTH)
                .withExpirationYear(EXPIRATION_YEAR)
                .withCardHolderName(CARD_HOLDER_NAME)
                .withTransactionCountry("UK")
                .withCurrency(GBP)
                .withCustomerIPAddress(aHost())
                .withEmailAddress(EMAIL_ADDRESS)
                .withGameType(GAME_TYPE).build();
    }

    private PaymentOption paymentOption(boolean withPromotion) {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setRealMoneyCurrency(GBP.getCurrencyCode());
        paymentOption.setId("test-id");
        paymentOption.setAmountRealMoneyPerPurchase(CASH_AMOUNT);
        paymentOption.setNumChipsPerPurchase(valueOf(CHIPS_AMOUNT));
        if (withPromotion) {
            PromotionPaymentOption ppo = new PromotionPaymentOption(PaymentPreferences.PaymentMethod.CREDITCARD, 1l, PROMOTION_CHIPS, "", "");
            paymentOption.addPromotionPaymentOption(ppo);
        }
        return paymentOption;
    }

    private String dump(final NVPResponse response) {
        return String.format("Req=%s;Res=%s", response.getRequestString(), response.getResponseString());
    }

    private NVPResponse anApprovedResponse() {
        return new NVPResponse("aRequest",
                "~MerchantId^200161~TransactionType^PT~OrderNumber^344_20130917T155258997_10.9.8.204_4~StrId^803615040"
                        + "~PTTID^503034900~MOP^CC~CurrencyId^826~Amount^10.00~AuthCode^B4A835~RequestType^A"
                        + "~MessageCode^2100~Message^Transaction Approved.~CVNMessageCode^2~CVNMessage^No Data Matched ");
    }

    private NVPResponse aDeclinedResponse() {
        return new NVPResponse("aRequest",
                "~MerchantId^200161~TransactionType^PT~OrderNumber^344_20130917T155258997_10.9.8.204_4~StrId^803615040"
                        + "~PTTID^503034900~MOP^CC~CurrencyId^826~Amount^10.00~AuthCode^B4A835~RequestType^A"
                        + "~MessageCode^2200~Message^Transaction NOT Authorized.~CVNMessageCode^2~CVNMessage^No Data Matched ");
    }

    private NVPResponse aPassingRiskGuardianResponse() {
        return new NVPResponse("aRGRequest", "~MerchantId^200161~TransactionType^RG~GttId^503034900~MessageCode^100~tScore^60~tRisk^70~");
    }

    private NVPResponse aFailingRiskGuardianResponse() {
        return new NVPResponse("aRGRequest", "~MerchantId^200161~TransactionType^RG~RGID^503034900~MessageCode^100~tScore^80~tRisk^70~");
    }

    private NVPResponse errorResponse(final MessageCode errorCode) {
        final NVPResponse response = new NVPResponse("unused",
                String.format("~TransactionType^PT~PTTID^503034900~MessageCode^%s~Message^%s~", errorCode.getCode(), errorCode.getDescription()));
        when(stLink.send(any(NVPMessage.class)))
                .thenReturn(aPassingRiskGuardianResponse())
                .thenReturn(response);
        return response;
    }

    private static InetAddress aHost() {
        try {
            return Inet4Address.getByAddress(new byte[]{10, 9, 8, 16});
        } catch (UnknownHostException e) {
            throw new RuntimeException("Couldn't create IPv4 address", e);
        }
    }

}
