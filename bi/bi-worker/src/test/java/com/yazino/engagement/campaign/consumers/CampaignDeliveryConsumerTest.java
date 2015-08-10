package com.yazino.engagement.campaign.consumers;

import com.google.common.collect.Lists;
import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.campaign.application.ChannelCampaignDeliveryAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


public class CampaignDeliveryConsumerTest {
    public static final long CAMPAIGN_RUN_ID = 1l;
    private CampaignDeliveryConsumer underTest;
    @Mock
    private ChannelCampaignDeliveryAdapter googleCampaignDeliveryAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHandleShouldOnlyProcessForAdaptersItHas() throws Exception {
        Mockito.when(googleCampaignDeliveryAdapter.getChannel()).thenReturn(ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        underTest = new CampaignDeliveryConsumer(Lists.newArrayList(googleCampaignDeliveryAdapter));

        final CampaignDeliverMessage expectedCampaignDeliverMessageForAndroid = new CampaignDeliverMessage(CAMPAIGN_RUN_ID,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID);
        underTest.handle(expectedCampaignDeliverMessageForAndroid);
        underTest.handle(new CampaignDeliverMessage(1l, ChannelType.IOS));
        underTest.handle(new CampaignDeliverMessage(1l, ChannelType.FACEBOOK_APP_TO_USER_REQUEST));

        Mockito.verify(googleCampaignDeliveryAdapter).sendMessageToPlayers(expectedCampaignDeliverMessageForAndroid);
    }
}
