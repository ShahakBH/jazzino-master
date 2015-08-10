package com.yazino.engagement.campaign.application;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.amazon.AmazonCampaignDeliveryService;
import com.yazino.engagement.amazon.AmazonDeviceMessage;
import com.yazino.engagement.amazon.AmazonDeviceMessagingPublisher;
import com.yazino.engagement.campaign.domain.MessageContentType;
import com.yazino.engagement.mobile.MobileDeviceCampaignDao;
import com.yazino.platform.Platform;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.operations.repository.GameTypeRepository;

import java.math.BigDecimal;
import java.util.HashMap;

import static com.yazino.engagement.ChannelType.AMAZON_DEVICE_MESSAGING;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AmazonCampaignDeliveryServiceTest {
    public static final long CAMPAIGN_RUN_ID = 1l;
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final String TITLE = "title";
    public static final String MESSAGE = "Message";
    AmazonCampaignDeliveryService underTest;
    @Mock
    private MobileDeviceCampaignDao mobileDeviceCampaignDao;
    @Mock
    private GameTypeRepository gameTypeRepository;
    @Mock
    private CampaignContentService campaignContentService;
    @Mock
    private AmazonDeviceMessagingPublisher amazonDeviceMessagingPublisher;


    @Before
    public void setUp() throws Exception {
        underTest = new AmazonCampaignDeliveryService(mobileDeviceCampaignDao, campaignContentService, amazonDeviceMessagingPublisher);
    }

    @Test
    public void getChannelShouldReturnAmazonDeviceMessage() {
        Assert.assertThat(underTest.getChannel(), CoreMatchers.is(IsEqual.equalTo(AMAZON_DEVICE_MESSAGING)));
    }

    @Test
    public void sendMessageToPlayersShouldPublishMessageToQueue() {
        CampaignDeliverMessage campaignDeliverMessage = new CampaignDeliverMessage(CAMPAIGN_RUN_ID, AMAZON_DEVICE_MESSAGING);
        PlayerTarget playerTarget = new PlayerTarget("gameTye", "external id", PLAYER_ID, "target token", "bundle", null);

        when(mobileDeviceCampaignDao.getEligiblePlayerTargets(CAMPAIGN_RUN_ID, Platform.AMAZON,
                                                              campaignDeliverMessage.getFrom(),
                                                              campaignDeliverMessage.getTo())).thenReturn(
                asList(playerTarget)
        );

        HashMap<String, String> content = new HashMap<>();
        content.put(MessageContentType.TITLE.getKey(), TITLE);
        content.put(MessageContentType.MESSAGE.getKey(), MESSAGE);
        when(campaignContentService.getPersonalisedContent(anyMap(), eq(playerTarget))).thenReturn(content);

        underTest.sendMessageToPlayers(campaignDeliverMessage);

        Mockito.verify(amazonDeviceMessagingPublisher).sendPushNotification(
                new AmazonDeviceMessage(PLAYER_ID, "target token",
                        TITLE, TITLE, MESSAGE, "notify", CAMPAIGN_RUN_ID));
    }
}
