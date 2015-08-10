package com.yazino.engagement.campaign.application;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.dao.CampaignContentDao;
import com.yazino.engagement.campaign.dao.CampaignNotificationDao;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.engagement.campaign.domain.NotificationCustomField;
import com.yazino.engagement.campaign.publishers.PushNotificationPublisher;
import com.yazino.engagement.mobile.MobileDeviceCampaignDao;
import com.yazino.game.api.GameType;
import com.yazino.platform.table.GameTypeInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.operations.repository.GameTypeRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION;
import static com.yazino.engagement.campaign.domain.MessageContentType.MESSAGE;
import static com.yazino.engagement.campaign.domain.MessageContentType.TITLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CampaignDeliveryAdapterTest {

    public static final String GAME_TYPE = "GAME_TYPE";
    public static final String OTHER_GAME_TYPE = "OTHER_GAME_TYPE";
    public static final GameType GAME_TYPE_OBJECT = new GameType(GAME_TYPE, GAME_TYPE, new HashSet<String>());
    public static final GameType OTHER_GAME_TYPE_OBJECT = new GameType(OTHER_GAME_TYPE, OTHER_GAME_TYPE, new HashSet<String>());
    public static final String TARGET_TOKEN = "token";
    public static final String BUNDLE = "bundle";
    public static final HashMap<String, String> EMPTY_CUSTOM_DATA = new HashMap<>();

    @Mock
    private CampaignNotificationDao campaignNotificationDao;

    @Mock
    private MobileDeviceCampaignDao mobileDeviceCampaignDao;

    @Mock
    private CampaignService campaignService;

    @Mock
    private GameTypeRepository gameTypeRepository;

    @Mock
    private PushNotificationPublisher pushNotificationPublisher;

    @Mock
    private CampaignContentService campaignContentService;

    @Mock
    private CampaignContentDao campaignContentDao;

    private CampaignDeliveryAdapter underTest;

    private Long campaignRunId = 1234L;


    @Before
    public void setUp() throws Exception {
        underTest = new CampaignDeliveryAdapter(
                campaignNotificationDao,
                mobileDeviceCampaignDao,
                gameTypeRepository,
                campaignContentService, pushNotificationPublisher,
                FACEBOOK_APP_TO_USER_NOTIFICATION);

        Map<String, GameTypeInformation> gameTypeInformationMap = newHashMap();
        gameTypeInformationMap.put(GAME_TYPE, new GameTypeInformation(GAME_TYPE_OBJECT, true));
        gameTypeInformationMap.put(OTHER_GAME_TYPE, new GameTypeInformation(OTHER_GAME_TYPE_OBJECT, true));
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypeInformationMap);
    }

    @Test
    public void getChannelShouldReturnIOSChannel() {
        assertThat(underTest.getChannel(), is(FACEBOOK_APP_TO_USER_NOTIFICATION));
    }

    @Test
    public void sendMessageToPlayersShouldFindAllEligiblePlayersAndPutIndividualMessagesToQueue() {

        PlayerTarget playerOne = new PlayerTarget(GAME_TYPE, "345678", BigDecimal.ONE, TARGET_TOKEN, BUNDLE, EMPTY_CUSTOM_DATA);
        PlayerTarget playerTwo = new PlayerTarget(GAME_TYPE, "345678", BigDecimal.TEN, TARGET_TOKEN, BUNDLE, EMPTY_CUSTOM_DATA);
        List<PlayerTarget> playerTargetList = newArrayList(playerOne, playerTwo);

        when(campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION)).thenReturn(playerTargetList);
        Map<String, String> contentMap = newHashMap();
        when(campaignContentService.getContent(campaignRunId)).thenReturn(contentMap);
        when(campaignContentService.personaliseContentData(contentMap, EMPTY_CUSTOM_DATA, new GameType("1", GAME_TYPE, null))).thenReturn(contentMap);

        CampaignDeliverMessage message = new CampaignDeliverMessage(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION);
        underTest.sendMessageToPlayers(message);

        verify(pushNotificationPublisher).sendPushNotification(new PushNotificationMessage(playerOne, contentMap, FACEBOOK_APP_TO_USER_NOTIFICATION, campaignRunId));
        verify(pushNotificationPublisher).sendPushNotification(new PushNotificationMessage(playerTwo, contentMap, FACEBOOK_APP_TO_USER_NOTIFICATION, campaignRunId));
    }

    @Test
    public void sendMessageToPlayersShouldBeFilteredOnGameTypeIfNeeded() {

        PlayerTarget playerOne = new PlayerTarget(GAME_TYPE, "345678", BigDecimal.ONE, TARGET_TOKEN, BUNDLE, EMPTY_CUSTOM_DATA);
        PlayerTarget playerTwo = new PlayerTarget("OTHER_GAME_TYPE", "345678", BigDecimal.TEN, TARGET_TOKEN, BUNDLE, EMPTY_CUSTOM_DATA);
        List<PlayerTarget> playerTargetList = newArrayList(playerOne, playerTwo);

        when(campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION)).thenReturn(playerTargetList);
        Map<String, String> contentMap = newHashMap();
        when(campaignContentService.getContent(campaignRunId)).thenReturn(contentMap);
        when(campaignContentService.personaliseContentData(contentMap, EMPTY_CUSTOM_DATA, new GameType("1", GAME_TYPE, null))).thenReturn(contentMap);

        final Map<NotificationChannelConfigType, String> channelConfig = newHashMap();

        final String gamesString = "SOME_OTHER_GAME_TYPE,GAME_TYPE";
        channelConfig.put(NotificationChannelConfigType.GAME_TYPE_FILTER, gamesString);
        when(campaignContentService.getChannelConfig(campaignRunId)).thenReturn(channelConfig);

        CampaignDeliverMessage message = new CampaignDeliverMessage(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION);
        underTest.sendMessageToPlayers(message);

        verify(pushNotificationPublisher).sendPushNotification(new PushNotificationMessage(playerOne, contentMap, FACEBOOK_APP_TO_USER_NOTIFICATION, campaignRunId));
        verifyNoMoreInteractions(pushNotificationPublisher);
    }

    @Test
    public void sendMessageToPlayersShouldHaveCustomDataInPushNotificationMessages() {
        Map<String, String> customData = new HashMap<>();
        customData.put(NotificationCustomField.PROGRESSIVE.name(), "1234");

        PlayerTarget playerOne = new PlayerTarget(GAME_TYPE, "345678", BigDecimal.ONE, TARGET_TOKEN, BUNDLE, customData);

        when(campaignNotificationDao.getEligiblePlayerTargets(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION)).thenReturn(newArrayList(playerOne));

        Map<String, String> contentMap = newHashMap();
        contentMap.put(TITLE.getKey(), "Title blah blah");
        contentMap.put(MESSAGE.getKey(), "Your progressive bonus is {PROGRESSIVE}");

        Map<String, String> modifiedMap = newHashMap(contentMap);
        modifiedMap.put(MESSAGE.getKey(), "Your progressive bonus is 1234");

        when(campaignContentService.getContent(campaignRunId)).thenReturn(contentMap);
        when(campaignContentService.personaliseContentData(contentMap, customData, GAME_TYPE_OBJECT)).thenReturn(modifiedMap);

        CampaignDeliverMessage message = new CampaignDeliverMessage(campaignRunId, FACEBOOK_APP_TO_USER_NOTIFICATION);
        underTest.sendMessageToPlayers(message);

        verify(pushNotificationPublisher).sendPushNotification(new PushNotificationMessage(playerOne, modifiedMap, FACEBOOK_APP_TO_USER_NOTIFICATION, campaignRunId));
    }
}
