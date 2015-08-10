package com.yazino.engagement.campaign.consumers;

import com.yazino.engagement.campaign.application.CampaignService;
import com.yazino.engagement.campaign.domain.CampaignRunMessage;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CampaignRunConsumerTest {
    private static final long CAMPAIGN_ID = 1l;
    private static final DateTime REPORT_TIME = new DateTime(90000);

    @Mock
    private CampaignService campaignService;

    private CampaignRunConsumer underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        underTest = new CampaignRunConsumer(campaignService);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testHandleShouldNotPropagateWithExceptions() throws Exception {
        when(campaignService.runCampaign(CAMPAIGN_ID, REPORT_TIME)).thenThrow(new RuntimeException("run time exception"));

        underTest.handle(new CampaignRunMessage(CAMPAIGN_ID, REPORT_TIME.toDate()));
    }

    @Test
    public void testHandleShouldSubstituteMissingDatesInLegacyMessagesWithTheCurrentTimeStamp() throws Exception {
        final CampaignRunMessage message = new CampaignRunMessage();
        ReflectionTestUtils.setField(message, "campaignId", CAMPAIGN_ID);

        underTest.handle(message);

        verify(campaignService).runCampaign(CAMPAIGN_ID, new DateTime());
    }
}
