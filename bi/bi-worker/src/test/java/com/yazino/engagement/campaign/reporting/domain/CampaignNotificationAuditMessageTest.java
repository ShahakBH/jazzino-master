package com.yazino.engagement.campaign.reporting.domain;

import com.yazino.engagement.ChannelType;
import com.yazino.yaps.JsonHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CampaignNotificationAuditMessageTest {

    public static final DateTime CURRENT_DATE_TIME = new DateTime(DateTimeZone.UTC);

    private JsonHelper jsonHelper = new JsonHelper();

    @Test
    public void CampaignNotificationAuditMessageShouldSerializeAndDeSerialize() {
        CampaignNotificationAuditMessage message = new CampaignNotificationAuditMessage(123L, BigDecimal.TEN, ChannelType.IOS, "SLOTS",
                NotificationAuditType.SEND_ATTEMPT, CURRENT_DATE_TIME);

        String serializedString = jsonHelper.serialize(message);

        CampaignNotificationAuditMessage deSerializedMessage = jsonHelper.deserialize(CampaignNotificationAuditMessage.class, serializedString);

        assertThat(deSerializedMessage, is(message));

    }

}
