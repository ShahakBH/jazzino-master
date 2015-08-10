package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.engagement.EngagementCampaignStatus;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FacebookAppRequestRemovalServiceTest {
    public static final String GAME_TYPE = "BLACKJACK";
    public static final int CAMPAIGN_1_ID = 2;
    public static final String CAMPAIGN_1_REQUEST_ID = "3294827342";
    public static final String CAMPAIGN_1_REQUEST_ID2 = "2345";
    public static final String CAMPAIGN_1_EXTERNAL_ID = "78";
    public static final String CAMPAIGN_1_EXTERNAL_ID2 = "87";
    public static final int CAMPAIGN_2_ID = 67;
    public static final String CAMPAIGN_2_EXTERNAL_ID = "4545";
    public static final String CAMPAIGN_2_REQUEST_ID = "56263727";
    private List<AppRequestExternalReference> externalReferencesForCAMPAIGN_1;

    private FacebookAppRequestRemovalService underTest;

    @Mock
    private FacebookAccessTokenService accessTokenService;
    @Mock
    private EngagementCampaignDao campaignDao;

    @Mock
    QueuePublishingService<FacebookDeleteAppRequestMessage> queuePublishingService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new FacebookAppRequestRemovalService(queuePublishingService, campaignDao);

        externalReferencesForCAMPAIGN_1 = Arrays.asList(
                new AppRequestExternalReference(
                        CAMPAIGN_1_EXTERNAL_ID,
                        GAME_TYPE, CAMPAIGN_1_REQUEST_ID),
                new AppRequestExternalReference(
                        CAMPAIGN_1_EXTERNAL_ID2,
                        GAME_TYPE, CAMPAIGN_1_REQUEST_ID2));
    }

    @Test
    public void whenExpiringACampaignTheStatusShouldBeChangedToExpiringBeforeSendingDeleteRequests() {
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(CAMPAIGN_1_ID));
        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_1_ID)).thenReturn(externalReferencesForCAMPAIGN_1);
        when(campaignDao.updateCampaignStatusToExpiring(CAMPAIGN_1_ID)).thenReturn(true);

        underTest.removeExpiredAppRequestsFromFacebook();

        InOrder inOrder = Mockito.inOrder(campaignDao, queuePublishingService);
        inOrder.verify(campaignDao).updateCampaignStatusToExpiring(CAMPAIGN_1_ID);
        inOrder.verify(queuePublishingService, Mockito.atLeastOnce()).send(
                Mockito.<FacebookDeleteAppRequestMessage>any());
    }

    @Test
    public void whenExpiringACampaignIfTheStatusCannotBeChangedToExpiredThenDeleteRequestsShouldNotBeSent() {
        when(campaignDao.updateCampaignStatusToExpiring(CAMPAIGN_1_ID)).thenReturn(false);
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(CAMPAIGN_1_ID));
        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_1_ID)).thenReturn(externalReferencesForCAMPAIGN_1);

        underTest.removeExpiredAppRequestsFromFacebook();

        verify(campaignDao).updateCampaignStatusToExpiring(CAMPAIGN_1_ID);
        verify(queuePublishingService, never()).send(Mockito.<FacebookDeleteAppRequestMessage>any());
    }

    @Test
    public void removeExpiredAppRequestsShouldPushDeleteRequestsOntoQueue() {
        // given a campaign with targets with sent requests
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(CAMPAIGN_1_ID));
        when(campaignDao.updateCampaignStatusToExpiring(CAMPAIGN_1_ID)).thenReturn(true);
        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_1_ID)).thenReturn(externalReferencesForCAMPAIGN_1);

        underTest.removeExpiredAppRequestsFromFacebook();

        verify(queuePublishingService).send(new FacebookDeleteAppRequestMessage(externalReferencesForCAMPAIGN_1.get(0)));
        verify(queuePublishingService).send(new FacebookDeleteAppRequestMessage(externalReferencesForCAMPAIGN_1.get(1)));
    }

    @Test
    public void removeExpiredAppRequestsShouldPushDeleteRequestsOntoQueueForAllExpiredCampaigns() {
        // given two campaign with sent requests
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(CAMPAIGN_1_ID,
                CAMPAIGN_2_ID));
        when(campaignDao.updateCampaignStatusToExpiring(anyInt())).thenReturn(true);
        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_1_ID)).thenReturn(externalReferencesForCAMPAIGN_1);

        final List<AppRequestExternalReference> campaign2Targets =
                new ArrayList<AppRequestExternalReference>();
        final AppRequestExternalReference externalReferenceCampaign2 = new AppRequestExternalReference(
                CAMPAIGN_2_EXTERNAL_ID,
                GAME_TYPE, CAMPAIGN_2_REQUEST_ID);
        campaign2Targets.add(externalReferenceCampaign2);

        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_2_ID)).thenReturn(campaign2Targets);

        underTest.removeExpiredAppRequestsFromFacebook();

        verify(queuePublishingService).send(new FacebookDeleteAppRequestMessage(externalReferencesForCAMPAIGN_1.get(0)));
        verify(queuePublishingService).send(new FacebookDeleteAppRequestMessage(externalReferencesForCAMPAIGN_1.get(1)));
        verify(queuePublishingService).send(new FacebookDeleteAppRequestMessage(externalReferenceCampaign2));
    }

    @Test
    public void removeExpiredAppRequestsShouldUpdateCampaignStatusAfterPushingAllDeleteRequests() {
        // given a campaign with targets with sent requests
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(CAMPAIGN_1_ID));
        when(campaignDao.updateCampaignStatusToExpiring(CAMPAIGN_1_ID)).thenReturn(true);
        final List<AppRequestExternalReference> campaignTargets =
                new ArrayList<AppRequestExternalReference>();
        when(campaignDao.fetchAppRequestExternalReferences(CAMPAIGN_1_ID)).thenReturn(campaignTargets);

        underTest.removeExpiredAppRequestsFromFacebook();
        verify(campaignDao).updateCampaignStatus(CAMPAIGN_1_ID, EngagementCampaignStatus.EXPIRED);
    }

    @Test
    public void updateAppRequestStatusToExpired() {
        underTest.updateCampaignStatusToExpired(CAMPAIGN_1_ID);

        verify(campaignDao).updateCampaignStatus(CAMPAIGN_1_ID, EngagementCampaignStatus.EXPIRED);
    }
}
