package com.yazino.web.payment.facebook;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.email.EmailService;
import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentDispute;
import com.yazino.platform.payment.PaymentService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.payment.creditcard.PurchaseOutcome;
import com.yazino.web.payment.creditcard.PurchaseResult;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.Currency;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.FACEBOOK;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.Facebook;
import static java.math.BigDecimal.*;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.STRICT;

public class FacebookPaymentServiceTest {

    private static final String DEFAULT_PAYMENT_OPTION = "1";
    private static final BigDecimal PLAYER_ID = ONE;
    private static final Long PROMO_ID = 1L;
    private static final PaymentPreferences.PaymentMethod PAYMENT_METHOD = FACEBOOK;
    private static final String PAYMENT_ID = "1234";
    private static final String REQUEST_ID = "5689";
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String PLAYER_NAME = "PLAYER_NAME";
    private static final String EMAIL_ADDRESS = "EMAIL";
    private static final String PAYMENT_OPTION_ID = "GBP1";
    private static final BigDecimal MONEY_PER_PURCHASE = BigDecimal.TEN;
    private static final BigDecimal CHIPS_PER_PURCHASE = ONE;
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(500);
    private static final BigDecimal AMOUNT_OF_CREDITS = BigDecimal.valueOf(50);
    private static final BigDecimal AMOUNT_OF_CHIPS = BigDecimal.valueOf(10000);
    private static final DateTime DISPUTE_DATE = new DateTime(2014, 1, 3, 12, 0, 0);
    private static final Long promoId = 123l;

    private FacebookPaymentService underTest;
    @Mock
    FacebookConfiguration facebookConfiguration;
    @Mock
    private BuyChipsPromotionService buyChipsPromotionService;
    @Mock
    private PlayerService playerService;
    @Mock
    private WalletService walletService;
    @Mock
    private EmailService emailService;
    @Mock
    private QuietPlayerEmailer emailer;
    @Mock
    private CommunityService communityService;
    @Mock
    private PurchaseTracking purchaseTracking;
    @Mock
    private PlayerProfileService profileService;
    @Mock
    private PlayerProfile playerProfile;
    @Mock
    private PaymentService paymentService;

    private String transactionRef;

    @Before
    public void setup() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(0);
        MockitoAnnotations.initMocks(this);
        underTest = new FacebookPaymentService(
                facebookConfiguration,
                buyChipsPromotionService,
                playerService,
                walletService,
                emailer,
                communityService,
                purchaseTracking,
                profileService,
                paymentService);

