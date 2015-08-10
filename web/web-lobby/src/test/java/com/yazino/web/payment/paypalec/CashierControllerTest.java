package com.yazino.web.payment.paypalec;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentStateException;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.PAYPAL;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.Paypal;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CashierControllerTest {
    private static final DateTimeFormatter FMT = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final long PROMO_ID = 1l;
    private static final BigDecimal PROMOTION_CHIPS_PER_PURCHASE = new BigDecimal("34000");
    private static final String HEADER = "header";
    private static final String TEXT = "text";
    private static final BigDecimal NUM_CHIPS_PER_PURCHASE = new BigDecimal("21000");
    private static final String PAYPAL_ENV = "play-in-a-sandbox";
    private static final String PAY_PAL_TOKEN = "pay pal token";
    private static final String PAYER_ID = "payer id";
    private static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "BLACKJACK";
    private static final String TXN_ID = "TXN_ID";
    private static final DateTime DATE_TIME = new DateTime(2011, 12, 21, 12, 1, 2, 0);
    private static final String TIME_STAMP = DATE_TIME.toString("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String LOCAL_TRANSACTION_ID = "aLocalTransactionId";

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private CookieHelper cookieHelper;
    @Mock
    private WalletService walletService;
    @Mock
    private PaypalRequester paypalRequester;
    @Mock
    private BuyChipsPromotionService promotionService;
    @Mock
    private CommunityService communityService;
    @Mock
    private PlayerService playerService;
    @Mock
    private CashierConfig cashierConfig;
    @Mock
    private ExpressCheckoutDetails expressCheckoutDetails;
    @Mock
    private ExpressCheckoutPayment expressCheckoutPayment;
    @Mock
    private TransactionIdGenerator transactionIdGenerator;
    @Mock
    private QuietPlayerEmailer emailer;

    private String baseUrl;

    private MockHttpServletRequest request;

    private CashierController underTest;

    private PaymentOption paymentOption;
    private PaymentOption paymentOptionWithPromotion;
    private PromotionPaymentOption promotionPaymentOption;

    @Mock
    private PurchaseTracking purchaseTracking;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        final LobbySession lobbySession = new LobbySession(SESSION_ID, PLAYER_ID, "playerName",
                "senetSessionKey",
                Partner.YAZINO,
                "pictureUrl",
                "email",
                null, false,
                Platform.WEB, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(Matchers.<HttpServletRequest>any())).willReturn(lobbySession);
        given(cashierConfig.getPaypalApiEnvironment()).willReturn(PAYPAL_ENV);
        given(transactionIdGenerator.generateTransactionId(ACCOUNT_ID)).willReturn(LOCAL_TRANSACTION_ID);
        given(playerService.getAccountId(PLAYER_ID)).willReturn(ACCOUNT_ID);
        given(cookieHelper.getPaymentGameType(Matchers.<Cookie[]>any())).willReturn(GAME_TYPE);

        underTest = new CashierController(promotionService, paypalRequester, lobbySessionCache, walletService,
                communityService, playerService, cookieHelper, cashierConfig, purchaseTracking, emailer,
                transactionIdGenerator);
        underTest.afterPropertiesSet();

        request = new MockHttpServletRequest();
        request.setServerName("yazinotest.com");
        request.setServerPort(8080);
        request.setContextPath("/paypal");
        baseUrl = "http://yazinotest.com:8080/paypal/payment/paypal-ec";

        paymentOption = new PaymentOption();
        paymentOption.setId("option2");
        paymentOption.setRealMoneyCurrency("USD");
        paymentOption.setAmountRealMoneyPerPurchase(new BigDecimal(10));
        paymentOption.setNumChipsPerPurchase(NUM_CHIPS_PER_PURCHASE);
        paymentOptionWithPromotion = new PaymentOption();
        paymentOptionWithPromotion.setId("optionWithPromo");
        paymentOptionWithPromotion.setRealMoneyCurrency("GBP");
        paymentOptionWithPromotion.setAmountRealMoneyPerPurchase(new BigDecimal(15));
        paymentOptionWithPromotion.setNumChipsPerPurchase(new BigDecimal("1000"));
        promotionPaymentOption = new PromotionPaymentOption(PAYPAL, PROMO_ID, PROMOTION_CHIPS_PER_PURCHASE, HEADER,
                TEXT);
        paymentOptionWithPromotion.addPromotionPaymentOption(promotionPaymentOption);
    }

    @After
    public void resetDateTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void successfulPaymentShouldSendSuccessEmail() throws Exception {
        initSuccessfulTxnWithDefaultChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", FMT.parseDateTime(TIME_STAMP))
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build());
        verifyNoMoreInteractions(walletService);

        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);

        verify(emailer).quietlySendEmail(captor.capture());

        BoughtChipsEmailBuilder builder = captor.getValue();
        assertEquals("email", builder.getEmailAddress());
        assertEquals("playerName", builder.getFirstName());
        assertEquals(NUM_CHIPS_PER_PURCHASE, builder.getPurchasedChips());
        assertEquals(Currency.getInstance("USD"), builder.getCurrency());
        assertEquals(new BigDecimal("10"), builder.getCost());
        assertEquals("", builder.getCardNumber());
        assertEquals(TXN_ID, builder.getPaymentId());
        assertEquals(Paypal, builder.getPaymentEmailBodyTemplate());
    }

    @Test(expected = NullPointerException.class)
    public void promotionServiceCannotBeNull() {
        new CashierController(null, paypalRequester, lobbySessionCache, walletService, communityService, playerService,
                cookieHelper, cashierConfig, purchaseTracking, emailer, transactionIdGenerator);
    }

    @Test
    public void shouldReturnErrorViewWhenPaymentOptionMissing() throws IOException {
        final ModelAndView modelAndView = underTest.processPayment(null, null, request);
        assertThat(modelAndView.getViewName(), is("payment/paypal-ec/error"));
    }

    @Test
    public void shouldReturnErrorViewWhenPromoChipsMissing() throws IOException {
        final ModelAndView modelAndView = underTest.processPayment("optionUSD1", 1l, request);
        assertThat(modelAndView.getViewName(), is("payment/paypal-ec/error"));
    }

    @Test
    public void processPaymentShouldReturnErrorViewWhenPaymentOptionNotloaded() throws IOException {
        ModelAndView mav = underTest.processPayment("optionUSD1", null, request);
        assertThat(mav.getViewName(), is("payment/paypal-ec/error"));
        String errorCode = (String) mav.getModel().get("errorCode");
        assertThat(errorCode, is("unknown.paymentOption"));
    }

    @Test
    public void processPaymentShouldCallExpressCheckoutWithPaymentOptionId() throws IOException, PaypalRequestException {
        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
        String returnUrl = String.format("%s/return?paymentOptionId=%s", baseUrl, paymentOption.getId());
        String cancelUrl = String.format("%s/cancel?paymentOptionId=%s", baseUrl, paymentOption.getId());
        given(paypalRequester.setExpressCheckout(returnUrl, cancelUrl, LOCAL_TRANSACTION_ID,
                paymentOption.getAmountRealMoneyPerPurchase().toPlainString(),
                paymentOption.getRealMoneyCurrency(), NUM_CHIPS_PER_PURCHASE)).willReturn(PAY_PAL_TOKEN);

        final ModelAndView mav = underTest.processPayment(paymentOption.getId(), null, request);
        String view = String.format("https://www.%s.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=%s",
                PAYPAL_ENV, PAY_PAL_TOKEN);

        assertThat(((RedirectView) mav.getView()).getUrl(), is(equalTo(view)));
        assertThat(((RedirectView) mav.getView()).isExposePathVariables(), is(false));
    }

    @Test
    public void processPaymentShouldLogAnExternalTransaction() throws IOException, PaypalRequestException, WalletServiceException {
        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
        String returnUrl = String.format("%s/return?paymentOptionId=%s", baseUrl, paymentOption.getId());
        String cancelUrl = String.format("%s/cancel?paymentOptionId=%s", baseUrl, paymentOption.getId());
        given(paypalRequester.setExpressCheckout(returnUrl, cancelUrl, LOCAL_TRANSACTION_ID,
                paymentOption.getAmountRealMoneyPerPurchase().toPlainString(),
                paymentOption.getRealMoneyCurrency(), NUM_CHIPS_PER_PURCHASE)).willReturn(PAY_PAL_TOKEN);

        underTest.processPayment(paymentOption.getId(), null, request);

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("x-x-x", new DateTime())
                .withAmount(Currency.getInstance("USD"), BigDecimal.valueOf(10))
                .withPaymentOption(NUM_CHIPS_PER_PURCHASE, null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build());
    }

    @Test
    public void processPaymentShouldCallExpressCheckoutWithPromoIdAndChips() throws IOException, PaypalRequestException {
        given(promotionService.getPaymentOptionFor(PLAYER_ID, promotionPaymentOption.getPromoId(),
                promotionPaymentOption.getPromotionPaymentMethod(),
                paymentOptionWithPromotion.getId())).willReturn(paymentOptionWithPromotion);
        String returnUrl = String.format("%s/return?paymentOptionId=%s&promoId=%s&promoChips=%s",
                baseUrl, paymentOptionWithPromotion.getId(), promotionPaymentOption.getPromoId(),
                promotionPaymentOption.getPromotionChipsPerPurchase().toPlainString());
        String cancelUrl = String.format("%s/cancel?paymentOptionId=%s&promoId=%s&promoChips=%s",
                baseUrl, paymentOptionWithPromotion.getId(), promotionPaymentOption.getPromoId(),
                promotionPaymentOption.getPromotionChipsPerPurchase().toPlainString());
        given(paypalRequester.setExpressCheckout(returnUrl, cancelUrl, LOCAL_TRANSACTION_ID,
                paymentOptionWithPromotion.getAmountRealMoneyPerPurchase().toPlainString(),
                paymentOptionWithPromotion.getRealMoneyCurrency(), PROMOTION_CHIPS_PER_PURCHASE)).willReturn(PAY_PAL_TOKEN);

        final ModelAndView mav = underTest.processPayment(paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(), request);
        String view = String.format("https://www.%s.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=%s",
                PAYPAL_ENV, PAY_PAL_TOKEN);
        assertThat(((RedirectView) mav.getView()).getUrl(), is(equalTo(view)));
        assertThat(((RedirectView) mav.getView()).isExposePathVariables(), is(false));
    }

    @Test
    public void testCancel() {
        ModelAndView mav = underTest.cancel(request);
        ModelAndViewAssert.assertViewName(mav, "payment/paypal-ec/cancel");
        ModelAndViewAssert.assertModelAttributeAvailable(mav, "assetUrl");
        ModelAndViewAssert.assertModelAttributeAvailable(mav, "gameType");
    }

    @Test
    public void returnFromPaypalShouldReturnErrorViewWhenGettingExpressCheckoutDetailsFails() throws WalletServiceException, IOException, PaypalRequestException {
        given(paypalRequester.getExpressCheckoutDetails(PAY_PAL_TOKEN)).willThrow(new PaypalRequestException());

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, "optionid", null, request);

        assertThat(mav.getViewName(), is("payment/paypal-ec/error"));
    }

    @Test
    public void returnFromPaypalShouldReturnErrorViewWhenCheckingOutFails() throws WalletServiceException, IOException, PaypalRequestException {
        initSuccessfulTxnWithDefaultChips();
        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
        when(expressCheckoutDetails.getCheckoutStatus()).thenReturn(ExpressCheckoutDetails.CheckoutStatus.PAYMENT_ACTION_NOT_INITIATED);
        given(paypalRequester.getExpressCheckoutDetails(PAY_PAL_TOKEN)).willReturn(expressCheckoutDetails);
        given(paypalRequester.doExpressCheckoutPayment(anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .willThrow(new PaypalRequestException());

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        assertThat(mav.getViewName(), is("payment/paypal-ec/error"));
        verify(paypalRequester).doExpressCheckoutPayment(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    public void returnFromPaypalShouldReturnErrorViewWhenThePaymentHasAlreadyBeenCompleted()
            throws WalletServiceException, IOException, PaypalRequestException, PaymentStateException {
        initSuccessfulTxnWithDefaultChips();
        when(expressCheckoutDetails.getCheckoutStatus()).thenReturn(ExpressCheckoutDetails.CheckoutStatus.PAYMENT_COMPLETED);

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        assertThat(mav.getViewName(), is("payment/paypal-ec/error"));
        verifyZeroInteractions(walletService);
    }

    @Test
    public void shouldIncrementPlayBalanceWithDefaultChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithDefaultChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", FMT.parseDateTime(TIME_STAMP))
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldIncrementPlayBalanceWithPromotionChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithPromotionChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(), request);

        verify(walletService).record(ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", FMT.parseDateTime(TIME_STAMP))
                .withAmount(Currency.getInstance(paymentOptionWithPromotion.getRealMoneyCurrency()), paymentOptionWithPromotion.getAmountRealMoneyPerPurchase())
                .withPaymentOption(promotionPaymentOption.getPromotionChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .build());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldLogExternalTxnWithDefaultChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithDefaultChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", FMT.parseDateTime(TIME_STAMP))
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        verify(walletService).record(transaction);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldLogExternalTxnOnAPaymentException() throws PaypalRequestException, WalletServiceException, IOException {
        given(cookieHelper.getPaymentGameType(Matchers.<Cookie[]>any())).willReturn(GAME_TYPE);
        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
        final String amount = paymentOption.getAmountRealMoneyPerPurchase().toPlainString();
        final String currencyCode = paymentOption.getRealMoneyCurrency();
        prepareSuccessfulExpressCheckoutDetails(amount, currencyCode);
        when(paypalRequester.doExpressCheckoutPayment(anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("aTestException"));

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null, request);

        final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(null)
                .withMessage("x-x-x", new DateTime())
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        verify(walletService).record(transaction);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldLogExternalTxnOnAPaymentFailure() throws PaypalRequestException, WalletServiceException, IOException {
        initUnsuccessfulTxnWithDefaultChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), PROMO_ID, request);

        final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", new DateTime())
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .build();
        verify(walletService).record(transaction);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldLogExternalTxnWithPromotionChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithPromotionChips();

        underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(),
                request);

        final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(LOCAL_TRANSACTION_ID)
                .withExternalTransactionId(TXN_ID)
                .withMessage("x-x-x", FMT.parseDateTime(TIME_STAMP))
                .withAmount(Currency.getInstance(paymentOptionWithPromotion.getRealMoneyCurrency()), paymentOptionWithPromotion.getAmountRealMoneyPerPurchase())
                .withPaymentOption(promotionPaymentOption.getPromotionChipsPerPurchase(), null)
                .withCreditCardNumber("x-x-x")
                .withCashierName("PayPal")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withSessionId(SESSION_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.WEB)
                .build();
        verify(walletService).record(transaction);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    public void shouldNotAddPromotionPlayerRewardWhenTxnIsSuccessfulWithoutPromotionChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithDefaultChips();

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null,
                request);

        assertThat(mav.getViewName(), is("payment/paypal-ec/success"));
        verify(promotionService, never()).logPlayerReward(
                Matchers.<BigDecimal>any(),
                Matchers.<Long>any(),
                Matchers.any(PaymentPreferences.PaymentMethod.class),
                Matchers.<String>any(),
                Matchers.<DateTime>any());
    }

    @Test
    public void shouldAddPromotionPlayerRewardWhenTxnIsSuccessfulWithPromotionChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithPromotionChips();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100l);

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(),
                request);

        assertThat(mav.getViewName(), is("payment/paypal-ec/success"));
        verify(promotionService).logPlayerReward(PLAYER_ID, PROMO_ID, PAYPAL, paymentOptionWithPromotion.getId(),
                DATE_TIME);
    }

    @Test
    public void shouldAddDefaultChipsToModelWhenTxnIsSuccessfulWithDefaultChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithDefaultChips();

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOption.getId(), null,
                request);

        assertThat((BigDecimal) mav.getModel().get("numberOfChips"), is(paymentOption.getNumChipsPerPurchase()));
        assertThat(mav.getViewName(), is("payment/paypal-ec/success"));
    }

    @Test
    public void shouldAddPromotionChipsToModelWhenTxnIsSuccessfulWithPromotionChips() throws PaypalRequestException, WalletServiceException, IOException {
        initSuccessfulTxnWithPromotionChips();

        final ModelAndView mav = underTest.returnFromPaypal(PAY_PAL_TOKEN, paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(),
                request);

        assertThat((BigDecimal) mav.getModel().get("numberOfChips"), is(
                promotionPaymentOption.getPromotionChipsPerPurchase()));
        assertThat(mav.getViewName(), is("payment/paypal-ec/success"));
    }

    private void initSuccessfulTxnWithDefaultChips() throws PaypalRequestException {
        final String amount = paymentOption.getAmountRealMoneyPerPurchase().toPlainString();
        final String currencyCode = paymentOption.getRealMoneyCurrency();
        prepareSuccessfulExpressCheckoutDetails(amount, currencyCode);
        prepareSuccessfulExpressCheckoutPayment(amount, currencyCode, NUM_CHIPS_PER_PURCHASE);

        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
    }

    private void initUnsuccessfulTxnWithDefaultChips() throws PaypalRequestException {
        final String amount = paymentOption.getAmountRealMoneyPerPurchase().toPlainString();
        final String currencyCode = paymentOption.getRealMoneyCurrency();
        prepareSuccessfulExpressCheckoutDetails(amount, currencyCode);
        prepareUnsuccessfulExpressCheckoutPayment(amount, currencyCode, NUM_CHIPS_PER_PURCHASE);

        given(promotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).willReturn(paymentOption);
    }

    private void initSuccessfulTxnWithPromotionChips() throws PaypalRequestException {
        final String amount = paymentOptionWithPromotion.getAmountRealMoneyPerPurchase().toPlainString();
        final String currencyCode = paymentOptionWithPromotion.getRealMoneyCurrency();
        prepareSuccessfulExpressCheckoutDetails(amount, currencyCode);
        prepareSuccessfulExpressCheckoutPayment(amount, currencyCode, PROMOTION_CHIPS_PER_PURCHASE);

        given(promotionService.getDefaultPaymentOptionFor(paymentOptionWithPromotion.getId(), Platform.WEB)).willReturn(
                paymentOptionWithPromotion);
    }

    private void prepareSuccessfulExpressCheckoutPayment(final String amount,
                                                         final String currencyCode,
                                                         final BigDecimal chipsPerPurchase)
            throws PaypalRequestException {
        given(expressCheckoutPayment.getAmount()).willReturn(amount);
        given(expressCheckoutPayment.getCurrency()).willReturn(currencyCode);
        given(expressCheckoutPayment.getTimestamp()).willReturn(TIME_STAMP);
        given(expressCheckoutPayment.getTransactionId()).willReturn(TXN_ID);
        given(expressCheckoutPayment.isSuccessful()).willReturn(true);
        given(paypalRequester.doExpressCheckoutPayment(PAY_PAL_TOKEN, PAYER_ID, LOCAL_TRANSACTION_ID, amount,
                currencyCode, chipsPerPurchase)).willReturn(expressCheckoutPayment);
    }

    private void prepareUnsuccessfulExpressCheckoutPayment(final String amount,
                                                           final String currencyCode,
                                                           final BigDecimal chipsPerPurchase)
            throws PaypalRequestException {
        given(expressCheckoutPayment.getAmount()).willReturn(amount);
        given(expressCheckoutPayment.getCurrency()).willReturn(currencyCode);
        given(expressCheckoutPayment.getTimestamp()).willReturn(TIME_STAMP);
        given(expressCheckoutPayment.getTransactionId()).willReturn(TXN_ID);
        given(expressCheckoutPayment.isSuccessful()).willReturn(false);
        given(paypalRequester.doExpressCheckoutPayment(PAY_PAL_TOKEN, PAYER_ID, LOCAL_TRANSACTION_ID, amount,
                currencyCode, chipsPerPurchase)).willReturn(expressCheckoutPayment);
    }

    private void prepareSuccessfulExpressCheckoutDetails(final String amount, final String currencyCode) throws PaypalRequestException {
        given(expressCheckoutDetails.getToken()).willReturn(PAY_PAL_TOKEN);
        given(expressCheckoutDetails.getPayerId()).willReturn(PAYER_ID);
        given(expressCheckoutDetails.getAmount()).willReturn(amount);
        given(expressCheckoutDetails.getCurrency()).willReturn(currencyCode);
        given(expressCheckoutDetails.getInvoiceId()).willReturn(LOCAL_TRANSACTION_ID);
        given(expressCheckoutDetails.getCheckoutStatus()).willReturn(ExpressCheckoutDetails.CheckoutStatus.PAYMENT_ACTION_NOT_INITIATED);
        given(paypalRequester.getExpressCheckoutDetails(PAY_PAL_TOKEN)).willReturn(expressCheckoutDetails);
    }
}
