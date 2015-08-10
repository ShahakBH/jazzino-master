package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.ChannelType;
import com.yazino.engagement.PlayerTarget;
import com.yazino.engagement.PushNotificationMessage;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.campaign.domain.MessageContentType.*;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AndroidNotificationCampaignConsumerIntegrationTest {

    @Autowired
    private AndroidNotificationCampaignConsumer underTest;


    /**
     * This will show message in following format in android device
     * (Icon) my message Title
     * my message information
     */
    @Test
    @Ignore
    public void handleMessageShouldSendMessageToUser() {
        //  Make Sure that environment.properties are having following values set from production values (Hint: see Support project)
        //   google-cloud-messaging.api-key=

        // Get PUSH_TOKEN from MOBILE_DEVICE which is to be used as target token in this message
//        String TARGET_TOKEN = "APA91bGiQNwTKaqFBfZt5bRDQHAHKnwY7EB85vSARchxlIZ8MLrtyUGGhivXEYxAWJFzgDRehSNJmqk1iXrtAOk-N2JZUtGWkGuM5A_NrKURS637pXoNsalV9uYjQ_OeRW6nY0UYQo70CXZvMf0FE7yTit8y1ceQQg";
//        String TARGET_TOKEN = "APA91bGpzeT1ydLW80b_0XM68F8ri1cN6A7Z0yE4fbW_iPwvFsgd9CHS4O8HlTs5i7l9FOXpM947uPUeNLtH8CWcF4zowi2SHCWWJUeztknSNy2RJoZZWtpEE12_NbgDpOBjdyuHFqwe6nMMrCqY9ByzmmmzhDinZfZXGFMrnB6T24BVcs_XwDY";
        String TARGET_TOKEN = "APA91bHkKYy1rfD5cAYmuJ_1l7mziX7q50uQHk-5qoHdwZkcOX04qPpNCGFdznALq9_IIvl32OrxJiQ3ECCzJ_9DphGZU1ouCyjNvqYxqOkC6-7gAIB2TG02v_VrSiGd39WqoOrRbSbLWkVdWWSIu2tDO3SKSikCdKdA3VmD21npH4MSc-6l5xs";
//        String TARGET_TOKEN = "APA91bHL5WssTLEvIddxf6Xf0vukHhatnm8cUv0ZQpBzNKR1nt1ppt1xqQ2QOhiP17LtLdEcnBEDA1VfxEQ3wXNmUdi-hnL5xbqLUTKOsqeK_6xhwy0jh7JSz4yXc1JVWXYY23lZZ2_bP-jz-4nL6bPNAiJUyUfwt-tdgkQuKfb9VDACQH0hA_o";
//            targetToken='APA91bHL5WssTLEvIddxf6Xf0vukHhatnm8cUv0ZQpBzNKR1nt1ppt1xqQ2QOhiP17LtLdEcnBEDA1VfxEQ3wXNmUdi-hnL5xbqLUTKOsqeK_6xhwy0jh7JSz4yXc1JVWXYY23lZZ2_bP-jz-4nL6bPNAiJUyUfwt-tdgkQuKfb9VDACQH0hA_o',
        String EXTERNAL_ID = "987654321";
//        BigDecimal PLAYER_ID = new BigDecimal("20670260");
        BigDecimal PLAYER_ID = new BigDecimal("150009636");
        PlayerTarget playerTarget = new PlayerTarget("slots", EXTERNAL_ID, PLAYER_ID, TARGET_TOKEN, "", null);
        Map<String, String> messageContent = newHashMap();
        messageContent.put(TITLE.getKey(), "Yazino Wheel Deal");
        messageContent.put(DESCRIPTION.getKey(), "Jae Rae Desk");
        messageContent.put(MESSAGE.getKey(), "QQg");

        PushNotificationMessage pushNotificationMessage = new PushNotificationMessage(playerTarget, messageContent,
                ChannelType.GOOGLE_CLOUD_MESSAGING_FOR_ANDROID, 179l);

        underTest.handle(pushNotificationMessage);
    }

}
