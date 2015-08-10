package com.yazino.bi.operations.campaigns.controller;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ContextConfiguration
@Transactional
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignScheduleWithNameDaoIntegrationTest {
    public static final DateTime NEXT_RUN_TS = new DateTime().plusDays(2).withMillisOfSecond(0);
    public static final long RUN_HOURS = 168l;
    public static final long CAMPAIGN_ID = 1l;
    public static final long RUN_MINUTES = 0L;
    private CampaignScheduleWithNameDao underTest;

    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate template;

    @Before
    public void setUp() throws Exception {
        underTest = new CampaignScheduleWithNameDao(template);
        template.update("delete from CAMPAIGN_RUN");
        template.update("delete from CAMPAIGN_CONTENT");
        template.update("delete from CAMPAIGN_CHANNEL_CONFIG");
        template.update("delete from CAMPAIGN_CHANNEL");
        template.update("delete from CAMPAIGN_SCHEDULE");
        template.update("delete from CAMPAIGN_TARGET");
        template.update("delete from CAMPAIGN_DEFINITION");

        template.update("INSERT INTO CAMPAIGN_DEFINITION (id, name, segmentSqlQuery) VALUES (?,?,?)", 1l, "test", "select 1");
        template.update("INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)", 1l, new Timestamp(NEXT_RUN_TS.getMillis()), RUN_HOURS, RUN_MINUTES, null);
    }

    @Test
    public void getCampaignListShouldReturnAllScheduledCampaigns() throws Exception {
        final List<CampaignScheduleWithName> actual = underTest.getCampaignList(false);
        final List<CampaignScheduleWithName> expected = asList(new CampaignScheduleWithName(1l, "test", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES));

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void getCampaignListShouldReturnScheduledCampaignsBetweenBoundary() throws Exception {

        template.update("INSERT INTO CAMPAIGN_DEFINITION (id, name, segmentSqlQuery) VALUES (?,?,?)", 2l, "test2", "select 1");
        template.update("INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)", 2l, new Timestamp(NEXT_RUN_TS.getMillis()-1), RUN_HOURS, RUN_MINUTES, null);

        final List<CampaignScheduleWithName> actual = underTest.getCampaignList(0, 1,false);
        final List<CampaignScheduleWithName> expected = asList(new CampaignScheduleWithName(1l, "test", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES));

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void getCampaignListShouldNotReturnDisabledCampaignsWhenSpecified() throws Exception {

        template.update("INSERT INTO CAMPAIGN_DEFINITION (id, name, segmentSqlQuery,enabled) VALUES (?,?,?,?)", 2l, "disabled", "select 1",false);
        template.update("INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)", 2l, new Timestamp(NEXT_RUN_TS.getMillis()), RUN_HOURS, RUN_MINUTES, null);

        final List<CampaignScheduleWithName> actual = underTest.getCampaignList(0, 10,false);
        final List<CampaignScheduleWithName> expected = asList(new CampaignScheduleWithName(1l, "test", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES));

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void getCampaignListShouldReturnDisabledCampaignsWhenSpecified() throws Exception {

        template.update("INSERT INTO CAMPAIGN_DEFINITION (id, name, segmentSqlQuery,enabled) VALUES (?,?,?,?)", 2l, "disabled", "select 1",false);
        template.update("INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)", 2l, new Timestamp(NEXT_RUN_TS.getMillis()+1), RUN_HOURS, RUN_MINUTES, null);

        final List<CampaignScheduleWithName> actual = underTest.getCampaignList(0, 10,true);

        final CampaignScheduleWithName camp2=new CampaignScheduleWithName(2l, "disabled", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES);
        final CampaignScheduleWithName camp1 = new CampaignScheduleWithName(1l, "test", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES);

        final List<CampaignScheduleWithName> expected = asList(camp1,camp2);

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void countOfCampaignRecordsShouldReturnTotalNumberOfCampaignRecords(){
     Assert.assertThat(underTest.getCampaignRecordCount(), CoreMatchers.is(IsEqual.equalTo(1)));
    }

    @Test
    public void getCampaignScheduledWithNameShouldReturnACampaignScheduleWithName(){
     Assert.assertThat(underTest.getCampaignScheduleWithName(CAMPAIGN_ID),
             CoreMatchers.is(IsEqual.equalTo(new CampaignScheduleWithName(CAMPAIGN_ID, "test", NEXT_RUN_TS, null, RUN_HOURS, RUN_MINUTES))));
    }


}
