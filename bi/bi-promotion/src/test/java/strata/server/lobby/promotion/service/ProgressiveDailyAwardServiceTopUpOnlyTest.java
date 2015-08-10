package strata.server.lobby.promotion.service;

import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.promotion.PromoRewardEvent;
import com.yazino.promotions.PromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.yazino.platform.Platform.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.promotion.TopUpStatus.ACKNOWLEDGED;
import static strata.server.lobby.api.promotion.TopUpStatus.CREDITED;

public class ProgressiveDailyAwardServiceTopUpOnlyTest {
    public static final BigDecimal PLAYER_ID = new BigDecimal(String.valueOf(-2));
    public static final BigDecimal SESSION_ID = new BigDecimal(String.valueOf(-3));
    // NOTE this is also 1980 02 03 NY time
    public static final DateTime CURRENT_TIME = new DateTime(1980, 2, 3, 18, 30, 12, 0);
    public static final DateTime LAST_TOPUP_DATE = CURRENT_TIME.minusMinutes(1);
    public static final BigDecimal BALANCE = new BigDecimal(1066);
    // day 1, day 2 and day 3 amounts
    public static final BigDecimal[] PROGRESSIVE_DAY_AMOUNTS = {
            BigDecimal.valueOf(2500), BigDecimal.valueOf(3000), BigDecimal.valueOf(3500), BigDecimal.valueOf(4000), BigDecimal.valueOf(5000)};
    public static final BigDecimal DAILY_AWARD_AMOUNT = BigDecimal.valueOf(76);
    public static final long DAILY_AWARD_PROMO_ID = 20L;
    public static final long PROGRESSIVE_PROMO_ID = 10L;
    public static final long BUY_CHIPS_PROMO_ID = 89L;
    public static final String ASSET_URL = "http://defaultUrl.com/";
    public static final String MAIN_IMAGE = ASSET_URL + "images/gloss/defaultMainImage.png";
    public static final String DAILY_AWARD_MAIN_IMAGE_OVERRIDE = ASSET_URL + "images/gloss/overriddenMainImage.png";
    public static final String DAILY_AWARD_MAIN_IMAGE_LINK_OVERRIDE = "http://go/here/when/you/click";
    public static final String BUY_CHIPS_MAIN_IMAGE_OVERRIDE = ASSET_URL + "images/gloss/overriddenMainImageBuyChips.png";
    public static final String BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE = "http://go/here/when/you/click/buychips";
    public static final String BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_FACEBOOK = ASSET_URL + "images/gloss/facebookeOverriddenMainImageBuyChips.png";
    public static final String BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK = "http://go/here/when/you/click/facebook/buychips";
    public static final String BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_IOS = ASSET_URL + "images/gloss/IOS/Image.png";
    public static final String BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_ANDROID = ASSET_URL + "images/gloss/ANDROID/Image.png";
    public static final String SECONDARY_IMAGE = ASSET_URL + "images/gloss/defaultSecondImage.png";
    public static final String SECONDARY_LINK_IMAGE = "www.defaultLink.com";
    public static final String DAILY_AWARD_SECONDARY_IMAGE_OVERRIDE = ASSET_URL + "images/gloss/overriddenSecondImage.png";
    public static final String DAILY_AWARD_SECONDARY_IMAGE_LINK_OVERRIDE = "http://go/here/when/you/click/on/the/secondary/image/link";
    public static final String BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE = ASSET_URL + "images/gloss/overriddenSecondImage.png";
    public static final String BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE = "http://go/here/when/you/click/on/the/secondary/image/link";
    public static final String BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE_FOR_FACEBOOK = ASSET_URL + "images/gloss/facebook/overriddenSecondImage.png";
    public static final String BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK = "http://facebook/go/here/when/you/click/on/the/secondary/image/link";
    public static final PlayerProfile PLAYER_PROFILE = new PlayerProfile();

    ProgressiveDailyAwardService underTest;

    @Mock
    PlayerPromotionStatusDao playerPromotionStatusDao;

    @Mock
    PromotionDao promotionDao;

