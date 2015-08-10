package com.yazino.engagement.campaign.reporting.application;

import com.yazino.engagement.campaign.reporting.domain.CampaignAuditMessage;
import com.yazino.engagement.campaign.reporting.domain.CampaignRunAuditMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AuditDeliveryServiceImplTest {

    public static final long CAMPAIGN_ID = 1l;
    public static final long CAMPAIGN_RUN_ID = 2l;
    public static final String NAME = "name";
    public static final int SIZE = 20;
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final long PROMO_ID = 345l;
    AuditDeliveryService underTest;

    @Mock
    QueuePublishingService<CampaignAuditMessage> campaignAuditDeliveryQueue;


    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
        underTest = new AuditDeliveryServiceImpl(campaignAuditDeliveryQueue);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void auditCampaignRunShouldPutMessageOnTheQueue() {
        underTest.auditCampaignRun(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, SIZE, PROMO_ID, STATUS, MESSAGE, new DateTime());

        verify(campaignAuditDeliveryQueue).send(
                new CampaignRunAuditMessage(
                        CAMPAIGN_ID,
                        CAMPAIGN_RUN_ID,
                        NAME,
                        SIZE,
                        new DateTime(),
                        PROMO_ID,
                        STATUS,
                        MESSAGE));
    }
}
