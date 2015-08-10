package com.yazino.engagement.campaign.reporting;

import com.yazino.engagement.campaign.reporting.domain.CampaignRunAuditMessage;
import com.yazino.engagement.campaign.reporting.consumers.CampaignAuditConsumer;
import com.yazino.engagement.campaign.reporting.dao.CampaignAuditDao;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CampaignAuditConsumerTest {

    public static final long CAMPAIGN_RUN_ID = 1l;
    public static final long CAMPAIGN_ID = 2l;
    public static final String NAME = "name";
    public static final int SIZE = 20;
    public static final DateTime TIMESTAMP = new DateTime();
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final long PROMO_ID = 345l;
    @Mock
    CampaignAuditDao campaignAuditDao;

    CampaignAuditConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignAuditConsumer(campaignAuditDao);

    }

    @Test
    public void handleShouldAllExceptions() throws Exception {
        doThrow(new RuntimeException()).when(campaignAuditDao).persistCampaignRun(
                Matchers.anyLong(),
                Matchers.anyLong(),
                Matchers.anyString(),
                Matchers.anyInt(),
                Matchers.any(DateTime.class),
                Matchers.anyString(),
                Matchers.anyString(),
                Matchers.anyLong());

        underTest.handle(new CampaignRunAuditMessage(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, SIZE, TIMESTAMP, PROMO_ID, STATUS, MESSAGE));
    }

    @Test
    public void handleShouldCallCampaignAuditDaoWhenMessageIsACampaignRunMessage(){
        underTest.handle(new CampaignRunAuditMessage(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, SIZE, TIMESTAMP, PROMO_ID, STATUS, MESSAGE));
        verify(campaignAuditDao).persistCampaignRun(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, SIZE, TIMESTAMP, STATUS, MESSAGE, PROMO_ID);
    }
}
