package com.yazino.engagement.campaign.application;

import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.dao.CampaignRunDao;
import com.yazino.engagement.campaign.dao.SegmentSelectorDao;
import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.engagement.campaign.reporting.application.AuditDeliveryService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CampaignServiceTest {
    private static final DateTime REPORT_TIME = new DateTime(2013, 1, 1, 10, 0, 0, 0);
    private static final long CAMPAIGN_RUN_ID = 2l;
    private static final long CAMPAIGN_ID = 1l;
    private static final String NAME = "This name";
    private static final long PROMO_ID = 435l;

    @Mock
    private CampaignDefinitionDao campaignDefinitionDao;
    @Mock
    private CampaignRunDao campaignRunDao;
    @Mock
    private SegmentSelectorDao segmentSelectorDao;
    @Mock
    private CampaignDeliveryService campaignDeliveryService;
    @Mock
    private AuditDeliveryService auditDeliveryService;
    @Mock
    private PromotionCreationService promotionCreationService;
    @Mock
    private CampaignContentService campaignContentService;
    @Mock
    private CampaignAddTargetDao campaignAddTargetDao;

    private CampaignService underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());
        underTest = new CampaignService(campaignDefinitionDao,
                campaignRunDao,
                segmentSelectorDao,
                campaignDeliveryService,
                auditDeliveryService,
                promotionCreationService,
                campaignContentService,
                campaignAddTargetDao);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void runCampaignShouldPutCampaignDeliverMessageForEachChannel() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);

        // currently does all channels because that's what has been wired into the campaignDefintion
        verify(campaignDeliveryService).deliverCommunications(new CampaignDeliverMessage(CAMPAIGN_RUN_ID, ChannelType.IOS));
        verify(campaignDeliveryService).deliverCommunications(new CampaignDeliverMessage(CAMPAIGN_RUN_ID,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST));
        verify(campaignDeliveryService).deliverCommunications(new CampaignDeliverMessage(CAMPAIGN_RUN_ID,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID));
    }

    private CampaignDefinition createCampaignDefinition(final Boolean hasPromo, final boolean delayNotifications) {
        return new CampaignDefinition(CAMPAIGN_ID,
                NAME,
                "select 1",
                null,
                asList(ChannelType.IOS, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, ChannelType.FACEBOOK_APP_TO_USER_REQUEST),
                hasPromo,
                null, true, delayNotifications);
    }


    @Test
    public void runCampaignShouldCallUpdateCustomDataFields() {
        Map<String, String> contentMap = new HashMap<>();
        contentMap.put(MessageContentType.MESSAGE.getKey(), "come back and receive {PROGRESSIVE}");
        final CampaignDefinition campaignDefinition = new CampaignDefinition(
                CAMPAIGN_ID,
                NAME,
                "select 1",
                contentMap,
                asList(ChannelType.IOS, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, ChannelType.FACEBOOK_APP_TO_USER_REQUEST),
                Boolean.FALSE, null, true, false);


        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(campaignDefinition);
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);

        verify(campaignContentService).updateCustomDataFields(CAMPAIGN_RUN_ID, contentMap);
    }


    @Test
    public void runCampaignShouldTellAuditDeliveryServiceToAuditCampaign() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(auditDeliveryService).auditCampaignRun(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, 1, null, "success", "scheduled time: 2013-01-01T10:00:00.000Z", new DateTime());
    }

    @Test
    public void runCampaignShouldAuditPromoIdForCampaignIfExists() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.TRUE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(promotionCreationService.createPromotionForCampaign(CAMPAIGN_ID, Collections.<BigDecimal>emptyList())).thenReturn(PROMO_ID);
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(auditDeliveryService).auditCampaignRun(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, 1, PROMO_ID, "success", "scheduled time: 2013-01-01T10:00:00.000Z", new DateTime());
    }

    @Test
    public void runShouldReturnCampaignRunId() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        final Long actual = underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        assertThat(actual, is(equalTo(CAMPAIGN_RUN_ID)));
    }

    @Test
    public void runShouldAuditRunWhenSegmentSelectorThrowsError() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenThrow(new RuntimeException(
                "Something happened in Redshift"));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(auditDeliveryService).auditCampaignRun(CAMPAIGN_ID,
                CAMPAIGN_RUN_ID,
                NAME,
                0,
                null,
                "segment selection failed",
                "Something happened in Redshift", new DateTime());
    }

    @Test
    public void runShouldAuditRunWhenSavingTheSegmentThrowsError() {

        final CampaignDefinition campaign = createCampaignDefinition(Boolean.FALSE, false);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        doThrow(new RuntimeException("problem with saving campaign list")).when(campaignRunDao).addPlayers(anyLong(),
                anyListOf(PlayerWithContent.class), anyBoolean());

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(auditDeliveryService).auditCampaignRun(CAMPAIGN_ID,
                CAMPAIGN_RUN_ID,
                NAME,
                1,
                null,
                "Adding players to campaign run failed",
                "problem with saving campaign list", new DateTime());
    }

    @Test
    public void delayedSendCampaignsShouldLeaveValidFromEmpty() {
        CampaignDefinition campaign = new CampaignDefinition(
                CAMPAIGN_ID,
                NAME,
                "select 1",
                null,
                asList(
                        ChannelType.IOS, ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID,
                        ChannelType.FACEBOOK_APP_TO_USER_REQUEST),
                false,
                null,
                true,
                true
        );

        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(campaignRunDao).addPlayers(CAMPAIGN_RUN_ID,playerList(),true);
    }

    @Test
    public void delayedNotificationCampaignsShouldSendNotifications(){
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, true));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);

        verify(campaignDeliveryService,times(0)).deliverCommunications(new CampaignDeliverMessage(CAMPAIGN_RUN_ID,ChannelType.IOS,now().getMillis(),now().getMillis()));

    }


    @Test
    public void runShouldNotCreatePromotionIfCampaignHasNoPromo() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.FALSE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));
        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verifyNoMoreInteractions(promotionCreationService);
    }

    @Test
    public void runShouldCreatePromotionIfCampaignHasPromo() {
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(createCampaignDefinition(Boolean.TRUE, false));
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(promotionCreationService.createPromotionForCampaign(CAMPAIGN_ID, Collections.<BigDecimal>emptyList())).thenReturn(PROMO_ID);
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);
        verify(promotionCreationService).createPromotionForCampaign(CAMPAIGN_ID, Collections.<BigDecimal>emptyList());
        verify(promotionCreationService).addPlayersToPromotionForCampaign(CAMPAIGN_ID, PROMO_ID, new HashSet<>(idList()));
    }

    @Test
    public void campaignTargetsShouldBeAddedToSegmentListIfTheyExist() {
        final CampaignDefinition campaign = createCampaignDefinition(Boolean.FALSE, false);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        final List<PlayerWithContent> campaignTargets = asList(new PlayerWithContent(valueOf(23l)));
        when(campaignAddTargetDao.fetchCampaignTargets(eq(CAMPAIGN_ID), anyBatchVisitor())).thenAnswer(withList(campaignTargets));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);

        verify(campaignRunDao).addPlayers(CAMPAIGN_RUN_ID, playerList(), campaign.delayNotifications());
        verify(campaignRunDao).addPlayers(CAMPAIGN_RUN_ID, campaignTargets, campaign.delayNotifications());
    }

    @Test
    public void campaignTargetsShouldNotBlowUpIfItReceivesAnEmptyListFromFetchCampaignTargets() {
        final CampaignDefinition campaign = createCampaignDefinition(Boolean.FALSE, false);
        when(campaignDefinitionDao.fetchCampaign(CAMPAIGN_ID)).thenReturn(campaign);
        when(campaignRunDao.createCampaignRun(eq(CAMPAIGN_ID), any(DateTime.class))).thenReturn(CAMPAIGN_RUN_ID);
        when(campaignRunDao.getCampaignRun(CAMPAIGN_RUN_ID)).thenReturn(new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime()));
        when(campaignAddTargetDao.fetchCampaignTargets(eq(CAMPAIGN_ID), anyBatchVisitor())).thenAnswer(withList(new ArrayList<PlayerWithContent>()));
        when(segmentSelectorDao.fetchSegment(anyString(), any(DateTime.class), anyBatchVisitor())).thenAnswer(withList(playerList()));

        underTest.runCampaign(CAMPAIGN_ID, REPORT_TIME);

        verify(campaignRunDao).addPlayers(CAMPAIGN_RUN_ID, playerList(), campaign.delayNotifications());
    }

    @Test
    public void runAppToUserGiftingShouldFetchAppToUserCampaignsInLast24Hours() {

//        underTest.runAppToUserGiftingPush(-666L, DateTime.now());

        //getGiftingCampaignRuns <24hours
    }


    @SuppressWarnings("unchecked")
    private BatchVisitor<PlayerWithContent> anyBatchVisitor() {
        return any(BatchVisitor.class);
    }

    private List<PlayerWithContent> playerList() {
        return newArrayList(new PlayerWithContent(BigDecimal.TEN));
    }

    private List<BigDecimal> idList() {
        return newArrayList(BigDecimal.TEN);
    }

    private Answer<Object> withList(final List<PlayerWithContent> list) {
        return new Answer<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                ((BatchVisitor<PlayerWithContent>) invocation.getArguments()[invocation.getArguments().length - 1]).processBatch(list);
                return list.size();
            }
        };
    }

}