    @Mock
    com.yazino.platform.community.PlayerService playerService;

    @Mock
    PromotionControlGroupService promotionControlGroupService;

    @Mock
    private PlayerProfileService playerProfileService;

    @Mock
    private QueuePublishingService<TopUpAcknowledgeRequest> topUpAcknowledgedRequestQueuePublishingService;
    @Mock
    private QueuePublishingService<PromoRewardEvent> promotionRewardEventService;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new ProgressiveDailyAwardService(promotionDao,
                playerPromotionStatusDao,
                playerService,
                promotionControlGroupService,
                ASSET_URL,
                MAIN_IMAGE,
                SECONDARY_IMAGE,
                SECONDARY_LINK_IMAGE,
                playerProfileService,
                promotionRewardEventService);
    }

    @Test
    public void awardDailyTopUpShouldUpdateLastTopUpDate() throws Exception {
        setUpPlayerWithProgressiveAward(2, 3);

        ArgumentCaptor<PlayerPromotionStatus> argument = ArgumentCaptor.forClass(PlayerPromotionStatus.class);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerPromotionStatusDao, times(1)).save(argument.capture());

        assertThat(CURRENT_TIME, is(argument.getValue().getLastTopup()));
    }

    @Test
    public void ifPlayerDidNotPlayYesterdayThenHeShouldBeAwardedDay1ChipAmount() throws Exception {
        final BigDecimal promotionAmount = new BigDecimal(2500);

        setUpPlayerWithProgressiveAward(2, 3);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID,
                SESSION_ID, promotionAmount,
                "Progressive",
                "Day AWARD_1");
    }

    @Test
    public void ifPlayerPlayedYesterdayWith1ConsecutiveDayThenHeShouldBeAwardedDay2ChipAmount() throws Exception {
        setUpPlayerWithProgressiveAward(1, 1);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, PROGRESSIVE_DAY_AMOUNTS[1], "Progressive", "Day AWARD_2");
    }

    @Test
    public void ifPlayerPlayedYesterdayWith2ConsecutiveDayThenHeShouldBeAwardedDay3ChipAmount() throws Exception {
        setUpPlayerWithProgressiveAward(1, 2);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, PROGRESSIVE_DAY_AMOUNTS[2], "Progressive", "Day AWARD_3");
    }

    @Test
    public void ifPlayerPlayedYesterdayWith3ConsecutiveDayThenHeShouldBeAwardedDay4ChipAmount() throws Exception {
        setUpPlayerWithProgressiveAward(1, 3);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, PROGRESSIVE_DAY_AMOUNTS[3], "Progressive",  "Day AWARD_4");
    }

    @Test
    public void ifPlayerPlayedYesterdayWith4ConsecutiveDayThenHeShouldBeAwardedDay5ChipAmount() throws Exception {
        setUpPlayerWithProgressiveAward(1, 4);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, PROGRESSIVE_DAY_AMOUNTS[4], "Progressive",  "Day AWARD_5");
    }

    @Test
    public void ifPlayerDidNotPlayYesterdayThenConsecutiveDaysPlayedShouldBeSetToZero() throws Exception {
        setUpPlayerWithProgressiveAward(2, 3);

        ArgumentCaptor<PlayerPromotionStatus> argument = ArgumentCaptor.forClass(PlayerPromotionStatus.class);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerPromotionStatusDao, times(1)).save(argument.capture());

        assertThat(0, is(argument.getValue().getConsecutiveDaysPlayed()));
    }

    @Test
    public void ifPlayerPlayedYesterdayThenConsecutiveDaysShouldNotBeReset() throws Exception {
        setUpPlayerWithProgressiveAward(1, 3);

        ArgumentCaptor<PlayerPromotionStatus> argument = ArgumentCaptor.forClass(PlayerPromotionStatus.class);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerPromotionStatusDao, times(1)).save(argument.capture());

        assertThat(3, is(argument.getValue().getConsecutiveDaysPlayed()));
    }

    // sets up, promotion player status, progressive promotion and wallet mocks
    private PlayerPromotionStatus setUpPlayerWithProgressiveAward(int numberOfDaysSinceLastPlayed,
                                                                  int consecutiveDays) throws WalletServiceException {
        final PlayerPromotionStatus originalPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withConsecutiveDaysPlayed(consecutiveDays)
                .withPlayerId(PLAYER_ID)
                .withLastPlayed(CURRENT_TIME.minusDays(numberOfDaysSinceLastPlayed))
                .withLastTopupDate(CURRENT_TIME.minusDays(1))
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(originalPlayerPromotionStatus);
        BigDecimal progressiveDayAmount;
        if (numberOfDaysSinceLastPlayed > 1) {
            // player did NOT play yesterday so gets day 1 promo
            progressiveDayAmount = PROGRESSIVE_DAY_AMOUNTS[0];
            final ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion = new ProgressiveDailyAwardPromotion(
                    PromotionType.PROGRESSIVE_DAY_1, PROGRESSIVE_PROMO_ID, progressiveDayAmount);
            List<ProgressiveDailyAwardPromotion> list = Arrays.asList(progressiveDailyAwardPromotion);
            when(promotionDao.getProgressiveDailyAwardPromotion(PLAYER_ID, CURRENT_TIME,
                    ProgressiveAwardEnum.AWARD_1)).thenReturn(list);
        } else {
            // player has played for last N days so gets day N award
            final PromotionType progressiveType = PromotionType.valueOf("PROGRESSIVE_DAY_" + consecutiveDays);
            progressiveDayAmount = PROGRESSIVE_DAY_AMOUNTS[consecutiveDays];
            final ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion = new ProgressiveDailyAwardPromotion(
                    progressiveType, PROGRESSIVE_PROMO_ID, progressiveDayAmount);
            List<ProgressiveDailyAwardPromotion> list = Arrays.asList(progressiveDailyAwardPromotion);
            final ProgressiveAwardEnum awardType = ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(consecutiveDays);
            when(promotionDao.getProgressiveDailyAwardPromotion(PLAYER_ID, CURRENT_TIME,
                    awardType)).thenReturn(
                    list);
        }
        when(playerService.postTransaction(eq(PLAYER_ID), eq(SESSION_ID), eq(progressiveDayAmount), anyString(), anyString())).thenReturn(
                BALANCE);
        return originalPlayerPromotionStatus;
    }

    @Test
    public void awardDailyTopUpShouldCheckForOtherDailyAwardPromotions() throws Exception {
        setUpProgressiveAndOtherDailyAwardPromotionDaoMock(WEB);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(promotionDao, times(1)).findPromotionsByTypeOrderByPriority(PLAYER_ID, PromotionType.DAILY_AWARD,
                WEB, CURRENT_TIME);
    }

    @Test
    public void awardDailyTopUpShouldUpdateWalletWithDailyAwardAmount() throws Exception {
        setUpProgressiveAndOtherDailyAwardPromotionDaoMock(WEB);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, DAILY_AWARD_AMOUNT, "TopUp", "Other Daily Award Promotion");
    }

    @Test
    public void awardDailyTopUpForIOSShouldUpdateWalletWithDailyAwardAmount() throws Exception {
        setUpProgressiveAndOtherDailyAwardPromotionDaoMock(IOS);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, IOS, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, DAILY_AWARD_AMOUNT, "TopUp", "Other Daily Award Promotion");
    }

    @Test
    public void awardDailyTopUpForFACEBOOK_CANVASShouldUpdateWalletWithDailyAwardAmount() throws Exception {
        setUpProgressiveAndOtherDailyAwardPromotionDaoMock(FACEBOOK_CANVAS);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, FACEBOOK_CANVAS, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID, SESSION_ID, DAILY_AWARD_AMOUNT, "TopUp", "Other Daily Award Promotion");
    }

    @Test
    public void awardDailyTopUpShouldWritePromotionPlayerRewardWhenAwardingProgressiveAward() throws Exception {
        setUpPlayerWithProgressiveAward(1, 2);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, IOS, CURRENT_TIME, SESSION_ID));

        final PromotionPlayerReward expectedPromotionPlayerReward = new PromotionPlayerReward(PROGRESSIVE_PROMO_ID,
                PLAYER_ID,
                false,
                CURRENT_TIME,
                "reward=" + PROGRESSIVE_DAY_AMOUNTS[2]);
        verify(promotionDao, times(1)).addLastReward(expectedPromotionPlayerReward);
        verify(promotionRewardEventService).send(new PromoRewardEvent(PLAYER_ID,PROGRESSIVE_PROMO_ID,CURRENT_TIME));
    }

    @Test
    public void awardDailyTopUpShouldNotTopUpPlayerIfLastTopUpDateIsTodayNYTime() throws Exception {

        final PlayerPromotionStatus playerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withConsecutiveDaysPlayed(0)
                .withLastPlayed(new DateTime(CURRENT_TIME, DateTimeZone.forID("America/New_York")))
                .withLastTopupDate(new DateTime(CURRENT_TIME, DateTimeZone.forID("America/New_York")))
                .build();

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verifyZeroInteractions(playerService);
        verifyZeroInteractions(promotionDao);
        verifyZeroInteractions(promotionRewardEventService);
    }

    @Test
    public void awardDailyTopUpShouldTopUpIfLastPlayedDateIsNull() throws Exception {
        final BigDecimal promotionAmount = new BigDecimal(2500);

        new PlayerPromotionStatusBuilder(setUpPlayerWithProgressiveAward(2, 3)).withLastTopupDate(null).build();

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        verify(playerService).postTransaction(PLAYER_ID,
                SESSION_ID, promotionAmount,
                "Progressive",
                "Day AWARD_1");
    }

    @Test
    public void dailyAwardShouldNotBeGivenWhenPlayerIsInControlGroup() throws WalletServiceException {
        final Promotion dailyAwardPromotion = setUpProgressiveAndOtherDailyAwardPromotionDaoMock(WEB);

        when(promotionControlGroupService.isControlGroupMember(PLAYER_PROFILE, dailyAwardPromotion)).thenReturn(true);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        final PromotionPlayerReward progressivePromotionPlayerReward = new PromotionPlayerReward(PROGRESSIVE_PROMO_ID,
                PLAYER_ID,
                false,
                CURRENT_TIME,
                "reward=" + PROGRESSIVE_DAY_AMOUNTS[2]);
        verify(promotionDao, times(1)).addLastReward(progressivePromotionPlayerReward);
        verify(promotionDao, times(1)).addLastReward(any(PromotionPlayerReward.class));
        verify(promotionRewardEventService).send(new PromoRewardEvent(PLAYER_ID,PROGRESSIVE_PROMO_ID,CURRENT_TIME));

    }

    @Test
    public void bothProgressiveAndDailyAwardsShouldBeGivenWhenPlayerIsNotInControlGroup() throws WalletServiceException {
        setUpProgressiveAndOtherDailyAwardPromotionDaoMock(WEB);

        underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, WEB, CURRENT_TIME, SESSION_ID));

        final PromotionPlayerReward progressivePromotionPlayerReward = new PromotionPlayerReward(PROGRESSIVE_PROMO_ID,
                PLAYER_ID,
                false,
                CURRENT_TIME,
                "reward=" + PROGRESSIVE_DAY_AMOUNTS[2]);
        final PromotionPlayerReward dailyPromotionPlayerReward = new PromotionPlayerReward(DAILY_AWARD_PROMO_ID,
                PLAYER_ID,
                false,
                CURRENT_TIME,
                "reward=" + DAILY_AWARD_AMOUNT);
        verify(promotionDao).addLastReward(progressivePromotionPlayerReward);
        verify(promotionDao).addLastReward(dailyPromotionPlayerReward);
        verify(promotionRewardEventService).send(new PromoRewardEvent(PLAYER_ID,DAILY_AWARD_PROMO_ID,CURRENT_TIME));
        verify(promotionRewardEventService).send(new PromoRewardEvent(PLAYER_ID,PROGRESSIVE_PROMO_ID,CURRENT_TIME));
    }

    @Test
    public void getTopUpResultShouldReturnACKNOWLEDGEDWhenPlayerHasBeenToppedUpTodayAndHasAcknowledgedTheTopUp() {
        // given a player that has NOT been topped up today
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(true)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);

        // when requesting the top up result
        final TopUpResult actualResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be
        assertThat(actualResult.getStatus(), is(ACKNOWLEDGED));
    }

    @Test
    public void getTopUpResultShouldReturnNEVER_CREDITEDWhenLastTopUpIsNull() {
        // given a player that has never been topped up
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withLastTopupDate(null)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);

        // when requesting the top up result
        final TopUpResult actualResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be
        TopUpResult expectedResult = new TopUpResult(PLAYER_ID, TopUpStatus.NEVER_CREDITED, null);
        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void getTopUpResultShouldReturnCreditedWhenPlayerHasBeenToppedUpTodayAndHasNotAcknowledgedResult() {
        // given a player that was topped up today and has not acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);

        // when requesting the top up result
        final TopUpResult actualResult = underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result status should be Credited
        assertThat(actualResult.getStatus(), is(CREDITED));
    }

    @Test
    public void topUpResultHasProgressiveAwardDetailsWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedResult() {
        // given a player has received a progressive top up
        setUpProgressiveDay3TopUp();
        // and that he has received a daily award top up
        PromotionPlayerReward progressiveAward = new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false,
                LAST_TOPUP_DATE,
                "daily award");
        // and the promotion player reward for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                progressiveAward));
        setUpProgressiveDay3PromotionMocks();

        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be ...
        WebTopUpResult expectedResult = expectedWebTopUpResult();
        assertThat(actualResult, is(expectedResult));
    }

    private WebTopUpResult expectedWebTopUpResult() {
        WebTopUpResult expectedResult = new WebTopUpResult(PLAYER_ID, CREDITED, LAST_TOPUP_DATE);
        expectedResult.setConsecutiveDaysPlayed(2);
        expectedResult.setTotalTopUpAmount(PROGRESSIVE_DAY_AMOUNTS[2]);
        expectedResult.setPromotionValueList(Arrays.asList(PROGRESSIVE_DAY_AMOUNTS));
        expectedResult.setMainImage(MAIN_IMAGE);
        expectedResult.setSecondaryImage(SECONDARY_IMAGE);
        expectedResult.setSecondaryImageLink(SECONDARY_LINK_IMAGE);
        return expectedResult;
    }

    @Test
    public void topUpResultHasProgressiveAndDailyAwardDetailsWhenPlayerHasBeenToppedAndHasNotAcknowledgedResult() {
        // given a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(DAILY_AWARD_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "daily award"),
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // and the progressive award promotion was
        setUpProgressiveDay3PromotionMocks();
        // and the daily award promotion was
        setUpDailyAwardPromotionMock();

        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be ...
        WebTopUpResult expectedResult = expectedWebTopResultWithDailyAwardOverrides();
        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void topUpResultHasProgressiveDetailsOnlyWhenPlayerIsInControlGroupOfDailyAwardAndHasNotAcknowledgedResult() {
        // given a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(DAILY_AWARD_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "daily award"),
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // and the progressive award promotion was
        setUpProgressiveDay3PromotionMocks();
        // and the daily award promotion was
        final DailyAwardPromotion dailyAwardPromotion = setUpDailyAwardPromotionMock();

        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(PLAYER_PROFILE);
        when(promotionControlGroupService.isControlGroupMember(PLAYER_PROFILE, dailyAwardPromotion)).thenReturn(Boolean.TRUE);

        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be ...
        WebTopUpResult expectedResult = expectedWebTopUpResult();
        assertThat(actualResult, is(expectedResult));
    }

    private WebTopUpResult expectedWebTopResultWithDailyAwardOverrides() {
        final WebTopUpResult expectedResult = expectedWebTopUpResult();
        expectedResult.setTotalTopUpAmount(expectedResult.getTotalTopUpAmount().add(DAILY_AWARD_AMOUNT));
        expectedResult.setMainImage(DAILY_AWARD_MAIN_IMAGE_OVERRIDE);
        expectedResult.setMainImageLink(DAILY_AWARD_MAIN_IMAGE_LINK_OVERRIDE);
        expectedResult.setSecondaryImage(DAILY_AWARD_SECONDARY_IMAGE_OVERRIDE);
        expectedResult.setSecondaryImageLink(DAILY_AWARD_SECONDARY_IMAGE_LINK_OVERRIDE);
        return expectedResult;
    }

    @Test
    public void topUpResultHasProgressiveDailyAndBuyChipDetailsWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedResult() {
        // given the progressive promotion
        setUpProgressiveDay3PromotionMocks();
        // and the daily award promotion
        setUpDailyAwardPromotionMock();
        // and the buy chip promotion
        setUpBuyChipPromotionMock(WEB);
        // and a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(DAILY_AWARD_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "daily award"),
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));

        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be ...
        WebTopUpResult expectedResult = expectedWebTopResultWithBuyChipsOverrides();
        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void topUpResultHasNoBuyChipDetailsWhenPlayerIsInControlGroup() {
        // given the progressive promotion
        setUpProgressiveDay3PromotionMocks();
        // and the daily award promotion
        setUpDailyAwardPromotionMock();
        // and the buy chip promotion
        final BuyChipsPromotion buyChipsPromotion = setUpBuyChipPromotionMock(WEB);
        // and a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(DAILY_AWARD_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "daily award"),
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // put player in buy chip control control group
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(PLAYER_PROFILE);
        when(promotionControlGroupService.isControlGroupMember(PLAYER_PROFILE, buyChipsPromotion)).thenReturn(Boolean.TRUE);

        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, WEB);

        // then the result should be ...
        WebTopUpResult expectedResult = expectedWebTopResultWithDailyAwardOverrides();
        assertThat(actualResult, is(expectedResult));
    }

    private WebTopUpResult expectedWebTopResultWithBuyChipsOverrides() {
        WebTopUpResult expectedResult = new WebTopUpResult(PLAYER_ID, CREDITED, LAST_TOPUP_DATE);
        expectedResult.setConsecutiveDaysPlayed(2);
        expectedResult.setTotalTopUpAmount(PROGRESSIVE_DAY_AMOUNTS[2].add(DAILY_AWARD_AMOUNT));
        expectedResult.setPromotionValueList(Arrays.asList(PROGRESSIVE_DAY_AMOUNTS));
        expectedResult.setMainImage(BUY_CHIPS_MAIN_IMAGE_OVERRIDE);
        expectedResult.setMainImageLink(BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE);
        expectedResult.setSecondaryImage(BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE);
        expectedResult.setSecondaryImageLink(BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE);
        return expectedResult;
    }

    @Test
    public void topUpResultHasFacebookCanvasBuyChipDetailsWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedResult() {
        // given the progressive promotion
        setUpProgressiveDay3PromotionMocks();
        // and the buy chip promotion
        setUpBuyChipPromotionMock(FACEBOOK_CANVAS);
        // and a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // when requesting the top up result
        final WebTopUpResult actualResult = (WebTopUpResult) underTest.getTopUpResult(PLAYER_ID, FACEBOOK_CANVAS);

        // then the result should be ...
        WebTopUpResult expectedResult = new WebTopUpResult(PLAYER_ID, CREDITED, LAST_TOPUP_DATE);
        expectedResult.setConsecutiveDaysPlayed(2);
        expectedResult.setTotalTopUpAmount(PROGRESSIVE_DAY_AMOUNTS[2]);
        expectedResult.setPromotionValueList(Arrays.asList(PROGRESSIVE_DAY_AMOUNTS));
        expectedResult.setMainImage(BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_FACEBOOK);
        expectedResult.setMainImageLink(BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK);
        expectedResult.setSecondaryImage(BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE_FOR_FACEBOOK);
        expectedResult.setSecondaryImageLink(BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK);
        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void topUpResultHasIOSBuyChipDetailsWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedResult() {
        // given the progressive promotion
        setUpProgressiveDay3PromotionMocks();
        // and the buy chip promotion
        setUpBuyChipPromotionMock(IOS);
        // and a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // when requesting the top up result
        final MobileTopUpResult actualResult = (MobileTopUpResult) underTest.getTopUpResult(PLAYER_ID, IOS);

        // then the result should be ...
        MobileTopUpResult expectedResult = new MobileTopUpResult(PLAYER_ID, CREDITED, LAST_TOPUP_DATE);
        expectedResult.setConsecutiveDaysPlayed(2);
        expectedResult.setTotalTopUpAmount(PROGRESSIVE_DAY_AMOUNTS[2]);
        expectedResult.setImageUrl(BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_IOS);
        assertThat(actualResult, is(expectedResult));
    }

    @Test
    public void topUpResultHasAndroidBuyChipDetailsWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedResult() {
        // given the progressive promotion
        setUpProgressiveDay3PromotionMocks();
        // and the buy chip promotion
        setUpBuyChipPromotionMock(ANDROID);
        // and a player that was topped up today with a progressive award and a daily award and has NOT acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player rewards for the top up date was...
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false, LAST_TOPUP_DATE, "progressive award")
        ));
        // when requesting the top up result
        final MobileTopUpResult actualResult = (MobileTopUpResult) underTest.getTopUpResult(PLAYER_ID, ANDROID);

        // then the result should be ...
        MobileTopUpResult expectedResult = new MobileTopUpResult(PLAYER_ID, CREDITED, LAST_TOPUP_DATE);
        expectedResult.setConsecutiveDaysPlayed(2);
        expectedResult.setTotalTopUpAmount(PROGRESSIVE_DAY_AMOUNTS[2]);
        expectedResult.setImageUrl(BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_ANDROID);
        assertThat(actualResult, is(expectedResult));
    }

    private BuyChipsPromotion setUpBuyChipPromotionMock(Platform platform) {
        // and the buy chip promotion was ...
        final BuyChipsPromotion buyChipsPromotion = new BuyChipsPromotion();
        buyChipsPromotion.setId(BUY_CHIPS_PROMO_ID);
        final PromotionConfiguration promotionConfiguration = new PromotionConfiguration();
        if (platform == WEB) {
            promotionConfiguration.addConfigurationItem("main.image", BUY_CHIPS_MAIN_IMAGE_OVERRIDE);
            promotionConfiguration.addConfigurationItem("main.image.link", BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE);
            promotionConfiguration.addConfigurationItem("secondary.image", BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE);
            promotionConfiguration.addConfigurationItem("secondary.image.link", BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE);
        } else if (platform == FACEBOOK_CANVAS) {
            promotionConfiguration.addConfigurationItem("main.image", BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_FACEBOOK);
            promotionConfiguration.addConfigurationItem("main.image.link", BUY_CHIPS_MAIN_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK);
            promotionConfiguration.addConfigurationItem("secondary.image", BUY_CHIPS_SECONDARY_IMAGE_OVERRIDE_FOR_FACEBOOK);
            promotionConfiguration.addConfigurationItem("secondary.image.link", BUY_CHIPS_SECONDARY_IMAGE_LINK_OVERRIDE_FOR_FACEBOOK);
        } else if (platform == IOS) {
            promotionConfiguration.addConfigurationItem("ios.image", BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_IOS);
        } else if (platform == ANDROID) {
            promotionConfiguration.addConfigurationItem("android.image", BUY_CHIPS_MAIN_IMAGE_OVERRIDE_FOR_ANDROID);
        }
        buyChipsPromotion.setConfiguration(promotionConfiguration);
        when(promotionDao.findPromotionsByTypeOrderByPriority(
                PLAYER_ID, PromotionType.BUY_CHIPS, platform, LAST_TOPUP_DATE))
                .thenReturn(Arrays.asList((Promotion)buyChipsPromotion));
        return buyChipsPromotion;
    }

    // sets up a progressive day 2 award and a daily award
    private Promotion setUpProgressiveAndOtherDailyAwardPromotionDaoMock(Platform platform) throws WalletServiceException {
        setUpPlayerWithProgressiveAward(1, 2);

        final Promotion dailyAwardPromotion = new DailyAwardPromotion();
        dailyAwardPromotion.setId(DAILY_AWARD_PROMO_ID);
        final PromotionConfiguration promotionConfiguration = new PromotionConfiguration();
        promotionConfiguration.addConfigurationItem("reward.chips", DAILY_AWARD_AMOUNT.toPlainString());

        dailyAwardPromotion.setConfiguration(promotionConfiguration);

        List<Promotion> list = Arrays.asList(dailyAwardPromotion);
        when(promotionDao.findPromotionsByTypeOrderByPriority(PLAYER_ID, PromotionType.DAILY_AWARD,
                platform, CURRENT_TIME)).thenReturn(list);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(PLAYER_PROFILE);
        return dailyAwardPromotion;
    }

    // sets a daily award with images/links
    private DailyAwardPromotion setUpDailyAwardPromotionMock() {
        final DailyAwardPromotion dailyAwardPromotion = new DailyAwardPromotion();
        dailyAwardPromotion.setId(DAILY_AWARD_PROMO_ID);
        final PromotionConfiguration promotionConfiguration = new PromotionConfiguration();
        promotionConfiguration.addConfigurationItem("reward.chips", DAILY_AWARD_AMOUNT.toPlainString());
        promotionConfiguration.addConfigurationItem("main.image", DAILY_AWARD_MAIN_IMAGE_OVERRIDE);
        promotionConfiguration.addConfigurationItem("main.image.link", DAILY_AWARD_MAIN_IMAGE_LINK_OVERRIDE);
        promotionConfiguration.addConfigurationItem("secondary.image", DAILY_AWARD_SECONDARY_IMAGE_OVERRIDE);
        promotionConfiguration.addConfigurationItem("secondary.image.link", DAILY_AWARD_SECONDARY_IMAGE_LINK_OVERRIDE);
        dailyAwardPromotion.setConfiguration(promotionConfiguration);
        when(promotionDao.findById(DAILY_AWARD_PROMO_ID)).thenReturn(dailyAwardPromotion);
        return dailyAwardPromotion;
    }

    private void setUpProgressiveDay3TopUp() {
        // given a player that was topped up today with a progressive award and has not acknowledged the result
        final PlayerPromotionStatus playerPromotionStatus = createPlayerToppedUpToday()
                .withTopUpAcknowledged(false)
                .withConsecutiveDaysPlayed(2)
                .build();
        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(playerPromotionStatus);
        // and the promotion player reward for the top up date was...
        PromotionPlayerReward progressiveAward = new PromotionPlayerReward(PROGRESSIVE_PROMO_ID, PLAYER_ID, false,
                LAST_TOPUP_DATE,
                "progressive award");
        when(promotionDao.findPromotionPlayerRewards(PLAYER_ID, LAST_TOPUP_DATE)).thenReturn(Arrays.asList(
                progressiveAward));
        // and the progressive award promotion was ...
        setUpProgressiveDay3PromotionMocks();
    }

    private void setUpProgressiveDay3PromotionMocks() {
        // given the progressive promotion
        final ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion = new ProgressiveDailyAwardPromotion(
                PromotionType.PROGRESSIVE_DAY_3,
                PROGRESSIVE_PROMO_ID,
                PROGRESSIVE_DAY_AMOUNTS[2]);
        when(promotionDao.findById(PROGRESSIVE_PROMO_ID)).thenReturn(progressiveDailyAwardPromotion);

        // and the list of progressive award amounts
        when(promotionDao.getProgressiveAwardPromotionValueList()).thenReturn(Arrays.asList(PROGRESSIVE_DAY_AMOUNTS));
    }

    private PlayerPromotionStatusBuilder createPlayerToppedUpToday() {
        return new PlayerPromotionStatusBuilder()
                .withPlayerId(PLAYER_ID)
                .withConsecutiveDaysPlayed(4)
                .withLastTopupDate(LAST_TOPUP_DATE)
                .withLastPlayed(CURRENT_TIME.minusDays(10))
                .withTopUpAcknowledged(true);
    }
}
