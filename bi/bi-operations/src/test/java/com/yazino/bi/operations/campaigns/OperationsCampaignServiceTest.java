package com.yazino.bi.operations.campaigns;

import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.bi.operations.campaigns.controller.CampaignScheduleWithName;
import com.yazino.bi.operations.campaigns.controller.CampaignScheduleWithNameDao;
import com.yazino.bi.operations.campaigns.model.CampaignPlayerUpload;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.platform.Platform;
import com.yazino.promotions.*;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.promotion.PromotionType.*;

@RunWith(MockitoJUnitRunner.class)
public class OperationsCampaignServiceTest {
    private static final String CAMPAIGN_1 = "Campaign 1";
    private static final DateTime NEXT_RUN = new DateTime().plusDays(2);
    private static final DateTime END_TIME = new DateTime().plusMonths(2);
    private static final Long RUN_HOURS = 168l;
    private static final Long RUN_MINUTES = 0l;
    private static final String SQL_QUERY = "SELECT 1";
    private static final List<ChannelType> CHANNELS = asList(ChannelType.IOS);
    private static final Map<String, String> CONTENT_MAP = newHashMap();
    public static final long CAMPAIGN_ID = 1l;
    public static final long PROMOTION_DEFINITION_ID = 2l;
    public static final int TOP_UP_AMOUNT = 2500;
    private static final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();

    private OperationsCampaignService underTest;
    @Mock
    private CampaignDefinitionDao campaignDefinitionDao;
    @Mock
    private CampaignScheduleWithNameDao campaignScheduleWithNameDao;
    @Mock
    private PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao;

    @Mock
    private PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao;

    @Mock
    private PromotionDefinitionDao promotionDefinitionDao;

    @Mock
    private CampaignAddTargetDao campaignAddTargetDao;
    @Mock
    private PromotionFormDefinitionDao<GiftingForm> giftingPromotionDefinitionDao;

    @Before
    public void setUp() throws Exception {
        underTest = new OperationsCampaignService(
                campaignDefinitionDao,
                campaignScheduleWithNameDao,
                buyChipsPromotionDefinitionDao,
                dailyAwardPromotionDefinitionDao,
                promotionDefinitionDao,
                campaignAddTargetDao,
                giftingPromotionDefinitionDao);
    }

    @Test
    public void createShouldSaveCampaignDefinition() {
        when(campaignDefinitionDao.save(new CampaignDefinition(null,
                CAMPAIGN_1,
                SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                FALSE,
                channelConfig, true, false))).thenReturn(CAMPAIGN_ID);

        final Campaign campaignToBeSaved = getDefaultCampaign();
        final BuyChipsForm buyChipsForm = getDefaultBuyChipsForm();

        underTest.save(new CampaignForm(campaignToBeSaved, buyChipsForm));

        verify(campaignDefinitionDao).save(new CampaignDefinition(null,
                CAMPAIGN_1,
                SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                FALSE,
                channelConfig,
                true, false));
        verify(campaignScheduleWithNameDao).save(new CampaignSchedule(CAMPAIGN_ID, NEXT_RUN, RUN_HOURS, RUN_MINUTES, END_TIME));
    }

    @Test
    public void createShouldSaveCampaignSchedule() {
        when(campaignDefinitionDao.save(new CampaignDefinition(null,
                CAMPAIGN_1,
                SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                FALSE,
                channelConfig,
                true, false))).thenReturn(CAMPAIGN_ID);

        final Campaign campaignToBeSaved = getDefaultCampaign();
        final BuyChipsForm buyChipsForm = getDefaultBuyChipsForm();

        underTest.save(new CampaignForm(campaignToBeSaved, buyChipsForm));
        verify(campaignScheduleWithNameDao).save(new CampaignSchedule(CAMPAIGN_ID, NEXT_RUN, RUN_HOURS, RUN_MINUTES, END_TIME));
    }

    @Test
    public void createShouldSaveBuyChipsPromotionIfExists() {
        when(campaignDefinitionDao.save(new CampaignDefinition(null, CAMPAIGN_1, SQL_QUERY, CONTENT_MAP, CHANNELS, TRUE,
                channelConfig, true, false))).thenReturn(CAMPAIGN_ID);

        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.setPromo(TRUE);

        final BuyChipsForm buyChipsFormToBeSaved = getDefaultBuyChipsForm();
        buyChipsFormToBeSaved.setCampaignId(null);

        underTest.save(new CampaignForm(campaignToBeSaved, buyChipsFormToBeSaved));
        final BuyChipsForm expectedBuyChipsForm = getDefaultBuyChipsForm();
        expectedBuyChipsForm.setCampaignId(CAMPAIGN_ID);
        expectedBuyChipsForm.setPaymentMethods(CampaignHelper.getSupportedPaymentMethods(expectedBuyChipsForm.getPlatforms()));
        verify(buyChipsPromotionDefinitionDao).save(expectedBuyChipsForm);
    }

