package com.yazino.engagement.campaign;

import com.google.common.collect.Lists;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.application.CampaignRunService;
import com.yazino.engagement.campaign.application.CampaignScheduler;
import com.yazino.bi.campaign.dao.CampaignScheduleDao;
import com.yazino.engagement.campaign.dao.MySqlLockDao;
import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CampaignSchedulerTest {
    public static final long CAMPAIGN_ID = 1l;
    public static final long CAMPAIGN_ID2 = 2l;
    public static final DateTime CURRENT_RUN_TS = new DateTime();
    public static final DateTime END_TIME = new DateTime();
    public static final long RUN_HOURS = 168l;
    public static final long RUN_MINUTES = 16l;
    public static final CampaignSchedule CAMPAIGN_SCHEDULE2 = new CampaignSchedule(CAMPAIGN_ID2, CURRENT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME);
    public static final CampaignSchedule CAMPAIGN_SCHEDULE1 = new CampaignSchedule(1l, CURRENT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME);
    private CampaignScheduler underTest;

    @Mock
    private CampaignScheduleDao campaignScheduleDao;

    @Mock
    private CampaignRunService campaignRunService;

    @Mock
    private MySqlLockDao lockDao;

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private DateTime now = new DateTime();

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now.getMillis());
        when(lockDao.lock(anyString(), anyString())).thenReturn(true);
        underTest = new CampaignScheduler(campaignScheduleDao, campaignRunService, lockDao, yazinoConfiguration);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void runScheduledCampaignsShouldUpdateDbWithNextRunTsForCampaign() {
        final CampaignSchedule campaignSchedule = new CampaignSchedule(1l, CURRENT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME);
        when(campaignScheduleDao.getDueCampaigns(now)).thenReturn(Lists.newArrayList(campaignSchedule));
        underTest.runScheduledCampaign();

        Mockito.verify(campaignScheduleDao).updateNextRunTs(CAMPAIGN_ID, new DateTime(CURRENT_RUN_TS.getMillis()
                + (DateTimeConstants.MILLIS_PER_HOUR * RUN_HOURS)
                + (DateTimeConstants.MILLIS_PER_MINUTE * RUN_MINUTES)));
        Mockito.verify(campaignRunService).runCampaign(CAMPAIGN_ID, CURRENT_RUN_TS.toDate());
    }

    @Test
    public void runScheduledCampaignsShouldUpdateDbWithNextRunTsWithTimeFromNowForSchedulesLessThanAnHour() {
        final CampaignSchedule campaignSchedule = new CampaignSchedule(1l, CURRENT_RUN_TS.minusMinutes(10), 0L, 2L, END_TIME);
        when(campaignScheduleDao.getDueCampaigns(now)).thenReturn(Lists.newArrayList(campaignSchedule));
        underTest.runScheduledCampaign();

        Mockito.verify(campaignScheduleDao).updateNextRunTs(CAMPAIGN_ID, new DateTime().plusMinutes(2));
        Mockito.verify(campaignRunService).runCampaign(CAMPAIGN_ID, CURRENT_RUN_TS.minusMinutes(10).toDate());
    }

    @Test
    public void runScheduledCampaignsShouldHandleMoreThanOneDueCampaign() {
        when(campaignScheduleDao.getDueCampaigns(now)).thenReturn(Lists.newArrayList(CAMPAIGN_SCHEDULE1, CAMPAIGN_SCHEDULE2));
        underTest.runScheduledCampaign();

        Mockito.verify(campaignScheduleDao).updateNextRunTs(CAMPAIGN_ID, CAMPAIGN_SCHEDULE1.calculateNextRunTs());
        Mockito.verify(campaignScheduleDao).updateNextRunTs(CAMPAIGN_ID2, CAMPAIGN_SCHEDULE2.calculateNextRunTs());
        Mockito.verify(campaignRunService).runCampaign(CAMPAIGN_ID, CURRENT_RUN_TS.toDate());
        Mockito.verify(campaignRunService).runCampaign(CAMPAIGN_ID2, CURRENT_RUN_TS.toDate());
    }

    @Test
    public void runScheduledCampaignsShouldNotRunConcurrently() {
        when(lockDao.lock(eq("campaign_runner"), anyString())).thenReturn(false);
        when(campaignScheduleDao.getDueCampaigns(now)).thenReturn(Lists.newArrayList(CAMPAIGN_SCHEDULE1));

        underTest.runScheduledCampaign();
        Mockito.verifyZeroInteractions(campaignScheduleDao);
    }

    @Test
    public void runScheduledCampaignsShouldUnlockAfterRun() {
        when(lockDao.lock(eq("campaign_runner"), anyString())).thenReturn(true);
        when(campaignScheduleDao.getDueCampaigns(now)).thenReturn(Lists.newArrayList(CAMPAIGN_SCHEDULE1));

        underTest.runScheduledCampaign();
        verify(lockDao).unlock(eq("campaign_runner"), anyString());
    }

    @Test
    public void runScheduledShouldUnlockOnError() {
        when(lockDao.lock(eq("campaign_runner"), anyString())).thenReturn(true);
        when(campaignScheduleDao.getDueCampaigns(now)).thenThrow(new RuntimeException());

        underTest.runScheduledCampaign();
        verify(lockDao).unlock(eq("campaign_runner"), anyString());
    }

    @Test
    public void runScheduledShouldNotRunIfItHasBeenDisabled(){
        when(yazinoConfiguration.getBoolean("engagement.campaigns.schedule.disabled", Boolean.FALSE)).thenReturn(Boolean.TRUE);
        underTest.runScheduledCampaign();
        verifyZeroInteractions(lockDao);
        verifyZeroInteractions(campaignScheduleDao);
    }
}
