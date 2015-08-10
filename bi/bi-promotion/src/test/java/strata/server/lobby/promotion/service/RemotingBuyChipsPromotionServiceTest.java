package strata.server.lobby.promotion.service;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.reference.Currency;
import com.yazino.promotion.PromoRewardEvent;
import com.yazino.promotions.MysqlPromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import strata.server.lobby.api.promotion.BuyChipsPromotion;
import strata.server.lobby.api.promotion.InGameMessage;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.helper.BuyChipsPromotionBuilder;
import strata.server.lobby.promotion.matchers.PromotionPlayerRewardIsEqual;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PaymentOptionBuilder;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.*;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.promotion.BuyChipsPromotion.*;
import static strata.server.lobby.api.promotion.PromotionType.BUY_CHIPS;

@RunWith(MockitoJUnitRunner.class)
public class RemotingBuyChipsPromotionServiceTest {
    // NB BuyChipsPromotionBuilder initialises the seed to 37 and CG% to 10 hence the following IDs are as indicated
    private static final BigDecimal PLAYER_ID_NOT_IN_CONTROL_GROUP = valueOf(99L);
    private static final BigDecimal PLAYER_ID_IN_CONTROL_GROUP = valueOf(65L);
    private static final Long PROMO_ID = 1l;
    private static final PlayerProfile PLAYER_PROFILE = new PlayerProfile();

    @Mock
    private MysqlPromotionDao promotionDao;
    @Mock
    private PromotionControlGroupService promotionPlayerService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private QueuePublishingService<PromoRewardEvent> promoRewardEventService;
    @Mock
    private JDBCPaymentOptionDAO paymentOptionDAO;

    private RemotingBuyChipsPromotionService underTest;

    @Before
    public void init() {
        preparePaymentOptionDAOMock();

        given(playerProfileService.findByPlayerId(PLAYER_ID_IN_CONTROL_GROUP)).willReturn(PLAYER_PROFILE);
        given(playerProfileService.findByPlayerId(PLAYER_ID_NOT_IN_CONTROL_GROUP)).willReturn(PLAYER_PROFILE);

        underTest = new RemotingBuyChipsPromotionService(promotionDao,
                paymentOptionDAO,
                promotionPlayerService,
                playerProfileService,
                promoRewardEventService);
    }

