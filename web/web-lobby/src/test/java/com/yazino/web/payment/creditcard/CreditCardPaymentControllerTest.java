package com.yazino.web.payment.creditcard;

import com.google.common.base.Optional;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.email.EmailException;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerInfo;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.community.ProfileInformation;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.payment.CardRegistrationResultBuilder;
import com.yazino.web.domain.payment.CardRegistrationTokenResult;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.creditcard.worldpay.WorldPayCreditCardRegistrationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;

import static com.yazino.payment.worldpay.MessageCode.CUSTOMER_INFORMATION_ADDED;
import static com.yazino.payment.worldpay.MessageCode.INVALID_CARD_EXPIRY_DATE;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static java.lang.String.valueOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class CreditCardPaymentControllerTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(34L);
    private static final String VIEW_PATH = "payment/creditcard/";
    public static final long PROMO_ID = 1l;
    public static final BigDecimal PROMOTION_CHIPS_PER_PURCHASE = new BigDecimal("34000");
    public static final String HEADER = "header";
    public static final String TEXT = "text";
    public static final BigDecimal NUM_CHIPS_PER_PURCHASE = new BigDecimal("21000");
    public static final String ONE_TIME_TOKEN = "ott";

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final PlayerService playerService = mock(PlayerService.class);
    private final LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private final CookieHelper cookieHelper = mock(CookieHelper.class);
    private final PurchaseOutcomeMapper purchaseOutcomeMapper = mock(PurchaseOutcomeMapper.class);
    private final BuyChipsPromotionService buyChipsPromotionService = mock(BuyChipsPromotionService.class);
    private final CreditCardService creditCardService = mock(CreditCardService.class);
    private final WorldPayCreditCardRegistrationService worldPayCardRegistrationService = mock(WorldPayCreditCardRegistrationService.class);
    private final YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);

    private CreditCardPaymentController underTest = new CreditCardPaymentController(
            lobbySessionCache,
            purchaseOutcomeMapper,
            cookieHelper,
            creditCardService,
            worldPayCardRegistrationService,
            yazinoConfiguration);

    private BigDecimal playerAcountId = new BigDecimal("2");
    private CreditCardForm validCreditCardForm = CreditCardFormBuilder.valueOf()
            .withPaymentOptionId("option2")
            .withPromotionId(null)
            .withCreditCardNumber("4200000000000000")
            .withCvc2("123")
            .withExpirationMonth("11")
            .withExpirationYear("2019")
            .withCardHolderName("Nick Jones")
            .withEmailAddress("somebody@somewhere.com")
            .withTermsAndServiceAgreement("true")
            .build();

    private LobbySession lobbySession = new LobbySession(
            BigDecimal.valueOf(3141592), PLAYER_ID,
            "Nick",
            "12345",
            Partner.YAZINO,
            "http://twogirlsonecup.png",
            "nick@whoknows.com",
            null, false,
            Platform.WEB,
            AuthProvider.YAZINO);
    private PaymentOption paymentOption;
    private PaymentOption paymentOptionWithPromotion;
    private PromotionPaymentOption promotionPaymentOption;

    @Before
    public void setup() {
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
        promotionPaymentOption = new PromotionPaymentOption(CREDITCARD, PROMO_ID, PROMOTION_CHIPS_PER_PURCHASE, HEADER, TEXT);
        paymentOptionWithPromotion.addPromotionPaymentOption(promotionPaymentOption);

        when(lobbySessionCache.getActiveSession(request)).thenReturn(lobbySession);
        when(cookieHelper.getPaymentGameType(null)).thenReturn("BLACKJACK");
        when(creditCardService.resolvePaymentOption(PLAYER_ID, paymentOption.getId(), null)).thenReturn(paymentOption);
        when(worldPayCardRegistrationService.prepareCardRegistration(any(BigDecimal.class), anyString())).thenReturn(Optional.<CardRegistrationTokenResult>absent());
    }

    @Test
    public void shouldAddPaymentOptionToModel() {
        ModelMap modelMap = new ModelMap();
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);

        underTest.viewForm(request, response, paymentOption.getId(), null, modelMap);
    }

    @Test
    public void shouldReturnABadRequestErrorWhenCreditCardServiceReturnsNull() {
        ModelMap modelMap = new ModelMap();
        reset(creditCardService);
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);

        final ModelAndView view = underTest.viewForm(request, response, paymentOption.getId(), null, modelMap);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(view, is(nullValue()));
    }

    @Test
    public void shouldAddPromotionShownToModel() {
        ModelMap modelMap = new ModelMap();
        when(creditCardService.resolvePaymentOption(PLAYER_ID, paymentOptionWithPromotion.getId(), PROMO_ID)).thenReturn
                (paymentOptionWithPromotion);

        underTest.viewForm(request, response, paymentOptionWithPromotion.getId(),
                promotionPaymentOption.getPromoId(), modelMap);

        assertThat((Boolean) modelMap.get("promotionShown"), is(true));
    }

    @Test
    public void shouldDefaultTheExpiryMonthAndYearToTheCurrentMonthAndYear() {
        final Calendar cal = Calendar.getInstance();
        final String expectedMonth = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        final String expectedYear = Integer.toString(cal.get(Calendar.YEAR));
        ModelMap modelMap = new ModelMap();

        underTest.viewForm(request, response, paymentOption.getId(), null, modelMap);

        final CreditCardForm creditCardForm = (CreditCardForm) modelMap.get("creditCardForm");
        assertThat(creditCardForm, is(not(nullValue())));
        assertThat(creditCardForm.getExpirationMonth(), is(equalTo(expectedMonth)));
        assertThat(creditCardForm.getExpirationYear(), is(equalTo(expectedYear)));
    }

    @Test
    public void testSuccessViewReturnedIfTransactionSuccessfulAndStatsPublished() {
        String transactionId = "1";
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(true, transactionId));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        ModelAndView modelAndView = underTest.completePayment(validCreditCardForm, request, response, new ModelMap());
        assertEquals(VIEW_PATH + "success", modelAndView.getViewName());
    }

    @Test
    public void thePlayerIsRedirectedToTheBlockedPageIfTheyHaveBeenBlocked() {
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aBlockedPurchaseResult());
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);

        ModelAndView modelAndView = underTest.completePayment(validCreditCardForm, request, response, new ModelMap());

        assertThat(modelAndView.getView(), is(instanceOf(RedirectView.class)));
        assertThat(((RedirectView) modelAndView.getView()).getUrl(), is(equalTo("/blocked?reason=payment")));
    }

    @Test
    public void onTxnSuccessDefaultChipsAddedToModel() {
        String transactionId = "1";
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(true, transactionId));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        ModelAndView modelAndView = underTest.completePayment(validCreditCardForm, request, response, new ModelMap());
        final BigDecimal transactionValue = (BigDecimal) modelAndView.getModel().get("transactionValue");
        assertThat(transactionValue, is(paymentOption.getNumChipsPerPurchase(CREDITCARD.name())));
    }

    @Test
    public void shouldNotAddPromotionPlayerRewardWhenTxnIsSuccessfulWithoutPromotionChips() throws EmailException {
        String transactionId = "txId";
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(true, transactionId));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);

        underTest.completePayment(validCreditCardForm, request, response, new ModelMap());

        verify(buyChipsPromotionService, never()).logPlayerReward(Matchers.<BigDecimal>any(),
                Matchers.<Long>any(),
                Matchers.<PaymentPreferences.PaymentMethod>any(),
                Matchers.<String>any(),
                Matchers.<DateTime>any());

        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testErrorViewReturnedInCaseOfError() {
        String transactionId = "1";
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(false, transactionId));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        ModelAndView modelAndView = underTest.completePayment(validCreditCardForm, request, response, new ModelMap());
        assertEquals(VIEW_PATH + "error", modelAndView.getViewName());
    }

    @Test
    public void testErrorViewReturnedWithMappedErrorMessage() {
        String transactionId = "1";
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(false, transactionId));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(playerAcountId);
        when(playerService.getProfileInformation(PLAYER_ID, "BLACKJACK")).thenReturn(profileInformation());
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        when(purchaseOutcomeMapper.getErrorMessage(PurchaseOutcome.DECLINED)).thenReturn("message for code 1");
        ModelAndView modelAndView = underTest.completePayment(validCreditCardForm, request, response, new ModelMap());
        assertEquals(VIEW_PATH + "error", modelAndView.getViewName());
        assertEquals("message for code 1", modelAndView.getModel().get("ResultMessage"));
    }

    @Test
    public void completePaymentAfterACardHasBeenRegistered() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOption.getId(), Platform.WEB)).thenReturn(paymentOption);
        final String oneTimeToken = ONE_TIME_TOKEN;
        final CardRegistrationResultBuilder builder = CardRegistrationResultBuilder.builder()
                .withAccountName("accountName")
                .withCardId("cardId")
                .withCustomerId(PLAYER_ID.toString())
                .withExpiryDate("092103")
                .withMessage("Message")
                .withMessageCode(valueOf(CUSTOMER_INFORMATION_ADDED.getCode()))
                .withObscuredCardNumber("XXXXXXXXXXXX0000");
        when(creditCardService.completePurchase(any(PaymentContext.class), any(CreditCardDetails.class), any(
                InetAddress.class))).thenReturn(aPurchaseResult(true, "1"));
        when(worldPayCardRegistrationService.retrieveCardRegistrationResult(oneTimeToken)).thenReturn(builder.build());

        ModelAndView actual = underTest.cardRegisteredPostBack(request, response, paymentOption.getId(), oneTimeToken, "emailAddress", null, new ModelMap());

        verify(worldPayCardRegistrationService, times(1)).retrieveCardRegistrationResult(oneTimeToken);
        assertThat(((Boolean) actual.getModel().get("inError")), is(equalTo(false)));
        assertThat(((BigDecimal) actual.getModel().get("transactionValue")), is(equalTo(BigDecimal.valueOf(21000))));
        assertThat(((String) actual.getModel().get("transactionId")), is(equalTo("1")));
        assertThat(actual.getViewName(), is(equalTo("payment/creditcard/success")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenPaymentWithRegisteredCardFailsReturnErrorsInTheResponse() {
        final CardRegistrationResultBuilder builder = CardRegistrationResultBuilder.builder()
                .withAccountName("accountName")
                .withCardId("cardId")
                .withCustomerId(PLAYER_ID.toString())
                .withExpiryDate("092103")
                .withMessage(INVALID_CARD_EXPIRY_DATE.getDescription())
                .withMessageCode(valueOf(INVALID_CARD_EXPIRY_DATE.getCode()))
                .withObscuredCardNumber("XXXXXXXXXXXX0000");
        when(worldPayCardRegistrationService.retrieveCardRegistrationResult(ONE_TIME_TOKEN)).thenReturn(builder.build());

        ModelAndView actual = underTest.cardRegisteredPostBack(request, response, paymentOption.getId(), ONE_TIME_TOKEN, "emailAddress", null, new ModelMap());

        assertThat((List<String>) actual.getModel().get("errorMessages"), hasItems(INVALID_CARD_EXPIRY_DATE.getDescription()));
    }

    @Test
    public void whenPaymentWithRegisteredCardShouldHandleIllegalArgumentException() throws IOException {
        when(worldPayCardRegistrationService.retrieveCardRegistrationResult(anyString())).thenThrow(new IllegalArgumentException("Invalid parameter"));

        HttpServletResponse response = mock(HttpServletResponse.class);
        underTest.cardRegisteredPostBack(request, response, paymentOption.getId(), ONE_TIME_TOKEN, "emailAddress", null, new ModelMap());

        verify(response, times(1)).sendError(anyInt());
    }

    private ProfileInformation profileInformation() {
        return new ProfileInformation(new PlayerInfo("Bob", "http://pic"), 0, 0, BigDecimal.TEN);
    }

    private PurchaseResult aPurchaseResult(final boolean success,
                                           final String transactionId) {
        String message = "success";
        PurchaseOutcome outcome = PurchaseOutcome.APPROVED;
        if (!success) {
            message = "error";
            outcome = PurchaseOutcome.DECLINED;
        }
        return new PurchaseResult("wirecard", outcome, "anEmail", message,
                Currency.getInstance("GBP"), BigDecimal.valueOf(10), paymentOption.getNumChipsPerPurchase(
                CREDITCARD.name()), "aCard",
                transactionId, transactionId, "trace"
        );
    }

    private PurchaseResult aBlockedPurchaseResult() {
        return new PurchaseResult("wirecard", PurchaseOutcome.PLAYER_BLOCKED, "anEmail", "blocked",
                Currency.getInstance("GBP"), BigDecimal.valueOf(10), paymentOption.getNumChipsPerPurchase(
                CREDITCARD.name()), "aCard", "1", "1", "trace"
        );
    }

}
