package com.yazino.bi.campaign.dao;

import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.joda.time.DateTime.now;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional()
@DirtiesContext
public class MySqlCampaignScheduleDaoIntegrationTest {

    public static final Long CAMPAIGN_ID = 1l;
    public static final DateTime CURRENT_TIME = new DateTime().withMillisOfSecond(0);
    public static final DateTime END_TIME_AFTER_CURRENT_TIME = CURRENT_TIME.withMillisOfSecond(0).plusHours(1);
    public static final DateTime CAMPAIGN_LAST_RUN_TS = new DateTime().withMillisOfSecond(0).minusDays(3);
    public static final Long RUN_HOURS = 168l;
    public static final Long RUN_MINUTES = 30l;
    @Autowired
    private JdbcTemplate jdbc;
    private CampaignScheduleDao underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now().getMillis());
        jdbc.update("DELETE FROM CAMPAIGN_SCHEDULE");
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL");
        jdbc.update("DELETE FROM CAMPAIGN_RUN");
        jdbc.update("DELETE FROM CAMPAIGN_CONTENT");
        jdbc.update("DELETE FROM CAMPAIGN_CHANNEL_CONFIG");
        jdbc.update("DELETE FROM CAMPAIGN_DEFINITION");

        underTest = new MySqlCampaignScheduleDao(jdbc);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void getCampaignScheduleShouldReturnCampaignSchedule() {
        insertIntoCampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, END_TIME_AFTER_CURRENT_TIME, RUN_MINUTES);
        final CampaignSchedule actual = underTest.getCampaignSchedule(CAMPAIGN_ID);
        assertThat(
                actual, equalTo(
                        new CampaignSchedule(
                                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES,
                                END_TIME_AFTER_CURRENT_TIME)
                )
        );
    }

    @Test
    public void updateScheduleShouldUpdateNextRunTsOnly() throws Exception {
        final long runHours = 1l;
        insertIntoCampaignScheduleNoExpireTime(CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, runHours, RUN_MINUTES);

        final DateTime expectedRunTimestamp = new DateTime().withMillisOfDay(0);

        underTest.updateNextRunTs(CAMPAIGN_ID, expectedRunTimestamp);

        Timestamp actualNextRunTimestamp = jdbc.queryForObject(
                "SELECT NEXT_RUN_TS from CAMPAIGN_SCHEDULE where CAMPAIGN_ID =?", Timestamp.class, CAMPAIGN_ID);
        Long actualRunHours = jdbc.queryForObject(
                "SELECT RUN_HOURS from CAMPAIGN_SCHEDULE where CAMPAIGN_ID =?", Long.class, CAMPAIGN_ID);

        assertThat(new DateTime(actualNextRunTimestamp), equalTo(expectedRunTimestamp));
        assertThat(actualRunHours, equalTo(runHours));
    }

    @Test
    public void testGetDueCampaigns() throws Exception {
        final long dueCampaign2 = 2l;
        final long notDueCampaign = 3l;

        insertIntoCampaignScheduleNoExpireTime(CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES);
        insertIntoCampaignScheduleNoExpireTime(dueCampaign2, CURRENT_TIME, RUN_HOURS, RUN_MINUTES);
        insertIntoCampaignScheduleNoExpireTime(notDueCampaign, CURRENT_TIME.plusHours(1), RUN_HOURS, RUN_MINUTES);

        final List<CampaignSchedule> actualDueCampaigns = underTest.getDueCampaigns(CURRENT_TIME);
        assertThat(actualDueCampaigns.size(), equalTo(2));
        final CampaignSchedule campaignBeforeCurrentTime = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES, null);
        final CampaignSchedule campaignOnCurrentTime = new CampaignSchedule(
                dueCampaign2, CURRENT_TIME, RUN_HOURS, RUN_MINUTES, null);
        assertThat(actualDueCampaigns, hasItems(campaignBeforeCurrentTime, campaignOnCurrentTime));
    }

    @Test
    public void disablingCampaignsShouldStopThemAppearingAsDue() {
        final long dueCampaign = 2l;

        insertIntoCampaignScheduleNoExpireTime(dueCampaign, CURRENT_TIME, RUN_HOURS, RUN_MINUTES);

        final List<CampaignSchedule> actualDueCampaigns = underTest.getDueCampaigns(CURRENT_TIME);
        assertThat(actualDueCampaigns.size(), equalTo(1));
        final CampaignSchedule campaignOnCurrentTime = new CampaignSchedule(
                dueCampaign, CURRENT_TIME, RUN_HOURS, RUN_MINUTES, null);
        assertThat(actualDueCampaigns, hasItems(campaignOnCurrentTime));

        jdbc.update("update CAMPAIGN_DEFINITION set enabled=0 where id=2");

        final List<CampaignSchedule> emptyListOfCampaigns = underTest.getDueCampaigns(CURRENT_TIME);
        assertThat(emptyListOfCampaigns.size(), equalTo(0));
    }

    @Test
    public void testGetDueCampaignsShouldNotGetRecordsThatHaveGonePastEndTime() {
        final long dueCampaign2 = 2l;
        final long notDueCampaign = 3l;

        insertIntoCampaignScheduleNoExpireTime(CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES);
        insertIntoCampaignSchedule(
                dueCampaign2, CURRENT_TIME, RUN_HOURS,
                MySqlCampaignScheduleDaoIntegrationTest.END_TIME_AFTER_CURRENT_TIME, RUN_MINUTES);
        insertIntoCampaignSchedule(notDueCampaign, CURRENT_TIME, RUN_HOURS, CURRENT_TIME.minusMinutes(1), RUN_MINUTES);

        final List<CampaignSchedule> actualDueCampaigns = underTest.getDueCampaigns(CURRENT_TIME);
        assertThat(actualDueCampaigns.size(), equalTo(2));
        final CampaignSchedule campaignBeforeCurrentTime = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES, null);
        final CampaignSchedule campaignOnCurrentTime = new CampaignSchedule(
                dueCampaign2, CURRENT_TIME, RUN_HOURS, RUN_MINUTES, END_TIME_AFTER_CURRENT_TIME);

        assertThat(actualDueCampaigns, hasItems(campaignBeforeCurrentTime, campaignOnCurrentTime));
    }

    @Test
    public void saveShouldPersistRecord() {
        final CampaignSchedule expected = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME_AFTER_CURRENT_TIME);
        underTest.save(expected);

        final CampaignSchedule actual = jdbc.queryForObject(
                "SELECT * FROM CAMPAIGN_SCHEDULE WHERE CAMPAIGN_ID = ?", new RowMapper<CampaignSchedule>() {
                    @Override
                    public CampaignSchedule mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignSchedule(
                                rs.getLong("CAMPAIGN_ID"),
                                getNullDateTIme(rs.getTimestamp("NEXT_RUN_TS")),
                                rs.getLong("RUN_HOURS"),
                                rs.getLong("RUN_MINUTES"),
                                getNullDateTIme(rs.getTimestamp("END_TIME")));
                    }
                }, CAMPAIGN_ID);
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(expected)));
    }

    @Test
    public void saveShouldPersistRecordWithNullEndTime() {
        final CampaignSchedule expected = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES, null);
        underTest.save(expected);

        final CampaignSchedule actual = jdbc.queryForObject(
                "SELECT * FROM CAMPAIGN_SCHEDULE WHERE CAMPAIGN_ID = ?", new RowMapper<CampaignSchedule>() {
                    @Override
                    public CampaignSchedule mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignSchedule(
                                rs.getLong("CAMPAIGN_ID"),
                                getNullDateTIme(rs.getTimestamp("NEXT_RUN_TS")),
                                rs.getLong("RUN_HOURS"),
                                rs.getLong("RUN_MINUTES"), getNullDateTIme(rs.getTimestamp("END_TIME")));
                    }
                }, CAMPAIGN_ID);
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(expected)));
    }

    @Test
    public void updateShouldUpdateExistingRecord() {
        final CampaignSchedule preExistingCampaignSchedule = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME_AFTER_CURRENT_TIME);
        underTest.save(preExistingCampaignSchedule);
        final CampaignSchedule expected = new CampaignSchedule(
                CAMPAIGN_ID, CAMPAIGN_LAST_RUN_TS.plusDays(2), RUN_HOURS + 10l, RUN_MINUTES,
                END_TIME_AFTER_CURRENT_TIME.plusMonths(2));
        underTest.update(expected);

        final CampaignSchedule actual = jdbc.queryForObject(
                "SELECT * FROM CAMPAIGN_SCHEDULE WHERE CAMPAIGN_ID = ?", new RowMapper<CampaignSchedule>() {
                    @Override
                    public CampaignSchedule mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignSchedule(
                                rs.getLong("CAMPAIGN_ID"),
                                getNullDateTIme(rs.getTimestamp("NEXT_RUN_TS")),
                                rs.getLong("RUN_HOURS"),
                                rs.getLong("RUN_MINUTES"), getNullDateTIme(rs.getTimestamp("END_TIME")));
                    }
                }, CAMPAIGN_ID);
        Assert.assertThat(actual, CoreMatchers.is(IsEqual.equalTo(expected)));
    }

    private DateTime getNullDateTIme(Timestamp timestamp) throws SQLException {
        if (timestamp == null) {
            return null;
        }

        return new DateTime(timestamp.getTime());
    }

    private int insertIntoCampaignScheduleNoExpireTime(final long dueCampaign2, final DateTime nextRunTs,
                                                       final long runHours, final Long runMinutes) {
        jdbc.update("INSERT INTO CAMPAIGN_DEFINITION (ID,ENABLED)values(?,1)", dueCampaign2);
        return jdbc.update(
                "INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES) VALUES (?,?,?,?)",
                dueCampaign2, toTs(nextRunTs), runHours, runMinutes);
    }

    private Timestamp toTs(final DateTime nextRunTs) {
        return new Timestamp(nextRunTs.getMillis());
    }

    private int insertIntoCampaignSchedule(final long dueCampaign2, final DateTime nextRunTs, final long runHours,
                                           final DateTime endTime, final Long runMinutes) {
        return insertIntoCampaignSchedule(dueCampaign2, nextRunTs, runHours, endTime, runMinutes, false);
    }

    private int insertIntoCampaignSchedule(final long dueCampaign2, final DateTime nextRunTs, final long runHours,
                                           final DateTime endTime, final Long runMinutes, boolean delayed) {
        jdbc.update(
                "INSERT INTO CAMPAIGN_DEFINITION (ID,ENABLED, DELAY_NOTIFICATIONS)values(?,1,?)", dueCampaign2,
                delayed);
        return jdbc.update(
                "INSERT INTO CAMPAIGN_SCHEDULE (CAMPAIGN_ID, NEXT_RUN_TS, RUN_HOURS, RUN_MINUTES, END_TIME) VALUES (?,?,?,?,?)",
                dueCampaign2, toTs(nextRunTs), runHours, runMinutes, toTs(endTime));
    }
}