    private void preparePaymentOptionDAOMock() {
        final Map<Platform, Map<Currency, List<PaymentOption>>> paymentOptionsByPlatform = new HashMap<>();
        paymentOptionsByPlatform.put(Platform.WEB, defaultWebChipPackages());
        paymentOptionsByPlatform.put(Platform.IOS, defaultIOSChipPackages());
        paymentOptionsByPlatform.put(Platform.FACEBOOK_CANVAS, defaultFacebookChipPackages());
        paymentOptionsByPlatform.put(Platform.ANDROID, defaultAndroidChipPackages());

        when(paymentOptionDAO.findByPlatform(any(Platform.class))).thenAnswer(new Answer<Object>() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Map<Currency, List<PaymentOption>> optionsByCurrency
                        = paymentOptionsByPlatform.get(invocation.getArguments()[0]);
                final Set<PaymentOption> options = new HashSet<>();
                for (List<PaymentOption> paymentOptions : optionsByCurrency.values()) {
                    options.addAll(paymentOptions);
                }
                return options;
            }
        });
        when(paymentOptionDAO.findByPlatformWithCurrencyKey(any(Platform.class))).thenAnswer(new Answer<Object>() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return paymentOptionsByPlatform.get(invocation.getArguments()[0]);
            }
        });
        when(paymentOptionDAO.findByIdAndPlatform(anyString(), any(Platform.class))).thenAnswer(new Answer<Object>() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Map<Currency, List<PaymentOption>> optionsByCurrency = paymentOptionsByPlatform.get(invocation.getArguments()[1]);
                if (optionsByCurrency != null) {
                    for (List<PaymentOption> paymentOptions : optionsByCurrency.values()) {
                        for (PaymentOption paymentOption : paymentOptions) {
                            if (paymentOption.getId().equals(invocation.getArguments()[0])) {
                                return Optional.fromNullable(paymentOption);
                            }
                        }
                    }
                }
                return Optional.absent();
            }
        });
    }

    @After
    public void resetDateTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void promotionDaoCannotBeNull() {
        new RemotingBuyChipsPromotionService(null, paymentOptionDAO, promotionPlayerService, playerProfileService, promoRewardEventService);
    }

    @Test(expected = NullPointerException.class)
    public void defaultPromotionPlayerServiceCannotBeNull() {
        underTest = new RemotingBuyChipsPromotionService(promotionDao, paymentOptionDAO, null, playerProfileService, promoRewardEventService);
    }

    @Test(expected = NullPointerException.class)
    public void defaultPlayerProfileServiceCannotBeNull() {
        underTest = new RemotingBuyChipsPromotionService(promotionDao, paymentOptionDAO, promotionPlayerService, null, promoRewardEventService);
    }

    @Test(expected = NullPointerException.class)
    public void defaultPaymentOptionFactoryCannotBeNull() {
        new RemotingBuyChipsPromotionService(promotionDao, null, promotionPlayerService, playerProfileService, promoRewardEventService);
    }

    @Test(expected = NullPointerException.class)
    public void defaultPromoRewardServiceCannotBeNull() {
        new RemotingBuyChipsPromotionService(promotionDao, paymentOptionDAO, promotionPlayerService, playerProfileService, null);
    }

    @Test(expected = NullPointerException.class)
    public void getBuyChipPackagesShouldThrowExceptionIfPlayerIsNull() {
        underTest.getBuyChipsPaymentOptionsFor(null, Platform.WEB);
    }


    @Test(expected = NullPointerException.class)
    public void getBuyChipPackagesShouldThrowExceptionIfPlatformIsNull() {
        underTest.getBuyChipsPaymentOptionsFor(BigDecimal.ZERO, null);
    }

    @Test
    public void hasPromotionShouldReturnFalseIfGetBuyChipsPromotionsReturnsNull() {
// GIVEN that the player is not in any promotion
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(null);

        // WHEN requesting chip packages for player not in any promotion
        assertThat(underTest.hasPromotion(PLAYER_ID_NOT_IN_CONTROL_GROUP, Platform.WEB), is(false));

    }

    @Test
    public void hasPromotionShouldReturnFalseIfGetBuyChipsPromotionsReturnsEmptyList() {
// GIVEN that the player is not in any promotion
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(Maps.<PaymentPreferences.PaymentMethod, Promotion>newHashMap());

        // WHEN requesting chip packages for player not in any promotion
        assertThat(underTest.hasPromotion(PLAYER_ID_NOT_IN_CONTROL_GROUP, Platform.WEB), is(false));

    }

    @Test
    public void buyChipsShouldReturnTrueIfAnyPromotionsExist() {
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID).withChips("10000",
                "12000", Platform.WEB).withChips("21000", "24000", Platform.WEB).getPromotion();
        Map<PaymentPreferences.PaymentMethod, Promotion> expectedPromotions = new HashMap<>();
        expectedPromotions.put(CREDITCARD, promotion);
        expectedPromotions.put(PAYPAL, promotion);
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(expectedPromotions);

        assertThat(underTest.hasPromotion(PLAYER_ID_IN_CONTROL_GROUP, Platform.WEB), is(true));

    }

    @Test
    public void buyChipsShouldNotTryLogPromotionIfOneDoesNotExist() {

        DateTime awardDate = new DateTime();
        underTest.logPlayerReward(PLAYER_ID_IN_CONTROL_GROUP, PROMO_ID, GOOGLE_CHECKOUT, "blah blah", awardDate);
        when(promotionDao.findById(PROMO_ID)).thenReturn(null);

        verify(promotionDao, times(0)).addLastReward(Matchers.any(PromotionPlayerReward.class));

    }

    @Test
    public void shouldReturnDefaultPaymentOptionsWhenPlayerNotInPromotion() {
        // GIVEN that the player is not in any promotion
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(null);

        // WHEN requesting chip packages for player not in any promotion
        final Map<Currency, List<PaymentOption>> paymentOptionMap =
                underTest.getBuyChipsPaymentOptionsFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, Platform.WEB);


        for (Currency currency : paymentOptionMap.keySet()) {
            assertThat(paymentOptionMap.get(currency),
                    is(collectionEqualTo(paymentOptionDAO.findByPlatformWithCurrencyKey(Platform.WEB).get(currency))));
        }

        verify(promotionDao).getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any());
    }

    @Test
    public void shouldReturnPaymentOptionsWithPromotionDetailsWhenPlayerInPromotion() {
        // GIVEN that the player is in a creditcard and paypal promotion - note default package values here match package values in PaymentOptionFactory
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID).withChips("10000",
                "12000", Platform.WEB).withChips("21000", "24000", Platform.WEB).getPromotion();
        Map<PaymentPreferences.PaymentMethod, Promotion> expectedPromotions = new HashMap<>();
        expectedPromotions.put(CREDITCARD, promotion);
        expectedPromotions.put(PAYPAL, promotion);
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(expectedPromotions);

        // WHEN requesting chip packages for player in any promotion
        final Map<Currency, List<PaymentOption>> actualPaymentOptions = underTest.getBuyChipsPaymentOptionsFor(
                PLAYER_ID_NOT_IN_CONTROL_GROUP,
                Platform.WEB);

        // THEN defaults should be returned with additional promotion details
        final Map<Currency, List<PaymentOption>> expectedPaymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);


        for (Currency acceptedCurrency : Currency.values()) {
            assertThat(actualPaymentOptions.get(acceptedCurrency), is(collectionEqualTo(expectedPaymentOptions.get(acceptedCurrency))));
        }
    }

    @Test
    public void shouldReturnBuyChipPackagesWithPromotionDetailsWhenPlayerInPromotion() {
        // GIVEN that the player is in a creditcard and paypal promotion - note default package values here match package values in PaymentOptionFactory
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID).withChips("10000",
                "12000", Platform.WEB).withChips("21000", "24000", Platform.WEB).getPromotion();
        Map<PaymentPreferences.PaymentMethod, Promotion> expectedPromotions = new HashMap<>();
        expectedPromotions.put(CREDITCARD, promotion);
        expectedPromotions.put(PAYPAL, promotion);
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(expectedPromotions);

        // WHEN requesting chip packages for player in any promotion
        final Map<Currency, List<PaymentOption>> actualPaymentOptions = underTest.getBuyChipsPaymentOptionsFor(
                PLAYER_ID_NOT_IN_CONTROL_GROUP,
                Platform.WEB);

        // THEN defaults should be returned with additional promotion details
        final Map<Currency, List<PaymentOption>> expectedPaymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);


        for (Currency acceptedCurrency : Currency.values()) {
            assertThat(actualPaymentOptions.get(acceptedCurrency), is(collectionEqualTo(expectedPaymentOptions.get(acceptedCurrency))));
        }
    }

    @Test
    public void shouldReturnPaymentOptionsWithSpecificPackagePromotionDetailsWhenPlayerInPromotion() {
        // GIVEN that the player is in a creditcard promotion and that only some packages are overridden (10000 is not)
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID).withPaymentMethod(CREDITCARD)
                .withChips("21000", "24000", Platform.WEB)
                .getPromotion();

        Map<PaymentPreferences.PaymentMethod, Promotion> expectedPromotions = new HashMap<>();
        expectedPromotions.put(CREDITCARD, promotion);
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(expectedPromotions);

        // WHEN requesting chip packages for player in any promotion
        final Map<Currency, List<PaymentOption>> actualPaymentOptions = underTest.getBuyChipsPaymentOptionsFor(
                PLAYER_ID_NOT_IN_CONTROL_GROUP,
                Platform.WEB);

        // THEN defaults should be returned with additional promotion details
        final Map<Currency, List<PaymentOption>> expectedPaymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);


        for (Currency acceptedCurrency : Currency.values()) {
            assertThat(actualPaymentOptions.get(acceptedCurrency), is(collectionEqualTo(expectedPaymentOptions.get(acceptedCurrency))));
        }
    }

    @Test
    public void whenPlayerIsInControlGroupShouldReturnPaymentOptionsWithDefaultValuesInPromotion() {
        // GIVEN that the player is in a creditcard promotion and that the player is control group member
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID).withPaymentMethod(CREDITCARD)
                .withChips("10000", "12000", Platform.WEB).withChips("21000", "24000", Platform.WEB).getPromotion();
        Map<PaymentPreferences.PaymentMethod, Promotion> expectedPromotions = new HashMap<>();
        expectedPromotions.put(CREDITCARD, promotion);
        given(promotionDao.getBuyChipsPromotions(Matchers.<java.math.BigDecimal>any(),
                Matchers.<Platform>any(),
                Matchers.<DateTime>any())).willReturn(expectedPromotions);
        given(promotionPlayerService.isControlGroupMember(PLAYER_PROFILE, promotion)).willReturn(true);

        // WHEN requesting chip packages for player in any promotion
        final Map<Currency, List<PaymentOption>> actualPaymentOptions = underTest.getBuyChipsPaymentOptionsFor(PLAYER_ID_IN_CONTROL_GROUP,
                Platform.WEB);

        // THEN defaults should be returned with additional promotion details
        final Map<Currency, List<PaymentOption>> expectedPaymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);
        for (List<PaymentOption> paymentOptions : expectedPaymentOptions.values()) {
            for (PaymentOption paymentOption : paymentOptions) {
                final PromotionPaymentOption ccPromotion = paymentOption.getPromotion(CREDITCARD.name());
                final PromotionPaymentOption ccPromotionWithDefaultChips =
                        new PromotionPaymentOption(CREDITCARD, ccPromotion.getPromoId(),
                                paymentOption.getNumChipsPerPurchase(), "", "");
                paymentOption.addPromotionPaymentOption(ccPromotionWithDefaultChips);
            }
        }

        for (Currency acceptedCurrency : Currency.values()) {
            assertThat(actualPaymentOptions.get(acceptedCurrency), is(collectionEqualTo(expectedPaymentOptions.get(acceptedCurrency))));
        }
    }

    @Test
    public void shouldReturnPaymentOptionWIthPromotionChipsForActivePlayer() {
        // GIVEN that this promotion is returned
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(CREDITCARD).withChips("21000", "24000", Platform.WEB).getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);

        // WHEN requesting payment option for promo and option id
        final PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, CREDITCARD, "option2");

        // THEN PaymentOption with promotion details is returned
        final Map<Currency, List<PaymentOption>> paymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);
        PaymentOption expectedOption = paymentOptions.get(Currency.USD).get(1);
        assertThat(paymentOption, is(expectedOption));
    }

    @Test
    public void shouldReturnPaymentOptionWIthPromotionChipsForIOSActivePlayer() {
        // GIVEN that this promotion is returned
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(ITUNES).withChips("21000", "24000", Platform.IOS).getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);

        // WHEN requesting payment option for promo and option id
        final PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, ITUNES, "USD10");

        // THEN PaymentOption with promotion details is returned
        final Map<Currency, List<PaymentOption>> paymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.IOS);
        assertThat(paymentOptions.get(Currency.USD), hasItem(paymentOption));
    }

    @Test
    public void shouldReturnPaymentOptionWIthPromotionChipsForAndroidActivePlayer() {
        // GIVEN that this promotion is returned
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(GOOGLE_CHECKOUT).withChips("21000", "24000", Platform.ANDROID).getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);

        // WHEN requesting payment option for promo and option id
        final PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_NOT_IN_CONTROL_GROUP,
                PROMO_ID,
                GOOGLE_CHECKOUT,
                "USD10");

        // THEN PaymentOption with promotion details is returned
        final Map<Currency, List<PaymentOption>> paymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.ANDROID);
        assertThat(paymentOptions.get(Currency.USD), hasItem(paymentOption));
    }

    @Test
    public void shouldReturnPaymentOptionWithPromotionChipsOverriddenForControlGroupPlayer() {
        // GIVEN that this promotion is returned
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(CREDITCARD).withChips("21000", "24000", Platform.WEB).getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);
        given(promotionPlayerService.isControlGroupMember(PLAYER_PROFILE, promotion)).willReturn(true);

        // WHEN requesting payment option for promo and option id
        final PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_IN_CONTROL_GROUP, PROMO_ID, CREDITCARD, "option2");

        // THEN PaymentOption with promotion details is returned
        final Map<Currency, List<PaymentOption>> paymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.WEB);
        PaymentOption expectedOption = paymentOptions.get(Currency.USD).get(1);
        final PromotionPaymentOption promotionPaymentOptionWithDefaultChips =
                new PromotionPaymentOption(CREDITCARD, PROMO_ID, expectedOption.getNumChipsPerPurchase(), "", "");
        expectedOption.addPromotionPaymentOption(promotionPaymentOptionWithDefaultChips);
        assertThat(paymentOption, is(expectedOption));
    }

    @Test
    public void shouldReturnPaymentOptionWithPromotionChipsOverriddenForIOSControlGroupPlayer() {
        // GIVEN that this promotion is returned
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(ITUNES).withChips("21000", "24000", Platform.IOS).getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);
        given(promotionPlayerService.isControlGroupMember(PLAYER_PROFILE, promotion)).willReturn(true);

        // WHEN requesting payment option for promo and option id
        final PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_IN_CONTROL_GROUP, PROMO_ID, ITUNES, "USD10");

        // THEN PaymentOption with promotion details is returned
        final Map<Currency, List<PaymentOption>> paymentOptions = createPaymentOptionsFromDefaultsAndPromotion(promotion, Platform.IOS);
        PaymentOption expectedOption = paymentOptions.get(Currency.USD).get(1);
        assertThat(expectedOption.getId(), is(equalTo("USD10")));
        final PromotionPaymentOption promotionPaymentOptionWithDefaultChips
                = new PromotionPaymentOption(ITUNES, PROMO_ID, expectedOption.getNumChipsPerPurchase(), "", "");
        expectedOption.addPromotionPaymentOption(promotionPaymentOptionWithDefaultChips);
        assertThat(paymentOption, is(equalTo(expectedOption)));
    }

    @Test
    public void shouldDelegateToPromotionDaoWhenLoggingPlayerReward() {
        DateTime now = new DateTime();
        Promotion promotion = new BuyChipsPromotionBuilder()
                .withChips("10000", "1", Platform.WEB)
                .getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);

        underTest.logPlayerReward(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID,
                PAYPAL, "option1", now);

        final String expectedDetails = "method=PAYPAL, defaultChips=10000, promoChips=1";
        verify(promotionDao).addLastReward(argThat(PromotionPlayerRewardIsEqual.equalTo(
                new PromotionPlayerReward(PROMO_ID, PLAYER_ID_NOT_IN_CONTROL_GROUP, false, now, expectedDetails))));
        verify(promoRewardEventService).send(new PromoRewardEvent(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, now));
    }

    @Test
    public void shouldLogPlayerRewardWithGivenChipValuesWhenPlayerNotInControlGroup() {
        DateTime now = new DateTime();
        final BigDecimal defaultChips = BigDecimal.valueOf(10000);
        final BigDecimal promoChips = BigDecimal.valueOf(20000);
        Promotion promotion = new BuyChipsPromotionBuilder()
                .withChips(defaultChips.toPlainString(), promoChips.toPlainString(), Platform.ANDROID)
                .getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);

        underTest.logPlayerReward(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, defaultChips, promoChips, GOOGLE_CHECKOUT, now);

        final String expectedDetails = String.format("method=GOOGLE_CHECKOUT, defaultChips=%s, promoChips=%s",
                defaultChips.toPlainString(), promoChips.toPlainString());

        verify(promotionDao).addLastReward(argThat(PromotionPlayerRewardIsEqual.equalTo(
                new PromotionPlayerReward(PROMO_ID, PLAYER_ID_NOT_IN_CONTROL_GROUP, false, now, expectedDetails))));
        verify(promoRewardEventService).send(new PromoRewardEvent(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, now));

    }

    @Test
    public void shouldLogPlayerRewardWithGivenChipValuesWhenPlayerInControlGroup() {
        DateTime now = new DateTime();
        final BigDecimal defaultChips = BigDecimal.valueOf(10000);
        final BigDecimal promoChips = BigDecimal.valueOf(20000);
        Promotion promotion = new BuyChipsPromotionBuilder()
                .withChips(defaultChips.toPlainString(), promoChips.toPlainString(), Platform.ANDROID)
                .getPromotion();
        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);
        given(promotionPlayerService.isControlGroupMember(PLAYER_PROFILE, promotion)).willReturn(true);

        underTest.logPlayerReward(PLAYER_ID_IN_CONTROL_GROUP, PROMO_ID, defaultChips, promoChips, GOOGLE_CHECKOUT, now);

        final String expectedDetails = String.format("method=GOOGLE_CHECKOUT, defaultChips=%s, promoChips=%s",
                defaultChips.toPlainString(), promoChips.toPlainString());

        verify(promotionDao).addLastReward(argThat(PromotionPlayerRewardIsEqual.equalTo(
                new PromotionPlayerReward(PROMO_ID, PLAYER_ID_IN_CONTROL_GROUP, true, now, expectedDetails))));
        verify(promoRewardEventService).send(new PromoRewardEvent(PLAYER_ID_IN_CONTROL_GROUP, PROMO_ID, now));

    }

    @Test
    public void shouldNotLogPlayerRewardWhenPromotionNoLongerExists() {
        DateTime now = new DateTime();
        given(promotionDao.findById(PROMO_ID)).willReturn(null);

        underTest.logPlayerReward(PLAYER_ID_NOT_IN_CONTROL_GROUP, PROMO_ID, BigDecimal.ONE, BigDecimal.TEN, PAYPAL, now);

        verify(promotionDao, never()).addLastReward(any(PromotionPlayerReward.class));
        verifyNoMoreInteractions(promoRewardEventService);

    }

    @Test
    public void inGameMessageShouldBeNullWhenPlayerNotInPromotion() {
        // GIVEN that a player is not in any current promotion
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);
        given(promotionDao.findPromotionsFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, BUY_CHIPS, Platform.WEB,
                new DateTime())).willReturn(new ArrayList<Promotion>());

        // WHEN requesting in game message for player
        final InGameMessage inGameMessage = underTest.getInGameMessageFor(PLAYER_ID_NOT_IN_CONTROL_GROUP);

        // THEN message should be null
        assertThat(inGameMessage, nullValue());
    }

    @Test
    public void shouldReturnInGameMessageOfHighestPriorityPromotion() {
        // GIVEN that a player is multiple promotions
        final Promotion promoPriorityNull = new BuyChipsPromotionBuilder().withPriority(null).withInGameHeader
                ("header").withInGameMessage("msg")
                .getPromotion();
        final Promotion promoPriority1 = new BuyChipsPromotionBuilder().withPriority(1).withInGameHeader("header")
                .withInGameMessage("msg")
                .getPromotion();
        final InGameMessage expected = new InGameMessage("high priority header", "high priority message");
        final BuyChipsPromotion promoPriorityHighest = new BuyChipsPromotionBuilder().withPriority(10)
                .withInGameHeader(expected.getHeader())
                .withInGameMessage(expected.getMessage())
                .getPromotion();
        final List<Promotion> promotions = Arrays.asList(promoPriority1, promoPriorityNull, promoPriorityHighest);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);
        given(promotionDao.findPromotionsFor(PLAYER_ID_NOT_IN_CONTROL_GROUP,
                BUY_CHIPS,
                Platform.WEB,
                new DateTime())).willReturn(promotions);

        // WHEN requesting the in game message for player
        final InGameMessage actual = underTest.getInGameMessageFor(PLAYER_ID_NOT_IN_CONTROL_GROUP);

        // THEN message should be that of the highest priority promotion
        assertThat(actual, is(InGameMessageIsEqual.equalTo(expected)));
    }

    @Test
    public void shouldReturnInGameMessageForPlatform() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);

        underTest.getInGameMessageFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, Platform.FACEBOOK_CANVAS);
        verify(promotionDao).findPromotionsFor(PLAYER_ID_NOT_IN_CONTROL_GROUP, BUY_CHIPS, Platform.FACEBOOK_CANVAS, new DateTime());
    }

    @Test
    public void inGameMessageShouldBeNullForPlayerInControlGroup() {
        // GIVEN that a player is not in any current promotion
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(CREDITCARD).withChips("21000", "24000", Platform.WEB).getPromotion();
        given(promotionDao.findPromotionsFor(PLAYER_ID_IN_CONTROL_GROUP, BUY_CHIPS, Platform.WEB,
                new DateTime())).willReturn(Arrays.asList(promotion));
        PlayerProfile playerProfile = new PlayerProfile();
        given(playerProfileService.findByPlayerId(PLAYER_ID_IN_CONTROL_GROUP)).willReturn(playerProfile);
        given(promotionPlayerService.isControlGroupMember(playerProfile, promotion)).willReturn(true);

        // WHEN requesting in game message for player in control group
        final InGameMessage inGameMessage = underTest.getInGameMessageFor(PLAYER_ID_IN_CONTROL_GROUP);

        // THEN message should be null
        assertThat(inGameMessage, nullValue());
        verify(promotionDao).findPromotionsFor(PLAYER_ID_IN_CONTROL_GROUP, BUY_CHIPS, Platform.WEB, new DateTime());
    }

    @Test
    public void returnsDefaultPaymentOptionForFacebook() {
        PaymentOption paymentOption = underTest.getDefaultFacebookPaymentOptionFor("option1");

        assertThat(paymentOption, is(toOption("option1", "$", 10000, "5", "USD")));
    }

    @Test
    public void shouldReturnFacebookPaymentOptions() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100);
        Promotion promotion = new BuyChipsPromotionBuilder().withId(PROMO_ID)
                .withPaymentMethod(FACEBOOK)
                .withChips("21000", "24000", Platform.FACEBOOK_CANVAS)
                .getPromotion();

        given(promotionDao.findById(PROMO_ID)).willReturn(promotion);
        PlayerProfile playerProfile = new PlayerProfile();
        given(playerProfileService.findByPlayerId(PLAYER_ID_IN_CONTROL_GROUP)).willReturn(playerProfile);


        PaymentOption paymentOption = underTest.getPaymentOptionFor(PLAYER_ID_NOT_IN_CONTROL_GROUP,
                PROMO_ID,
                PaymentPreferences.PaymentMethod.FACEBOOK,
                "option1");

        PaymentOption expected = toOption("option1", "$", 10000, "5", "USD");
        expected.addPromotionPaymentOption(new PromotionPaymentOption(PaymentPreferences.PaymentMethod.FACEBOOK,
                1L,
                valueOf(10000),
                "",
                ""));
        assertThat(paymentOption, is(expected));
    }

    private Map<Currency, List<PaymentOption>> createPaymentOptionsFromDefaultsAndPromotion(Promotion promotion, Platform platform) {
        final Map<Currency, List<PaymentOption>> expectedPaymentOptions = new HashMap<>();
        PromotionConfiguration configuration = promotion.getConfiguration();
        String paymentMethods = configuration.getConfigurationValue(PAYMENT_METHODS_KEY);
        String rolloverHeader = configuration.getConfigurationValue(ROLLOVER_HEADER_KEY);
        String rolloverText = configuration.getConfigurationValue(ROLLOVER_TEXT_KEY);
        for (PaymentOption paymentOption : paymentOptionDAO.findByPlatform(platform)) {
            final Currency acceptedCurrency = Currency.valueOf(paymentOption.getRealMoneyCurrency());
            List<PaymentOption> paymentOptions = expectedPaymentOptions.get(acceptedCurrency);
            if (paymentOptions == null) {
                paymentOptions = new ArrayList<>();
                expectedPaymentOptions.put(acceptedCurrency, paymentOptions);
            }
            paymentOptions.add(paymentOption);
            final BigDecimal promoChips = configuration.getConfigurationValueAsBigDecimal(String.format(CHIP_AMOUNT_FORMAT_KEY,
                    platform,
                    paymentOption.getNumChipsPerPurchase().toString()));
            if (promoChips != null) {
                for (String method : paymentMethods.split(",")) {
                    PaymentPreferences.PaymentMethod paymentMethod = PaymentPreferences.PaymentMethod.valueOf(method);
                    PromotionPaymentOption promotionPaymentOption = new PromotionPaymentOption(paymentMethod,
                            promotion.getId(), promoChips, rolloverHeader, rolloverText);
                    paymentOption.addPromotionPaymentOption(promotionPaymentOption);
                }
            }
        }
        for (List<PaymentOption> paymentOptions : expectedPaymentOptions.values()) {
            Collections.sort(paymentOptions);
        }
        return expectedPaymentOptions;
    }

    // factory methods for fluent language
    private static <T> Matcher<? super Collection<T>> collectionEqualTo(Collection<T> col) {
        return new CollectionIsEqual<>(col);
    }

    private static class InGameMessageIsEqual extends TypeSafeMatcher<InGameMessage> {

        private final InGameMessage inGameMessage;

        private InGameMessageIsEqual(InGameMessage inGameMessage) {
            this.inGameMessage = inGameMessage;
        }

        @Override
        protected boolean matchesSafely(InGameMessage other) {
            return StringUtils.equals(inGameMessage.getHeader(), other.getHeader()) &&
                    StringUtils.equals(inGameMessage.getMessage(), other.getMessage());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(inGameMessage.toString());
        }

        public static TypeSafeMatcher<InGameMessage> equalTo(InGameMessage inGameMessage) {
            return new InGameMessageIsEqual(inGameMessage);
        }

    }

    private static class CollectionIsEqual<T> extends BaseMatcher<Collection<T>> {

        private final Collection<T> collection;

        private CollectionIsEqual(Collection<T> collection) {
            this.collection = collection;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object o) {
            Collection<T> col = (Collection<T>) o;
            return collection == col
                    || collection != null
                    && collection.size() == col.size()
                    && collection.containsAll(col);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(collection.toString());
        }

    }

    private Map<Currency, List<PaymentOption>> defaultWebChipPackages() {
        List<PaymentOption> USD = Arrays.asList(toOption("option1", "$", 10000, "5", "USD"), toOption("option2", "$", 21000, "10", "USD"));
        List<PaymentOption> EUR = Arrays.asList(toOption("option3", "€", 10000, "3.50", "EUR"),
                toOption("option4", "€", 21000, "7", "EUR"));
        List<PaymentOption> GBP = Arrays.asList(toOption("option5", "£", 10000, "3", "GBP"), toOption("option6", "£", 21000, "6", "GBP"));
        List<PaymentOption> CAD = Arrays.asList(toOption("option7", "$", 10000, "3", "CAD"), toOption("option8", "$", 21000, "5", "CAD"));
        List<PaymentOption> AUD = Arrays.asList(toOption("option9", "$", 10000, "3", "AUD"), toOption("option9", "$", 21000, "4", "AUD"));

        Map<Currency, List<PaymentOption>> optionMap = new HashMap<>();
        optionMap.put(Currency.USD, USD);
        optionMap.put(Currency.EUR, EUR);
        optionMap.put(Currency.GBP, GBP);
        optionMap.put(Currency.CAD, CAD);
        optionMap.put(Currency.AUD, AUD);

        return optionMap;
    }

    private Map<Currency, List<PaymentOption>> defaultIOSChipPackages() {
        List<PaymentOption> USD = Arrays.asList(toOption("USD5", "$", 10000, "5", "USD"), toOption("USD10", "$", 21000, "10", "USD"));
        Map<Currency, List<PaymentOption>> optionMap = new HashMap<>();
        optionMap.put(Currency.USD, USD);
        return optionMap;
    }

    private Map<Currency, List<PaymentOption>> defaultAndroidChipPackages() {
        List<PaymentOption> USD = Arrays.asList(
                toOption("USD5", "$", 10000, "5", "USD"),
                toOption("USD10", "$", 21000, "10", "USD"));
        Map<Currency, List<PaymentOption>> optionMap = new HashMap<>();
        optionMap.put(Currency.USD, USD);
        return optionMap;
    }

    private Map<Currency, List<PaymentOption>> defaultFacebookChipPackages() {
        List<PaymentOption> USD = Arrays.asList(
                toOption("option1", "$", 10000, "5", "USD"),
                toOption("option2", "$", 200000, "10", "USD"),
                toOption("option3", "$", 500000, "20", "USD"),
                toOption("option4", "$", 1000000, "100", "USD"));

        Map<Currency, List<PaymentOption>> optionMap = new HashMap<>();
        optionMap.put(Currency.USD, USD);
        return optionMap;
    }

    private PaymentOption toOption(String id, String currencyLabel, int chips, String realMoney, String currency) {
        return new PaymentOptionBuilder().setId(id).setCurrencyLabel(currencyLabel)
                .setNumChipsPerPurchase(new BigDecimal(chips)).setAmountRealMoneyPerPurchase(new BigDecimal(realMoney))
                .setRealMoneyCurrency(currency).createPaymentOption();

    }
}
