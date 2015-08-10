package com.yazino.engagement.campaign.integration;

import com.google.common.collect.ImmutableMap;
import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.application.*;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.campaign.publishers.AndroidPushNotificationPublisher;
import com.yazino.engagement.campaign.reporting.application.AuditDeliveryService;
import com.yazino.engagement.mobile.MobileDeviceDao;
import com.yazino.platform.gifting.AppToUserGift;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.promotions.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.promotion.GiftingPromotionService;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;
import utils.PlayerBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static utils.ParamBuilder.params;
import static utils.PlayerBuilder.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@Ignore
//TO get this working, you need to add a dependancy to bi-promotions and change the remotingGiftingPromoService to autowired.
public class AppToUserGiftingIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AppToUserGiftingIntegrationTest.class);

    //    public static final String APP_TO_USER_PUSH = "INSERT INTO CAMPAIGN_DEFINITION (id,name, segmentSqlQuery, hasPromo, enabled) " +
//            "VALUES (-666,'GIFTING PUSH','select PLAYER_ID from LOBBY_USER',true,true)";
    public static final long REWARD = 999L;
    public static final String TITLE = "I want";
    public static final String DESCRIPTION = "your mum";
    CampaignService campaignService;

    @Autowired
    private CampaignDefinitionDao campaignDefinitionDao;
    @Autowired
    private CampaignRunDao campaignRunDao;
    @Autowired
    private SegmentSelectorDao segmentSelectorDao;

    @Mock
    private CampaignDeliveryService campaignDeliveryService;
    @Mock
    private AuditDeliveryService auditDeliveryService;

    private PromotionCreationService promotionCreationService;
    //
    @Mock
    private CampaignContentService campaignContentService;
    @Mock
    private CampaignAddTargetDao campaignAddTargetDao;

    @Autowired
    NamedParameterJdbcTemplate dwNamedJdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;


    @Autowired()
    @Qualifier("marketingJdbcTemplate")
    JdbcTemplate strataprodJdbcTemplate;

    @Mock
    private PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao;

    @Autowired
    private PromotionDao promotionDao;

    @Mock
    private PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;

    @Autowired
    private GiftingPromotionDefinitionDao giftingPromotionDefinitionDao;

    @Autowired
    private PromotionDefinitionDao promotionDefinitionDao;

    @Mock
    private PromotionFormDefinitionDao dailyAwardPromotionDefinitionDao;

    private Map<String, Object> emptyParams = ImmutableMap.of();

    @Autowired //Autowire this if you need it. you need to change the pom to include the bi-promotions
    private GiftingPromotionService remotingGiftingPromotionService;

    @Autowired
    private DelayedCampaignRunner delayedCampaignRunner;

    @Autowired
    private QueuePublishingService<CampaignDeliverMessage> campaignDeliverMessageQueuePublishingService;

    @Autowired
    private AndroidPushNotificationPublisher androidPushNotificationPublisher;
    private DateTime realNow;

    @Before
    public void setUp() throws Exception {
        realNow = now();
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(realNow.getMillis());
        MockitoAnnotations.initMocks(this);
        cleanDatabaseTables();
        PlayerBuilder.initialise(externalDwNamedJdbcTemplate);
        createSomePlayers();

        promotionCreationService = mockPromoService();
        campaignService = new CampaignService(
                campaignDefinitionDao,
                campaignRunDao,
                segmentSelectorDao,
                campaignDeliveryService,
                auditDeliveryService,
                promotionCreationService,
                campaignContentService,
                campaignAddTargetDao);

    }

    private void createSomePlayers() {
        PlayerBuilder.createPlayer(ANDY).withAnAndroid().whoLoggedIn(now().minusHours(24)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(BOB).withAnAndroid().whoLoggedIn(now().minusHours(23)).storeIn(
                externalDwNamedJdbcTemplate);
        PlayerBuilder.createPlayer(CHAZ).withAnAndroid().whoLoggedIn(now().minusHours(48)).storeIn(
                externalDwNamedJdbcTemplate);
    }

    @Test
    public void runGiftingCampaignShouldCreatePromotionAndSegmentSelection() throws InterruptedException {
        setupCampDelSrvToExplodeOnTryingToSendPushesForGifting();
        final Long giftingCampaignId = createGiftingCampaignDef("select 2 as player_id from lobby_user", false);

        assertThatPromotionWasCreatedForCampaign(giftingCampaignId);
        Thread.sleep(500l);

        final List<AppToUserGift> andyPromotions = remotingGiftingPromotionService.getGiftingPromotions(
                BigDecimal.valueOf(ANDY));
        assertThat(andyPromotions.size(), is(0));

        validateBobGotHisGift();
    }

    @Test
    public void runGiftingCampaignShouldCreatePromotionThatSelectsAllAndSegmentSelection() throws InterruptedException {
        setupCampDelSrvToExplodeOnTryingToSendPushesForGifting();
        final Long giftingCampaignId = createGiftingCampaignDef("select player_id from lobby_user", false);
        final Long campaignRunId = campaignService.runCampaign(giftingCampaignId, now());

        assertThatPromotionWasCreatedForCampaign(giftingCampaignId);
        verifySegmentsCreated(campaignRunId);
        Thread.sleep(500l);

        validateBobGotHisGift();
    }

    @Test
    public void runGiftingCampaignShouldCreateAllPlayerPromotionAndSetupPromo() throws InterruptedException {
        setupCampDelSrvToExplodeOnTryingToSendPushesForGifting();
        final Long giftingCampaignId = createGiftingCampaignDef("select 2 from lobby_user", true);
        final Long campaignRunId = campaignService.runCampaign(giftingCampaignId, now());

        assertThatPromotionWasCreatedForCampaign(giftingCampaignId);
        Thread.sleep(500l);

        validateBobGotHisGift();
    }

    @Test
    public void logPlayerRewardShouldChangeAvailabilityOfGift() throws InterruptedException {
        setupCampDelSrvToExplodeOnTryingToSendPushesForGifting();
        final Long giftingCampaignId = createGiftingCampaignDef("select 2 from lobby_user", true);
        final Long campaignRunId = campaignService.runCampaign(giftingCampaignId, now());
        Thread.sleep(500l);

        final List<AppToUserGift> giftingPromotions = remotingGiftingPromotionService.getGiftingPromotions(
                BigDecimal.valueOf(BOB));
        assertThat(giftingPromotions.size(), is(1));
        final AppToUserGift appToUserGift = giftingPromotions.get(0);


        remotingGiftingPromotionService.logPlayerReward(BigDecimal.valueOf(BOB), appToUserGift.getPromoId(), null);
        final List<AppToUserGift> updatedGiftingPromotions = remotingGiftingPromotionService.getGiftingPromotions(
                BigDecimal.valueOf(BOB));
        assertThat(updatedGiftingPromotions.size(), is(0));


    }

    private void validateBobGotHisGift() {
        final List<AppToUserGift> giftingPromotions = remotingGiftingPromotionService.getGiftingPromotions(
                BigDecimal.valueOf(BOB));
        assertThat(giftingPromotions.size(), is(1));
        final AppToUserGift appToUserGift = giftingPromotions.get(0);
        assertThat(appToUserGift.getAmount(), equalTo(REWARD));
        assertThat(appToUserGift.getTitle(), equalTo(TITLE));
        assertThat(appToUserGift.getDescription(), equalTo(DESCRIPTION));
        assertThat(appToUserGift.getPromoId(), notNullValue());
    }

    @Test
//    @Ignore
    public void runningTheAppToUserGiftingCampaignShouldSendAllThePushNotificationMessages() throws InterruptedException {
        //createGiftSendingCampaignDef();
//        createSomePlayers();

        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
        final Long giftingCampaignId = createGiftingCampaignDef("select player_id from lobby_user", true);
        final DateTime now = now().withMillisOfSecond(0);
        LOG.debug("now is {}", now);
        final Long campaignRunId = campaignService.runCampaign(giftingCampaignId, now);

        Thread.sleep(500l);
        final List<Map<String, Object>> segments = getSegmentsForCampaignRun(campaignRunId);
        assertThat(segments.size(), is(3));
        assertThat(segments.get(0).get("valid_from"), nullValue());
        assertThat(segments.get(1).get("valid_from"), nullValue());
        assertThat(segments.get(2).get("valid_from"), nullValue());

        verifyZeroInteractions(androidPushNotificationPublisher);
        delayedCampaignRunner.reRun();

        List<Map<String, Object>> updatedSegments = getSegmentsForCampaignRun(campaignRunId);
        assertThat(updatedSegments.get(0).get("valid_from"), notNullValue());
        assertThat(updatedSegments.get(1).get("valid_from"), nullValue());
        assertThat(updatedSegments.get(2).get("valid_from"), nullValue());

//        LOG.debug("lets pretend we go forward a day");
//        ThreadLocalDateTimeUtils.setCurrentMillisFixed(realNow.plusDays(1).getMillis());
//        delayedCampaignRunner.reRun();
//        updatedSegments = getSegmentsForCampaignRun(campaignRunId);
//        assertThat(updatedSegments.get(0).get("valid_from"), notNullValue());
//        assertThat(updatedSegments.get(1).get("valid_from"), nullValue());
//        assertThat(updatedSegments.get(2).get("valid_from"), nullValue());
//        Thread.sleep(1000L);
        Thread.sleep(500l);
        verify(androidPushNotificationPublisher).sendPushNotification(Mockito.any(PushNotificationMessage.class));
        Thread.sleep(500L);

        //        verify(campaignDeliveryService).deliverCommunications(new Cam);
//        campaignService.runAppToUserGiftingPush(-666L, now());

        //so it should select the people that we want, load up the config that we want
        //is this gonna be easy to just hardcode into the campaign runner? probably.
        //otherwise we're gonna have to do some weeeeird shit.
//        verify(campaignDeliveryService).deliverCommunications();

    }

    private List<Map<String, Object>> getSegmentsForCampaignRun(final Long campaignRunId) {
        return externalDwNamedJdbcTemplate.queryForList(
                "select * from segment_selection where campaign_run_id=:campaignRunId order by player_id",
                params().campaignRunId(campaignRunId));
    }

    private void setupCampDelSrvToExplodeOnTryingToSendPushesForGifting() {
        doThrow(new RuntimeException("Yikes!")).when(campaignDeliveryService).deliverCommunications(
                new CampaignDeliverMessage(666l, ChannelType.IOS));
        doThrow(new RuntimeException("Yikes!")).when(campaignDeliveryService).deliverCommunications(
                new CampaignDeliverMessage(666l, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID));
    }

    private void verifySegmentsCreated(final Long campaignRunId) {
        final List<Map<String, Object>> segments = getSegmentsForCampaignRun(campaignRunId);
        assertThat(segments.size(), is(3));
        assertThat(((BigDecimal) segments.get(0).get("player_id")).intValue(), is(ANDY));
        assertThat(((BigDecimal) segments.get(1).get("player_id")).intValue(), is(BOB));
        assertThat(((BigDecimal) segments.get(2).get("player_id")).intValue(), is(CHAZ));
    }

    private void assertThatPromotionWasCreatedForCampaign(final Long campaignId) {
        final Map<String, Object> promo = dwNamedJdbcTemplate.queryForMap(
                "select id,name from PROMOTION_DEFINITION where CAMPAIGN_ID=:campaignId",
                params().campaignId(campaignId));
        LOG.info("promoId {} for campaign {}", promo.get("id"), campaignId);
        final List<Map<String, Object>> promoDef = strataprodJdbcTemplate.queryForList(
                "select * from PROMOTION where name= ?", promo.get("name"));
        assertThat(promoDef.size(), equalTo(1));
//        assertThat((String) promoDef.get("PROMOTION_TYPE"), is(equalTo("GIFTING")));
    }


    private void cleanDatabaseTables() {
        dwNamedJdbcTemplate.update("delete from CAMPAIGN_RUN", emptyParams);
        dwNamedJdbcTemplate.update("delete from CAMPAIGN_CHANNEL", emptyParams);
        dwNamedJdbcTemplate.update("delete from PROMOTION_DEFINITION", emptyParams);
        dwNamedJdbcTemplate.update("delete from CAMPAIGN_DEFINITION where id<>1337", emptyParams);
        dwNamedJdbcTemplate.update("delete from PROMOTION_DEFINITION", emptyParams);
        strataprodJdbcTemplate.update("delete from PROMOTION_CONFIG where promo_id>10");
        strataprodJdbcTemplate.update("delete from PROMOTION_PLAYER_REWARD where promo_id>10");
        strataprodJdbcTemplate.update("delete from PROMOTION_PLAYER where promo_id>10");
        strataprodJdbcTemplate.update("delete from PROMOTION where promo_id>10");
        externalDwNamedJdbcTemplate.update("delete from segment_selection", emptyParams);
    }

    private PromotionCreationService mockPromoService() {
        return new PromotionCreationService(
                promotionDao,
                buyChipsPromotionDefinitionDao,
                dailyAwardPromotionDefinitionDao,
                paymentOptionsToChipPackageTransformer,
                promotionDefinitionDao, giftingPromotionDefinitionDao);
    }


    private Long createGiftingCampaignDef(final String segmentSelectionQuery, final boolean allPlayers) {
        final Map<String, String> content = newHashMap();
        final List<ChannelType> channels = newArrayList();
        channels.add(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();
        final CampaignDefinition campDef = new CampaignDefinition(
                null, "giftingTest",
                segmentSelectionQuery, content, channels, true, channelConfig, true, true);
        Long campId = campaignDefinitionDao.save(campDef);
        giftingPromotionDefinitionDao.save(createGiftingForm(campId, allPlayers));

        return campId;
    }

    private GiftingForm createGiftingForm(final Long campId, final boolean allPlayers) {
        final GiftingForm giftingForm = new GiftingForm();
        giftingForm.setAllPlayers(allPlayers);
        giftingForm.setDescription(DESCRIPTION);
        giftingForm.setTitle(TITLE);
        giftingForm.setReward(REWARD);
        giftingForm.setName("HARRO");
        giftingForm.setGameType("SLOTS");
        giftingForm.setPriority(99);
        giftingForm.setValidForHours(24);
        giftingForm.setCampaignId(campId);
        return giftingForm;
    }

}