    @Test
    public void createShouldSaveDailyAwardPromotionIfExists() {
        when(campaignDefinitionDao.save(new CampaignDefinition(null, CAMPAIGN_1, SQL_QUERY, CONTENT_MAP, CHANNELS, TRUE,
                channelConfig, true, false))).thenReturn(CAMPAIGN_ID);

        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.setPromo(TRUE);

        final DailyAwardForm dailyAwardFormToBeSaved = getDefaultDailyAwardForm();

        underTest.save(new CampaignForm(campaignToBeSaved, dailyAwardFormToBeSaved));

        final DailyAwardForm expectedDailyAwardForm = getDefaultDailyAwardForm();
        expectedDailyAwardForm.setCampaignId(CAMPAIGN_ID);
        verify(dailyAwardPromotionDefinitionDao).save(expectedDailyAwardForm);
    }

    private DailyAwardForm getDefaultDailyAwardForm() {
        final DailyAwardForm dailyAwardForm = new DailyAwardForm();
        dailyAwardForm.setTopUpAmount(TOP_UP_AMOUNT);
        dailyAwardForm.setValidForHours(24);
        dailyAwardForm.setName(CAMPAIGN_1);
        dailyAwardForm.setPlatforms(asList(Platform.WEB, Platform.IOS, Platform.FACEBOOK_CANVAS, Platform.ANDROID));
        return dailyAwardForm;
    }

    private GiftingForm getDefaultGiftingForm() {
        final GiftingForm giftingForm = new GiftingForm();
        giftingForm.setValidForHours(24);
        giftingForm.setName(CAMPAIGN_1);
        giftingForm.setDescription("GIFT DESCRIPTION");
        giftingForm.setTitle("GIFT TITLE");
        giftingForm.setReward(123343l);
        giftingForm.setGameType("SLOTS");
        giftingForm.setAllPlayers(true);
        giftingForm.setValidForHours(5);
        return giftingForm;
    }

    @Test
    public void updateShouldUpdateCampaignDefinition() {
        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);
        final BuyChipsForm defaultBuyChipsForm = getDefaultBuyChipsForm();

        underTest.update(new CampaignForm(campaignToBeSaved, defaultBuyChipsForm));

