package com.yazino.engagement.campaign.application;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.BuyChipsPromotion;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionType;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PromotionCreationServiceTest {
    public static final long CAMPAIGN_ID = 1l;
    public static final long PROMO_ID = 5l;
    public static final long PROMOTION_DEFINITION_ID = 12321l;
    public static final String PROMOTION_NAME = "Promtion Name";
    public static final String IN_GAME_NOTIFICATION_MSG = "in game notification msg";
    public static final String IN_GAME_NOTIFICATION_HEADER = "in game notification header";
    public static final String ROLLOVER_HEADER = "rollover header";
    public static final String ROLLOVER_TEXT = "rolloverText";
    public static final int MAX_REWARDS = 2;
    public static final int VALID_FOR_HOURS = 24;
    public static final int PRIORITY = 1;
    public static final ArrayList<BigDecimal> TO_DELIVER = newArrayList(BigDecimal.TEN);
    public static final List<Platform> PLATFORMS = asList(Platform.IOS, Platform.ANDROID);
    public static final DateTime CURRENT_DATE = new DateTime(2013, 8, 6, 10, 0);
    public static final DateTime END_DATE = CURRENT_DATE.plusHours(VALID_FOR_HOURS);
    public static final int CONTROL_GROUP_PERCENTAGE = 0;
    public static final BigDecimal EIGHT_THOUSAND_CHIPS = new BigDecimal(8000);
    public static final BigDecimal TEN_THOUSAND_CHIPS = new BigDecimal(10000);
    public static final BigDecimal TWELVE_THOUSAND_CHIPS = new BigDecimal(12000);
    private PromotionCreationService underTest;

    @Mock
    private PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao;
    @Mock
    private MysqlPromotionDao mysqlPromotionDao;
    @Mock
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;

    @Mock
    private PromotionDefinitionDao promotionDefinitionDao;

    @Mock
    private PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao;

    @Mock
    private PromotionFormDefinitionDao<GiftingForm>  giftingPromotionDefinitionDao;

    public PromotionCreationServiceTest() {
    }

    @Before
    public void setUp() throws Exception {
        underTest = new PromotionCreationService(mysqlPromotionDao, buyChipsPromotionDefinitionDao, dailyAwardPromotionDefinitionDao, paymentOptionsToChipPackageTransformer, promotionDefinitionDao, giftingPromotionDefinitionDao);
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_DATE.getMillis());
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void createPromotionConfigShouldReturnCreatedPromoId() {
        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(createBuyChipsForm());
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);
        Assert.assertThat(underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER), is(IsEqual.equalTo(PROMO_ID)));
    }

    @Test
    public void promotionCreationShouldCheckTypeOfPromoToCreate() {
        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(createBuyChipsForm());
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);

        underTest.createPromotionForCampaign(CAMPAIGN_ID, newArrayList(BigDecimal.ONE));
    }

    @Test
    public void createPromotionShouldCreatePromotionWithPromotionDefinitionValues() {
        final BuyChipsForm buyChipsForm = createBuyChipsForm();
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);
        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(buyChipsForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        Assert.assertThat(underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER), is(IsEqual.equalTo(PROMO_ID)));
    }

    @Test
    public void createPromotionShouldCreateGiftingBuyChipsPromotion(){
             final GiftingForm giftingForm = createGiftingForm();
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.GIFTING);
        when(giftingPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(giftingForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);

        Assert.assertThat(underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER), is(IsEqual.equalTo(PROMO_ID)));

        verify(mysqlPromotionDao).addPlayersTo(PROMO_ID,new LinkedHashSet<>(TO_DELIVER));
        verify(mysqlPromotionDao).updatePlayerCountInPromotion(PROMO_ID);
        verifyZeroInteractions(buyChipsPromotionDefinitionDao);

    }

    @Test
    public void createPromotionShouldCreateBuyChipsPromotionIfThatIsThePromoType() {
        final BuyChipsForm buyChipsForm = createBuyChipsForm();

        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(buyChipsForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);
        Assert.assertThat(underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER), is(IsEqual.equalTo(PROMO_ID)));

        verify(mysqlPromotionDao).addPlayersTo(PROMO_ID, new LinkedHashSet<>(TO_DELIVER));
        verify(mysqlPromotionDao).updatePlayerCountInPromotion(PROMO_ID);
    }

    @Test
    public void createPromotionShouldCreateDailyAwardPromotion() {
        final DailyAwardForm dailyAwardPromotion = new DailyAwardForm();
        dailyAwardPromotion.setTopUpAmount(1234);

        verifyZeroInteractions(buyChipsPromotionDefinitionDao);

        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.DAILY_AWARD);
        when(dailyAwardPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(dailyAwardPromotion);

        Assert.assertThat(underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER), is(IsEqual.equalTo(PROMO_ID)));
        verify(mysqlPromotionDao).addPlayersTo(PROMO_ID,new LinkedHashSet<>(TO_DELIVER));
        verify(mysqlPromotionDao).updatePlayerCountInPromotion(PROMO_ID);
    }

    @Test
    public void createShouldSetPaymentOptionsForEachPlatformInPromo() {
        final BuyChipsForm buyChipsForm = createBuyChipsForm();

        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(buyChipsForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);


        underTest.createPromotionForCampaign(CAMPAIGN_ID, TO_DELIVER);

        final BuyChipsPromotion buyChipsPromotion = buildBuyChipsPromotion(buyChipsForm);

        verify(mysqlPromotionDao).create(buyChipsPromotion);
    }

    private BuyChipsPromotion buildBuyChipsPromotion(BuyChipsForm buyChipsForm) {
        return new BuyChipsPromotionBuilder()
                .withName(buyChipsForm.getName())
                .withInGameHeader(IN_GAME_NOTIFICATION_HEADER)
                .withInGameMessage(IN_GAME_NOTIFICATION_MSG)
                .withRollOverHeaderValue(ROLLOVER_HEADER)
                .withRollOverTextValue(ROLLOVER_TEXT)
                .withMaxRewards(MAX_REWARDS)
                .withStartDate(CURRENT_DATE)
                .withEndDate(END_DATE)
                .withPriority(PRIORITY)
                .withPlatforms(PLATFORMS)
                .withControlGroupFunction(ControlGroupFunctionType.PLAYER_ID)
                .withControlGroupPercentage(CONTROL_GROUP_PERCENTAGE)
                .withPaymentMethods(asList(PaymentPreferences.PaymentMethod.ITUNES, PaymentPreferences.PaymentMethod.GOOGLE_CHECKOUT))
                .build();
    }

    private GiftingForm createGiftingForm() {
        final GiftingForm giftingForm = new GiftingForm();
        giftingForm.setAllPlayers(true);
        giftingForm.setDescription("your mum");
        giftingForm.setTitle("I want");
        giftingForm.setReward(999L);
        giftingForm.setName("HARRO");
        giftingForm.setGameType("SLOTS");
        giftingForm.setCampaignId(123L);
        giftingForm.setPriority(99);
        giftingForm.setPlatforms(null);
        giftingForm.setValidForHours(24);
        giftingForm.setCampaignId(666L);
        return giftingForm;
    }

    @Test
    public void createShouldAddPlayersToPromotionAndUpdatePlayerCount() {
        final BuyChipsForm buyChipsForm = createBuyChipsForm();

        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(buyChipsForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);


        final List<BigDecimal> playerIds = asList(BigDecimal.ONE, BigDecimal.TEN);
        underTest.createPromotionForCampaign(CAMPAIGN_ID, playerIds);
        verify(mysqlPromotionDao).addPlayersTo(PROMO_ID, new LinkedHashSet<BigDecimal>(playerIds));
        verify(mysqlPromotionDao).updatePlayerCountInPromotion(PROMO_ID);
    }

    @Test
    public void createBuyChipsPromotionForAllPlayersShouldAddFlagOnPromotion() {
        final BuyChipsForm buyChipsForm = createBuyChipsForm();
        buyChipsForm.setAllPlayers(true);
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.BUY_CHIPS);
        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(buyChipsForm);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());

        underTest.createPromotionForCampaign(CAMPAIGN_ID,null);

        ArgumentCaptor<Promotion> promoCaptor= ArgumentCaptor.forClass(Promotion.class);
        verify(mysqlPromotionDao).create(promoCaptor.capture());
        final Promotion promotion = promoCaptor.getValue();
        assertThat(promotion.isAllPlayers(), is(true));
        verify(mysqlPromotionDao,never()).addPlayersTo(anyLong(),anySet());

    }

    @Test
    public void createDailyAwardPromotionForAllPlayersShouldAddFlagOnPromotion() {
        final DailyAwardForm dailyAwardPromotion = new DailyAwardForm();
        dailyAwardPromotion.setAllPlayers(true);
        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(PromotionType.DAILY_AWARD);
        when(dailyAwardPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(dailyAwardPromotion);
        when(mysqlPromotionDao.create(any(Promotion.class))).thenReturn(PROMO_ID);
        when(paymentOptionsToChipPackageTransformer.getDefaultPackages()).thenReturn(getDefaultPackages());

        underTest.createPromotionForCampaign(CAMPAIGN_ID,null);

        ArgumentCaptor<Promotion> promoCaptor= ArgumentCaptor.forClass(Promotion.class);
        verify(mysqlPromotionDao).create(promoCaptor.capture());
        final Promotion promotion = promoCaptor.getValue();
        assertThat(promotion.isAllPlayers(), is(true));
        verify(mysqlPromotionDao,never()).addPlayersTo(anyLong(),anySet());

    }

    private BuyChipsForm createBuyChipsForm() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setPromotionDefinitionId(PROMOTION_DEFINITION_ID);
        buyChipsForm.setName(PROMOTION_NAME);
        buyChipsForm.setInGameNotificationHeader(IN_GAME_NOTIFICATION_HEADER);
        buyChipsForm.setInGameNotificationMsg(IN_GAME_NOTIFICATION_MSG);
        buyChipsForm.setRolloverHeader(ROLLOVER_HEADER);
        buyChipsForm.setRolloverText(ROLLOVER_TEXT);
        buyChipsForm.setMaxRewards(MAX_REWARDS);
        buyChipsForm.setValidForHours(VALID_FOR_HOURS);
        buyChipsForm.setPriority(PRIORITY);
        buyChipsForm.setPlatforms(PLATFORMS);
        buyChipsForm.setCampaignId(CAMPAIGN_ID);

        buyChipsForm.setPlatforms(PLATFORMS);
        return buyChipsForm;
    }

    @Test
    public void setChipsPackageBasedOnPercentagesShouldPutRightAmountOfChipsToEachPlatform() {

        final BuyChipsForm buyChipsForm = createBuyChipsForm();
        final BuyChipsPromotion buyChipsPromotion = buildBuyChipsPromotion(buyChipsForm);

        Map<Platform, List<ChipPackage>> defaultPackages = getDefaultPackages();

        Map<Integer, BigDecimal> chipsPackagePercentages = newLinkedHashMap();
        chipsPackagePercentages.put(1, BigDecimal.ZERO);
        chipsPackagePercentages.put(2, BigDecimal.TEN);

        underTest.addChipsPackageBasedOnPercentages(buyChipsPromotion, defaultPackages, chipsPackagePercentages);

        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue("IOS.chips.package.8000"), is("8000"));
        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue("IOS.chips.package.10000"), is("11000"));

        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue("ANDROID.chips.package.8000"), is("8000"));
        assertThat(buyChipsPromotion.getConfiguration().getConfigurationValue("ANDROID.chips.package.10000"), is("11000"));

        assertNull(buyChipsPromotion.getConfiguration().getConfigurationValue("WEB.chips.package.8000"));

    }

    private Map<Platform, List<ChipPackage>> getDefaultPackages() {
        Map<Platform, List<ChipPackage>> defaultPackages = newLinkedHashMap();

        ChipPackage eightThousandChipPackage = createChipPackage(EIGHT_THOUSAND_CHIPS);
        ChipPackage tenThousandChipPackage = createChipPackage(TEN_THOUSAND_CHIPS);

        List<ChipPackage> chipPackageList = newArrayList(eightThousandChipPackage, tenThousandChipPackage);
        defaultPackages.put(Platform.WEB, chipPackageList);
        defaultPackages.put(Platform.IOS, chipPackageList);
        defaultPackages.put(Platform.ANDROID, chipPackageList);
        defaultPackages.put(Platform.FACEBOOK_CANVAS, chipPackageList);
        return defaultPackages;
    }

    private ChipPackage createChipPackage(BigDecimal noOfChips) {
        ChipPackage eightThousandChipPackage = new ChipPackage();
        eightThousandChipPackage.setDefaultChips(noOfChips);
        return eightThousandChipPackage;
    }

    @Test
    public void sortChipPackagesBasedOnDefaultChipsShouldArrangeChipPackagesInAscendingOrderOfDefaultChips() {

        List<ChipPackage> originalChipsPackageList = newArrayList(createChipPackage(TEN_THOUSAND_CHIPS), createChipPackage(EIGHT_THOUSAND_CHIPS),
                createChipPackage(TWELVE_THOUSAND_CHIPS));

        List<ChipPackage> chipsPackageList = new ArrayList<ChipPackage>(originalChipsPackageList);

        underTest.sortChipPackagesBasedOnDefaultChips(chipsPackageList);

        List<ChipPackage> expectedChipsPackageList = newArrayList(createChipPackage(EIGHT_THOUSAND_CHIPS), createChipPackage(TEN_THOUSAND_CHIPS),
                createChipPackage(TWELVE_THOUSAND_CHIPS));

        assertThat(chipsPackageList, is(expectedChipsPackageList));
        assertThat(chipsPackageList, is(not(originalChipsPackageList)));


    }
}