        transactionRef = "1234567890";
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);
    }

    @Test
    public void promoServiceShouldProvidePaymentOption() {
        PaymentOption paymentOption = paymentOptionWithCurrency();
        when(buyChipsPromotionService.getPaymentOptionFor(PLAYER_ID, PROMO_ID, PAYMENT_METHOD, DEFAULT_PAYMENT_OPTION))
                .thenReturn(paymentOption);

        PaymentOption result = underTest.resolvePaymentOption(PLAYER_ID, DEFAULT_PAYMENT_OPTION, PROMO_ID);

        assertThat(paymentOption, is(equalTo(result)));
    }

    @Test
    public void resolvePaymentOptionShouldDefaultIfThereIsNoPromo() {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId("test");
        when(buyChipsPromotionService.getPaymentOptionFor(PLAYER_ID, PROMO_ID, PAYMENT_METHOD, DEFAULT_PAYMENT_OPTION))
                .thenReturn(new PaymentOption());
        when(buyChipsPromotionService.getDefaultFacebookPaymentOptionFor(DEFAULT_PAYMENT_OPTION))
                .thenReturn(paymentOption);

        PaymentOption result = underTest.resolvePaymentOption(PLAYER_ID, DEFAULT_PAYMENT_OPTION, null);

        assertThat(result, is(equalTo(paymentOption)));
    }


    @Test
    public void walletServiceIsCalledWhenLoggingATransactionAttempt() throws WalletServiceException {
        PaymentOption paymentOption = paymentOptionWithCurrency();
        paymentOption.setNumChipsPerPurchase(BigDecimal.TEN);

        when(playerService.getAccountId(PLAYER_ID)).thenReturn(PLAYER_ID);
        ExternalTransaction externalTransaction = underTest.logTransactionAttempt(PLAYER_ID, REQUEST_ID, paymentOption, GAME_TYPE, promoId);

        verify(walletService).record(any(ExternalTransaction.class));
        assertThat(externalTransaction.getAccountId(), is(equalTo(PLAYER_ID)));
        assertThat(externalTransaction.getInternalTransactionId(), is(equalTo(REQUEST_ID)));
        assertNull(externalTransaction.getExternalTransactionId());
        assertThat(externalTransaction.getAmount().getCurrency(), is(equalTo(Currency.getInstance("GBP"))));
        assertThat(externalTransaction.getAmountChips(), is(equalTo(TEN)));
        assertThat(externalTransaction.getGameType(), is(equalTo(GAME_TYPE)));
    }

    @Test
    public void completePurchaseCallsCommunityServiceWithTheCorrectPlayerID() throws UnknownHostException {
        PaymentOption paymentOption = paymentOptionWithCurrency();
        paymentOption.setAmountRealMoneyPerPurchase(MONEY_PER_PURCHASE);
        paymentOption.addPromotionPaymentOption(new PromotionPaymentOption(FACEBOOK, PROMO_ID, CHIPS_PER_PURCHASE, "", ""));
        FacebookAppConfiguration appConfiguration = new FacebookAppConfiguration();
        appConfiguration.setAppName("highstakes");
        when(facebookConfiguration.getAppConfigFor(GAME_TYPE, CANVAS, STRICT)).thenReturn(appConfiguration);
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(PLAYER_ID);
        when(profileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getEmailAddress()).thenReturn("EMAIL");
        when(playerProfile.getDisplayName()).thenReturn(PLAYER_NAME);
        PurchaseResult purchaseResult = underTest.completePurchase(PLAYER_ID,
                GAME_TYPE,
                paymentOption,
                PAYMENT_ID,
                REQUEST_ID,
                "GBP",
                valueOf(10), promoId);

        verify(emailer).quietlySendEmail(expectedEmail());
        verify(communityService).asyncPublishBalance(PLAYER_ID);
        verify(buyChipsPromotionService).logPlayerReward(PLAYER_ID, PROMO_ID, PAYMENT_METHOD, PAYMENT_OPTION_ID, new DateTime(0));
        final PaymentPreferences expectedPlayerPreferences = new PaymentPreferences(com.yazino.platform.reference.Currency.GBP, FACEBOOK);
        verify(playerService).updatePaymentPreferences(PLAYER_ID, expectedPlayerPreferences);
        verify(purchaseTracking).trackSuccessfulPurchase(PLAYER_ID);

        assertThat(purchaseResult.getOutcome(), is(equalTo(PurchaseOutcome.APPROVED)));
        assertThat(purchaseResult.getCustomerEmail(), is(equalTo(EMAIL_ADDRESS)));
        assertThat(purchaseResult.getMerchant(), is(equalTo(FACEBOOK.name())));
        assertThat(purchaseResult.getExternalTransactionId(), is(equalTo(PAYMENT_ID)));
        assertThat(purchaseResult.getInternalTransactionId(), is(equalTo(REQUEST_ID)));
    }

    @Test
    public void disputePurchaseChangesCallPaymentServiceWithDisputedPayment() {
        final PaymentOption paymentOption = paymentOptionWithCurrency();
        paymentOption.setAmountRealMoneyPerPurchase(MONEY_PER_PURCHASE);
        paymentOption.addPromotionPaymentOption(new PromotionPaymentOption(FACEBOOK, PROMO_ID, CHIPS_PER_PURCHASE, "", ""));
        when(playerService.getAccountId(PLAYER_ID)).thenReturn(ACCOUNT_ID);

        underTest.disputePurchase(REQUEST_ID, PAYMENT_ID, PLAYER_ID, paymentOption, GAME_TYPE, PROMO_ID, "aDispute", DISPUTE_DATE);

        verify(paymentService).disputePayment(
                PaymentDispute.newDispute(REQUEST_ID,
                        FACEBOOK.name(),
                        PAYMENT_ID,
                        PLAYER_ID,
                        ACCOUNT_ID,
                        DISPUTE_DATE,
                        MONEY_PER_PURCHASE,
                        Currency.getInstance("GBP"),
                        CHIPS_PER_PURCHASE,
                        ExternalTransactionType.DEPOSIT,
                        "aDispute")
                        .withPaymentOptionId(paymentOption.getId())
                        .withGameType(GAME_TYPE)
                        .withPlatform(Platform.FACEBOOK_CANVAS)
                        .withPromotionId(PROMO_ID)
                        .build());
    }

    @Test
    public void getCurrencyShouldHandleDefaultToUSDForUnknownCurrencies() {

        Assert.assertThat(underTest.getCurrency("EUR"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.EUR)));
        Assert.assertThat(underTest.getCurrency("USD"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.USD)));
        Assert.assertThat(underTest.getCurrency("GBP"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.GBP)));
        Assert.assertThat(underTest.getCurrency("AUD"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.AUD)));
        Assert.assertThat(underTest.getCurrency("CAD"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.CAD)));
        Assert.assertThat(underTest.getCurrency("TVD"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.TVD)));
        //default
        Assert.assertThat(underTest.getCurrency("LKM"), CoreMatchers.is(IsEqual.equalTo(com.yazino.platform.reference.Currency.USD)));
    }

    @Test
    public void earnChipsShouldPublishBalance() throws WalletServiceException {
        underTest.earnChips(PLAYER_ID, AMOUNT_OF_CREDITS, AMOUNT_OF_CHIPS, transactionRef, "", promoId);
        verify(communityService).asyncPublishBalance(PLAYER_ID);
    }

    @Test
    public void earnChipsShouldTrackPurchase() throws WalletServiceException {
        underTest.earnChips(PLAYER_ID, AMOUNT_OF_CREDITS, AMOUNT_OF_CHIPS, transactionRef, "", promoId);
        verify(purchaseTracking).trackSuccessfulPurchase(PLAYER_ID);
    }

    @Test
    public void earnChipsShouldLogExternalTransaction() throws WalletServiceException {
        underTest.earnChips(PLAYER_ID, AMOUNT_OF_CREDITS, AMOUNT_OF_CHIPS, transactionRef, "GAME_TYPE", promoId);
        final ExternalTransaction transaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(transactionRef)
                .withExternalTransactionId(transactionRef)
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance("USD"), new BigDecimal(5))
                .withPaymentOption(AMOUNT_OF_CHIPS, null)
                .withCreditCardNumber("")
                .withCashierName("FacebookEarnChips")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("GAME_TYPE")
                .withPlayerId(PLAYER_ID)
                .withPromotionId(123l)
                .withPlatform(Platform.FACEBOOK_CANVAS)
                .build();
        verify(walletService).record(transaction);

    }

    @Test
    public void getPlayerIdShouldReturnPlayerIdForThatFacebookUser() {
        final PlayerProfile profile = mock(PlayerProfile.class);
        when(profile.getPlayerId()).thenReturn(new BigDecimal(410));
        when(profileService.findByProviderNameAndExternalId("facebook", "1234567890")).thenReturn(profile);
        assertThat(underTest.getPlayerId("1234567890"), is(equalTo(new BigDecimal("410"))));

    }

    @Test
    public void logCancelledTransaction() throws WalletServiceException {
        PaymentOption paymentOption = paymentOptionWithCurrency();
        ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(REQUEST_ID)
                .withExternalTransactionId(null)
                .withMessage("Cancelled by user", new DateTime())
                .withAmount(Currency.getInstance("GBP"), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), paymentOption.getId())
                .withCreditCardNumber("")
                .withCashierName("FACEBOOK")
                .withStatus(ExternalTransactionStatus.CANCELLED)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.FACEBOOK_CANVAS)
                .build();

        underTest.logFailedTransaction("1383010", "fb user cancelled message", PLAYER_ID, null, REQUEST_ID, paymentOption, GAME_TYPE, PROMO_ID);
        verify(walletService).record(externalTransaction);
    }

    @Test
    public void logFailedTransaction() throws WalletServiceException {
        PaymentOption paymentOption = paymentOptionWithCurrency();
        ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(REQUEST_ID)
                .withExternalTransactionId(PAYMENT_ID)
                .withMessage("1383003 - payment failure, processor declined", new DateTime())
                .withAmount(Currency.getInstance("GBP"), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(), paymentOption.getId())
                .withCreditCardNumber("")
                .withCashierName("FACEBOOK")
                .withStatus(ExternalTransactionStatus.FAILURE)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(GAME_TYPE)
                .withPlayerId(PLAYER_ID)
                .withPromotionId(PROMO_ID)
                .withPlatform(Platform.FACEBOOK_CANVAS)
                .build();

        underTest.logFailedTransaction("1383003", "payment failure, processor declined", PLAYER_ID, PAYMENT_ID, REQUEST_ID, paymentOption, GAME_TYPE, PROMO_ID);
        verify(walletService).record(externalTransaction);
    }

    private EmailBuilder expectedEmail() {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        builder.withEmailAddress(EMAIL_ADDRESS);
        builder.withFirstName(PLAYER_NAME);
        builder.withPurchasedChips(CHIPS_PER_PURCHASE);
        builder.withCurrency(java.util.Currency.getInstance("GBP"));
        builder.withCost(MONEY_PER_PURCHASE);
        builder.withPaymentDate(new DateTime(0).toDate());
        builder.withPaymentId(PAYMENT_ID);
        builder.withPaymentEmailBodyTemplate(Facebook);
        builder.withTargetUrl("https://apps.facebook.com/highstakes");
        builder.withIncludeGamesInFooter(false);
        return builder;
    }

    // check that the email is sent for Facebook payment completion

    private PaymentOption paymentOptionWithCurrency() {
        PaymentOption paymentOption = new PaymentOption();
        paymentOption.setId("GBP1");
        paymentOption.setRealMoneyCurrency("GBP");
        paymentOption.setAmountRealMoneyPerPurchase(BigDecimal.valueOf(100));
        return paymentOption;
    }


}