        verify(campaignDefinitionDao).update(new CampaignDefinition(CAMPAIGN_ID,
                CAMPAIGN_1,
                SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                FALSE,
                channelConfig,
                true, false));
    }

    @Test
    public void updateShouldUpdateCampaignSchedule() {
        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);
        final BuyChipsForm defaultBuyChipsForm = getDefaultBuyChipsForm();

        underTest.update(new CampaignForm(campaignToBeSaved, defaultBuyChipsForm));

        verify(campaignScheduleWithNameDao).update(new CampaignSchedule(CAMPAIGN_ID, NEXT_RUN, RUN_HOURS, RUN_MINUTES, END_TIME));
    }

    @Test
    public void updateShouldUpdatePromotionBuyChipsPromotionIfExists() {
        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.setPromo(TRUE);
        campaignToBeSaved.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);
        final BuyChipsForm defaultBuyChipsForm = getDefaultBuyChipsForm();
        defaultBuyChipsForm.setPromotionDefinitionId(PROMOTION_DEFINITION_ID);
        defaultBuyChipsForm.setCampaignId(CAMPAIGN_ID);


        underTest.update(new CampaignForm(campaignToBeSaved, defaultBuyChipsForm));

        verify(buyChipsPromotionDefinitionDao).update(defaultBuyChipsForm);
        verifyNoMoreInteractions(dailyAwardPromotionDefinitionDao);
    }

    @Test
    public void updateShouldUpdateDailyAwardPromotionIfExists() {

        final Campaign campaignToBeSaved = getDefaultCampaign();
        campaignToBeSaved.setPromo(TRUE);
        campaignToBeSaved.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);

        final DailyAwardForm intialDailyAwardForm = getDefaultDailyAwardForm();
        intialDailyAwardForm.setPromotionDefinitionId(PROMOTION_DEFINITION_ID);
        intialDailyAwardForm.setCampaignId(CAMPAIGN_ID);
        intialDailyAwardForm.setName(null);

        final DailyAwardForm expectedDailyAwardForm = getDefaultDailyAwardForm();
        expectedDailyAwardForm.setPromotionDefinitionId(PROMOTION_DEFINITION_ID);
        expectedDailyAwardForm.setCampaignId(CAMPAIGN_ID);
        expectedDailyAwardForm.setName(CAMPAIGN_1);

        underTest.update(new CampaignForm(campaignToBeSaved, intialDailyAwardForm));
        verify(dailyAwardPromotionDefinitionDao).update(expectedDailyAwardForm);
        verifyNoMoreInteractions(buyChipsPromotionDefinitionDao);
    }

    @Test(expected = RuntimeException.class)
    public void saveShouldThrowExceptionAtDelayedNotCampWithShortRuntime(){
        final Campaign defaultCampaign = getDefaultCampaign();
        defaultCampaign.setDelayNotifications(true);
        defaultCampaign.getCampaignScheduleWithName().setRunHours(3L);
        underTest.save(new CampaignForm(defaultCampaign, null));
    }

    @Test
    public void getCampaignFormShouldReturnCampaignFormWithNoPromotions() {
        final Campaign campaign = getDefaultCampaign();
        campaign.setPromo(FALSE);
        campaign.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);

        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(
                new CampaignDefinition(
                        CAMPAIGN_ID,
                        CAMPAIGN_1,
                        SQL_QUERY,
                        CONTENT_MAP,
                        CHANNELS,
                        FALSE,
                        channelConfig,
                        true,
                        false)
        );
        when(campaignScheduleWithNameDao.getCampaignSchedule(CAMPAIGN_ID)).thenReturn(new CampaignSchedule(CAMPAIGN_ID,
                NEXT_RUN,
                RUN_HOURS,
                RUN_MINUTES, END_TIME));
        assertThat(underTest.getCampaignForm(CAMPAIGN_ID), equalTo(new CampaignForm(campaign, null)));
        verifyZeroInteractions(buyChipsPromotionDefinitionDao, dailyAwardPromotionDefinitionDao);
    }


    @Test
    public void getCampaignFormShouldReturnCampaignFormWithBuyChipPromotion() {
        final Campaign campaign = getDefaultCampaign();
        campaign.setPromo(TRUE);
        campaign.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);

        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(BUY_CHIPS);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(
                getDefaultCampaignDefinition());
        when(campaignScheduleWithNameDao.getCampaignSchedule(CAMPAIGN_ID)).thenReturn(new CampaignSchedule(CAMPAIGN_ID,
                NEXT_RUN,
                RUN_HOURS,
                RUN_MINUTES, END_TIME));
        when(buyChipsPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(getDefaultBuyChipsForm());


        assertThat(underTest.getCampaignForm(CAMPAIGN_ID),
                equalTo(new CampaignForm(campaign, getDefaultBuyChipsForm())));
        verifyZeroInteractions(dailyAwardPromotionDefinitionDao);
    }

    @Test
    public void getCampaignFormShouldReturnCampaignFormWithDailyAwardPromotion() {
        final Campaign campaign = getDefaultCampaign();
        campaign.setPromo(TRUE);
        campaign.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);

        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(DAILY_AWARD);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(getDefaultCampaignDefinition());
        when(campaignScheduleWithNameDao.getCampaignSchedule(CAMPAIGN_ID)).thenReturn(new CampaignSchedule(CAMPAIGN_ID,
                NEXT_RUN,
                RUN_HOURS,
                RUN_MINUTES, END_TIME));
        when(dailyAwardPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(getDefaultDailyAwardForm());

        assertThat(underTest.getCampaignForm(CAMPAIGN_ID),
                equalTo(new CampaignForm(campaign, getDefaultDailyAwardForm())));
        verifyZeroInteractions(buyChipsPromotionDefinitionDao);
    }

    @Test
    public void getCampaignFormShouldReturnCampaignFormWithGiftingPromotion() {
        final Campaign campaign = getDefaultCampaign();
        campaign.setPromo(TRUE);
        campaign.getCampaignScheduleWithName().setCampaignId(CAMPAIGN_ID);

        when(promotionDefinitionDao.getPromotionDefinitionType(CAMPAIGN_ID)).thenReturn(GIFTING);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(getDefaultCampaignDefinition());
        when(campaignScheduleWithNameDao.getCampaignSchedule(CAMPAIGN_ID)).thenReturn(new CampaignSchedule(CAMPAIGN_ID,
                NEXT_RUN,
                RUN_HOURS,
                RUN_MINUTES, END_TIME));
        when(giftingPromotionDefinitionDao.getForm(CAMPAIGN_ID)).thenReturn(getDefaultGiftingForm());

        final GiftingForm defaultGiftingForm = getDefaultGiftingForm();
        defaultGiftingForm.setCampaignId(CAMPAIGN_ID);
        assertThat(underTest.getCampaignForm(CAMPAIGN_ID),
                equalTo(new CampaignForm(campaign, defaultGiftingForm)));
        verifyZeroInteractions(buyChipsPromotionDefinitionDao);
        verifyZeroInteractions(dailyAwardPromotionDefinitionDao);
    }

    @Test
    public void disableShouldCallDisableOnTheDao() {
        underTest.disable(123L);
        verify(campaignDefinitionDao).setEnabledStatus(123L, false);
    }

    private CampaignDefinition getDefaultCampaignDefinition() {
        return new CampaignDefinition(
                CAMPAIGN_ID,
                CAMPAIGN_1,
                SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                TRUE, channelConfig, true, false);
    }

    @Test
    public void getCampaignListShouldReturnListOfAllEnabledCampaigns() {
        final CampaignScheduleWithName expected = new CampaignScheduleWithName(CAMPAIGN_ID, CAMPAIGN_1, NEXT_RUN, END_TIME, RUN_HOURS, RUN_MINUTES);
        when(campaignScheduleWithNameDao.getCampaignList(false))
                .thenReturn(asList(expected));

        assertThat(underTest.getCampaignScheduleWithNameList(), hasItem(expected));
    }

    @Test
    public void addPlayersToCampaignShouldHandleValidCsvListOfPlayerIds() throws IOException {
        final MockMultipartFile multipartFile = new MockMultipartFile("campaigns.csv", "123456\n654321".getBytes());
        final CampaignPlayerUpload campaignPlayerUpload = new CampaignPlayerUpload();
        campaignPlayerUpload.setCampaignId(CAMPAIGN_ID);
        campaignPlayerUpload.setFile(multipartFile);

        underTest.addPlayersToCampaign(campaignPlayerUpload);

        final LinkedHashSet<BigDecimal> expectedPlayerIds = new LinkedHashSet<>();
        expectedPlayerIds.add(new BigDecimal(123456l));
        expectedPlayerIds.add(new BigDecimal(654321l));

        verify(campaignAddTargetDao).savePlayersToCampaign(CAMPAIGN_ID, expectedPlayerIds);
    }

    @Test
    public void addPlayersToCampaignShouldIgnoreMalformedPlayerId() throws IOException {
        final MockMultipartFile multipartFile = new MockMultipartFile("campaigns.csv", "12332a1\n654321".getBytes());
        final CampaignPlayerUpload campaignPlayerUpload = new CampaignPlayerUpload();
        campaignPlayerUpload.setCampaignId(CAMPAIGN_ID);
        campaignPlayerUpload.setFile(multipartFile);

        underTest.addPlayersToCampaign(campaignPlayerUpload);

        final LinkedHashSet<BigDecimal> expectedPlayerIds = new LinkedHashSet<>();
        expectedPlayerIds.add(new BigDecimal(654321l));

        verify(campaignAddTargetDao).savePlayersToCampaign(CAMPAIGN_ID, expectedPlayerIds);
    }

    @Test
    public void addPlayersToCampaignShouldIgnoreDuplicates() throws IOException {
        final MockMultipartFile multipartFile = new MockMultipartFile("campaigns.csv", "123456\n654321\n123456".getBytes());
        final CampaignPlayerUpload campaignPlayerUpload = new CampaignPlayerUpload();
        campaignPlayerUpload.setCampaignId(CAMPAIGN_ID);
        campaignPlayerUpload.setFile(multipartFile);

        underTest.addPlayersToCampaign(campaignPlayerUpload);

        final LinkedHashSet<BigDecimal> expectedPlayerIds = new LinkedHashSet<>();
        expectedPlayerIds.add(new BigDecimal(123456l));
        expectedPlayerIds.add(new BigDecimal(654321l));

        verify(campaignAddTargetDao).savePlayersToCampaign(CAMPAIGN_ID, expectedPlayerIds);
    }


    // write test to  deal with duplicates

    private BuyChipsForm getDefaultBuyChipsForm() {
        BuyChipsForm buyChipsForm = new BuyChipsForm();
        buyChipsForm.setInGameNotificationHeader("in game blah");
        buyChipsForm.setMaxRewards(1);
        buyChipsForm.setInGameNotificationMsg("in game message");
        buyChipsForm.setValidForHours(24);
        buyChipsForm.setName(CAMPAIGN_1);
        buyChipsForm.setPlatforms(asList(Platform.WEB, Platform.IOS, Platform.FACEBOOK_CANVAS, Platform.ANDROID));
        return buyChipsForm;
    }

    private Campaign getDefaultCampaign() {
        return new Campaign(
                null,
                CAMPAIGN_1,
                NEXT_RUN,
                END_TIME,
                RUN_HOURS,
                RUN_MINUTES, SQL_QUERY,
                CONTENT_MAP,
                CHANNELS,
                FALSE,
                channelConfig,
                false
        );
    }

}
