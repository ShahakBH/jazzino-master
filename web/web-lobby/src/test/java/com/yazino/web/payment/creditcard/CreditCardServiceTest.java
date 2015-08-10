package com.yazino.web.payment.creditcard;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.email.EmailException;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.platform.reference.Currency;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.payment.CustomerDataBuilder;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.PAYPAL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class CreditCardServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(234);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    private static final String PAYMENT_OPTION_ID = "paymentOptionId";
    private static final InetAddress IP_ADDRESS = null;
    private static final long PROMOTION_ID = 1L;
    private static final BigDecimal AMOUNT_CHIPS = BigDecimal.valueOf(123);
    private static final BigDecimal AMOUNT_MONEY = BigDecimal.valueOf(500);
    private static final String CURRENCY = "USD";

    @Mock
    private AsyncEmailService emailService;
    @Mock
    private CommunityService communityService;
    @Mock
    private PurchaseTracking purchaseTracking;
    @Mock
    private QuietPlayerEmailer emailer;
    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;
    @Mock
    private PlayerService playerService;
    @Mock
    private CreditCardPaymentService creditCardPaymentService;

    private CreditCardService underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        underTest = new CreditCardService(buyChipsPromotionService, playerService,
                creditCardPaymentService, emailService, communityService, purchaseTracking, emailer, "dis <from@your.mum>");
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldResolvePaymentOptionForNormalPayment() {
        final PaymentOption expected = new PaymentOption();
        expected.setId(PAYMENT_OPTION_ID);
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(expected);
        final PaymentOption result = underTest.resolvePaymentOption(PLAYER_ID, PAYMENT_OPTION_ID, null);
        assertEquals(expected, result);
        verify(buyChipsPromotionService).getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB);
        verifyNoMoreInteractions(buyChipsPromotionService);
    }

    @Test
    public void shouldResolvePaymentOptionForPromotion() {
        final PaymentOption expected = new PaymentOption();
        expected.setId("promoPaymentId");
        when(buyChipsPromotionService.getPaymentOptionFor(PLAYER_ID, PROMOTION_ID, PaymentPreferences.PaymentMethod.CREDITCARD, PAYMENT_OPTION_ID)).thenReturn(expected);
        final PaymentOption result = underTest.resolvePaymentOption(PLAYER_ID, PAYMENT_OPTION_ID, PROMOTION_ID);
        assertEquals(expected, result);
        verify(buyChipsPromotionService).getPaymentOptionFor(PLAYER_ID, PROMOTION_ID, PaymentPreferences.PaymentMethod.CREDITCARD, PAYMENT_OPTION_ID);
        verifyNoMoreInteractions(buyChipsPromotionService);
    }

    @Test
    public void shouldFallbackToDefaultPaymentOptionIfPromoNotAvailable() {
        final long promotionId = 1L;
        final PaymentOption expected = new PaymentOption();
        expected.setId(PAYMENT_OPTION_ID);
        when(buyChipsPromotionService.getPaymentOptionFor(PLAYER_ID, promotionId, PaymentPreferences.PaymentMethod.CREDITCARD, PAYMENT_OPTION_ID)).thenReturn(null);
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(expected);
        final PaymentOption result = underTest.resolvePaymentOption(PLAYER_ID, PAYMENT_OPTION_ID, promotionId);
        assertEquals(expected, result);
    }

    @Test
    public void shouldCompletePurchase() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(creditCardPaymentService).purchase(aPurchaseRequest());
    }

    @Test
    public void shouldPublishBalanceOnSuccessfulTransaction() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void shouldNotPublishBalanceOnFailTransaction() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(false));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verifyZeroInteractions(communityService);
    }

    @Test
    public void shouldTrackingPurchaseOnSuccessfulTransaction() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(purchaseTracking).trackSuccessfulPurchase(PLAYER_ID);
    }

    @Test
    public void shouldNotTrackingPurchaseOnFailTransaction() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(false));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verifyZeroInteractions(purchaseTracking);
    }

    @Test
    public void shouldLogRewardIfPaymentIsPromotional() {
        final Map<PaymentPreferences.PaymentMethod, PromotionPaymentOption> promotions = new HashMap<>();
        final PromotionPaymentOption promotion = new PromotionPaymentOption();
        promotions.put(PaymentPreferences.PaymentMethod.CREDITCARD, promotion);
        final PaymentOption promotionalPaymentOption = aPaymentOption();
        promotionalPaymentOption.setPromotions(promotions);
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(promotionalPaymentOption);
        when(creditCardPaymentService.purchase(aPurchaseRequestWith(promotionalPaymentOption))).thenReturn(result(true));

        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);

        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, promotion.getPromoId(), CREDITCARD,
                promotionalPaymentOption.getId(), new DateTime());
    }

    @Test
    public void shouldNotLogRewardIfPaymentIsNotPromotional() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(buyChipsPromotionService, never()).logPlayerReward(any(BigDecimal.class), anyLong(),
                any(PaymentPreferences.PaymentMethod.class), anyString(), any(DateTime.class));
    }

    @Test
    public void shouldNotUpdatePreferredPaymentMethodOnFailTransaction() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(false));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(playerService, never()).updatePaymentPreferences(any(BigDecimal.class), any(PaymentPreferences.class));
    }

    @Test
    public void shouldUpdatePreferredMethodOnSuccessfulTransactionForFirstTime() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));

        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);

        verify(playerService).updatePaymentPreferences(PLAYER_ID, new PaymentPreferences(Currency.USD, CREDITCARD));
    }

    @Test
    public void shouldUpdatePreferredMethodOnSuccessfulTransactionForExistingPreference() {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result(true));
        when(playerService.getPaymentPreferences(PLAYER_ID)).thenReturn(new PaymentPreferences(Currency.EUR, PAYPAL));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verify(playerService).updatePaymentPreferences(PLAYER_ID, new PaymentPreferences(Currency.USD, CREDITCARD));
    }

    @Test
    public void shouldSendEmailForSuccessfulTransaction() throws EmailException {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        final PurchaseResult result = result(true);
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result);
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);

        ArgumentCaptor<BoughtChipsEmailBuilder> captor = ArgumentCaptor.forClass(BoughtChipsEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());
        BoughtChipsEmailBuilder builder = captor.getValue();

        assertEquals(aPaymentContext().getEmailAddress(), builder.getEmailAddress());
        assertEquals(aPaymentContext().getPlayerName(), builder.getFirstName());
        assertEquals(AMOUNT_CHIPS, builder.getPurchasedChips());
        assertEquals(java.util.Currency.getInstance(CURRENCY), builder.getCurrency());
        assertEquals(result.getCardNumberObscured(), builder.getCardNumber());
        assertEquals(result.getExternalTransactionId(), builder.getPaymentId());
        assertEquals(PaymentEmailBodyTemplate.CreditCard, builder.getPaymentEmailBodyTemplate());
        assertEquals(PaymentEmailBodyTemplate.CreditCard, builder.getPaymentEmailBodyTemplate());
    }

    @Test
    public void shouldIgnoreErrorSendingEmailForSuccessfulTransaction() throws EmailException {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        final PurchaseResult result = result(true);
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result);

        doThrow(new RuntimeException("error")).when(emailer).quietlySendEmail(any(EmailBuilder.class));
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
    }

    @Test
    public void shouldSendEmailForFailTransaction() throws EmailException {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        final PurchaseResult result = result(false);
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result);
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verifyZeroInteractions(emailer);
        verify(emailService).send(eq(result.getCustomerEmail()), anyString(), anyString(), anyString(), anyMap());
        verify(emailService).send(eq("wirecard@yazino.com"), anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    public void shouldIgnoreErrorSendingEmailForFailTransaction() throws EmailException {
        when(buyChipsPromotionService.getDefaultPaymentOptionFor(PAYMENT_OPTION_ID, Platform.WEB)).thenReturn(aPaymentOption());
        final PurchaseResult result = result(false);
        when(creditCardPaymentService.purchase(aPurchaseRequest())).thenReturn(result);
        doThrow(new RuntimeException("error"))
                .when(emailService).send(anyString(), anyString(), anyString(), anyString(), anyMap());
        underTest.completePurchase(aPaymentContext(), aCardDetails(), IP_ADDRESS);
        verifyZeroInteractions(emailer);
    }

    private PurchaseResult result(final boolean successful) {
        return new PurchaseResult("a",
                successful ? PurchaseOutcome.APPROVED : PurchaseOutcome.DECLINED,
                aPaymentContext().getEmailAddress(),
                "resMessage",
                java.util.Currency.getInstance(CURRENCY),
                AMOUNT_MONEY,
                AMOUNT_CHIPS,
                aCardDetails().getCreditCardNumber(),
                "internalId",
                "externalId",
                "trace"
        );
    }

    private PurchaseRequest aPurchaseRequest() {
        return aPurchaseRequestWith(aPaymentOption());
    }

    private PurchaseRequest aPurchaseRequestWith(final PaymentOption paymentOption) {
        final CreditCardDetails card = aCardDetails();
        final PaymentContext context = aPaymentContext();
        return new PurchaseRequest(
                new CustomerDataBuilder().withAmount(AMOUNT_MONEY)
                        .withCurrency(java.util.Currency.getInstance(CURRENCY))
                        .withTransactionCountry("GB")
                        .withCreditCardNumber(card.getCreditCardNumber())
                        .withCvc2(card.getCvc2())
                        .withExpirationMonth(card.getExpirationMonth())
                        .withExpirationYear(card.getExpirationYear())
                        .withCardHolderName(card.getCardHolderName())
                        .withCustomerIPAddress(IP_ADDRESS)
                        .withEmailAddress(context.getEmailAddress())
                        .withGameType(context.getGameType())
                        .build(),
                ACCOUNT_ID, paymentOption, new DateTime(), context.getPlayerId(), context.getSessionId(), context.getPromotionId());
    }

    private CreditCardDetails aCardDetails() {
        return new CreditCardDetails("ccNumber",
                "100",
                "05",
                "2015",
                "name on the card",
                null,
                "ccNumber");
    }

    private PaymentContext aPaymentContext() {
        return new PaymentContext(PLAYER_ID,
                SESSION_ID,
                "player " + PLAYER_ID,
                "GAME_TYPE",
                "EMAIL_ADDRESS",
                PAYMENT_OPTION_ID,
                null,
                Partner.YAZINO);
    }

    private PaymentOption aPaymentOption() {
        final PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId(PAYMENT_OPTION_ID);
        paymentOption.setNumChipsPerPurchase(AMOUNT_CHIPS);
        paymentOption.setRealMoneyCurrency(CURRENCY);
        paymentOption.setAmountRealMoneyPerPurchase(AMOUNT_MONEY);
        return paymentOption;
    }
}
