package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import com.yazino.engagement.campaign.AccessTokenException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.MESSAGE;
import static com.yazino.engagement.campaign.domain.MessageContentType.TRACKING;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class FacebookNotificationCampaignConsumerIntegrationTest {

    public static final String EMPTY_STRING = "";

    @Autowired
    private FacebookNotificationCampaignConsumer underTest;

    /*
        Make Sure that environment.properties are having following values set from production values (Hint: see Support project)

        facebook.highstakes.apikey=
        facebook.highstakes.secret=
        facebook.highstakes.application.id=
        facebook.highstakes.appName=yazinohighstakes
        facebook.highstakes.redirect=false
        facebook.highstakes.redirectUrl=https://www.yazino.com/fb/highStakes/?ref=FBApp_HighStakesPlay
        facebook.highstakes.canvasActionsAllowed=true

    */

    @Test
    @Ignore
    public void handleShouldSendFbRequestMessageToFacebookSender() throws IOException, AccessTokenException {

        //Get PlayerId and ExternalId from LOBBY_USER table
        String EXTERNAL_ID = "-12345";
        BigDecimal PLAYER_ID = new BigDecimal("-12345");

        PlayerTarget playerTarget = new PlayerTarget("HIGH_STAKES", EXTERNAL_ID, PLAYER_ID, EMPTY_STRING, EMPTY_STRING, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), "my message information");
        messageContent.put(TRACKING.getKey(), "tracking data");

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.FACEBOOK_APP_TO_USER_REQUEST, 76543l);

        underTest.handle(pushNotificationMessage);

    }

    @Test
    @Ignore
    public void handleShouldSendFbNotificationMessageToFacebookSender() throws IOException, AccessTokenException {

        //Get PlayerId and ExternalId from LOBBY_USER table
        String EXTERNAL_ID = "-12345";
        BigDecimal PLAYER_ID = new BigDecimal("-12345");

        PlayerTarget playerTarget = new PlayerTarget("HIGH_STAKES", EXTERNAL_ID, PLAYER_ID, EMPTY_STRING, EMPTY_STRING, null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(MESSAGE.getKey(), "my notification information");
        messageContent.put(TRACKING.getKey(), "tracking data");

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.FACEBOOK_APP_TO_USER_NOTIFICATION, 76543l);

        underTest.handle(pushNotificationMessage);

    }
}
