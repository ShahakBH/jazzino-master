package com.yazino.engagement.campaign.dao;

import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.platform.util.BigDecimals;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.yaps.JsonHelper;
import org.joda.time.DateTime;
import org.junit.After;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.joda.time.DateTime.now;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional()
@DirtiesContext
public class CampaignRunDaoImplIntegrationTest {
    private static final Long CAMPAIGN_ID = 1l;
    private static final long CAMPAIGN_RUN_ID = 2l;
    private static final Timestamp RUN_TS = new Timestamp(new DateTime().withMillisOfSecond(0).getMillis());
    private static final PlayerWithContent PLAYER_ONE = new PlayerWithContent(ONE);
    private static final PlayerWithContent PLAYER_TEN = new PlayerWithContent(TEN);

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;
    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate dwJdbcTemplate;

    private final JsonHelper jsonHelper = new JsonHelper(true);

    private CampaignRunDao underTest;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now().withMillisOfSecond(0).getMillis());
        jdbc.update("DELETE FROM SEGMENT_SELECTION");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_CONTENT");
        dwJdbcTemplate.update("delete from CAMPAIGN_CHANNEL_CONFIG");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_RUN");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_CHANNEL");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_TARGET");

        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_RUN");
        dwJdbcTemplate.update("DELETE FROM CAMPAIGN_DEFINITION");
        dwJdbcTemplate.update(
                "INSERT INTO CAMPAIGN_DEFINITION (id, name) VALUES " +
                        "(1,'campaignRunDaoImplTest')," +
                        "(2,'campaignRunDaoImplTest 2')," +
                        "(3,'campaignRunDaoImplTest 3')"
        );

        underTest = new CampaignRunDaoImpl(jdbc, dwJdbcTemplate);

        PLAYER_TEN.getContent().put("YOUR MUM", "123");
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();

    }

    @Test
    public void testCreateCampaignRun() throws Exception {
        final Long actualCampaignRunId = underTest.createCampaignRun(CAMPAIGN_ID, new DateTime());

        final long actualCampaignId = dwJdbcTemplate.queryForLong(
                "SELECT CAMPAIGN_ID FROM CAMPAIGN_RUN WHERE ID=?", actualCampaignRunId);
        assertThat(actualCampaignId, equalTo(CAMPAIGN_ID));
    }

    @Test
    public void testStorePlayers() throws Exception {
        insertCampaignRunRecord(RUN_TS);

        final List<PlayerWithContent> segmentSelection = getPlayerWithContents();

        underTest.addPlayers(CAMPAIGN_RUN_ID, segmentSelection, false);

        final List<BigDecimal> actualPlayerIds = strip(
                jdbc.queryForList(
                        "SELECT PLAYER_ID FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID = ?", BigDecimal.class,
                        CAMPAIGN_RUN_ID)
        );
        assertThat(actualPlayerIds, hasItems(ONE, TEN));
    }

    private List<PlayerWithContent> getPlayerWithContents() {
        return newArrayList(new PlayerWithContent(ONE), new PlayerWithContent(TEN));
    }

    private List<BigDecimal> strip(final List<BigDecimal> bigDecimals) {
        List<BigDecimal> stripped = newArrayList();
        for (BigDecimal actualPlayerId : bigDecimals) {
            stripped.add(BigDecimals.strip(actualPlayerId));
        }
        return stripped;
    }


    @Test
    public void testStorePlayersShouldIgnoreDuplicateRecord() throws Exception {
        insertCampaignRunRecord(RUN_TS);

        underTest.addPlayers(
                CAMPAIGN_RUN_ID, newArrayList(
                        new PlayerWithContent(ONE),
                        new PlayerWithContent(ONE)), false
        );

        final List<BigDecimal> actualPlayerIds = strip(
                jdbc.queryForList(
                        "SELECT PLAYER_ID FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID = ?", BigDecimal.class,
                        CAMPAIGN_RUN_ID)
        );

        assertThat(actualPlayerIds, hasItems(ONE));
    }

    @Test
    public void delayedNotificationsShouldCreateSegmentsWithNoValidFromTimes() {
        underTest.addPlayers(CAMPAIGN_RUN_ID, getPlayerWithContents(), true);
        final List<Map<String, Object>> maps = jdbc.queryForList("select * from segment_selection");
        assertThat(maps.size(), is(2));
        assertThat(maps.get(0).get("valid_from"), nullValue());
        assertThat(maps.get(1).get("valid_from"), nullValue());
    }

    @Test
    public void nonDelayedNotificationsShouldCreateSegmentsWithFromTimes() {
        underTest.addPlayers(CAMPAIGN_RUN_ID, getPlayerWithContents(), false);
        final List<Map<String, Object>> maps = jdbc.queryForList("select * from segment_selection");
        assertThat(maps.size(), is(2));
        assertThat((Timestamp) maps.get(0).get("valid_from"), equalTo(new Timestamp(now().getMillis())));
        assertThat((Timestamp) maps.get(1).get("valid_from"), equalTo(new Timestamp(now().getMillis())));

    }

    @Test
    public void getCampaignRunIdsShouldReturnLatestRunIdsForDelayedNotificationCampaigns() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2014, 01, 03, 00, 00).getMillis());

        dwJdbcTemplate.update(
                "update CAMPAIGN_DEFINITION set delay_notifications = true"
        );
        dwJdbcTemplate.update(
                "insert into CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS) values " +
                        "(1,1,'2014-01-02'),(2,1,'2014-01-03'),(3,2,'2014-01-02'),(4,3,'2014-01-01') "
        );
        final Map<Long, Long> latestCampaignRuns = underTest.getLatestDelayedCampaignRunsInLast24Hours();
        assertThat(latestCampaignRuns.keySet(), contains(2L, 3L));
        assertThat(latestCampaignRuns.get(2L), is(1L));
        assertThat(latestCampaignRuns.get(3L), is(2L));
    }

    @Test
    public void getCampaignRunIdsShouldNotReturnLatestRunIdsForNonDelayedNotificationCampaigns() {
        dwJdbcTemplate.update(
                "update CAMPAIGN_DEFINITION set delay_notifications = false"
        );
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2014, 01, 03, 00, 00).getMillis());
        dwJdbcTemplate.update(
                "insert into CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS) values " +
                        "(1,1,'2014-01-02'),(2,1,'2014-01-03'),(3,2,'2014-01-02'),(4,3,'2014-01-01') "
        );
        final Map<Long, Long> latestCampaignRuns = underTest.getLatestDelayedCampaignRunsInLast24Hours();
        assertThat(latestCampaignRuns.keySet(), empty());
    }

    @Test
    public void getLastRuntimeShouldReturnLastRuntimeAndStoreCurrentTime() {
        dwJdbcTemplate.update(
                "insert into CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS, last_rerun_ts) " +
                        "values (1,1,'2014-01-01','2014-01-02')"
        );

        final DateTime lastRuntime = underTest.getLastRuntimeForCampaignRunIdAndResetTo(1L, now());

        assertThat(lastRuntime, equalTo(new DateTime(2014, 01, 02, 0, 0)));
        final Timestamp storedLastRun = (Timestamp) dwJdbcTemplate.queryForMap(
                "select last_rerun_ts from CAMPAIGN_RUN where id=1").get("last_rerun_ts");
        assertThat(storedLastRun, equalTo(toTs(now())));

    }

    @Test
    public void getLastRuntimeShouldReturnRuntimeAndStoreCurrentTimeForNullRuntime() {
        dwJdbcTemplate.update(
                "insert into CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS, last_rerun_ts) " +
                        "values (1,1,'2014-01-01',null)"
        );

        final DateTime lastRuntime = underTest.getLastRuntimeForCampaignRunIdAndResetTo(1L, now());

        assertThat(lastRuntime, equalTo(new DateTime(2014, 01, 01, 0, 0)));
        final Timestamp storedLastRun = (Timestamp) dwJdbcTemplate.queryForMap(
                "select last_rerun_ts from CAMPAIGN_RUN where id=1").get("last_rerun_ts");
        assertThat(storedLastRun, equalTo(toTs(now())));
    }


    private Timestamp toTs(final DateTime dateTime) {
        return new Timestamp(dateTime.getMillis());
    }

    @Test
    public void testStorePlayersShouldIgnoreDuplicateRecordsOverMultipleBatches() throws Exception {
        insertCampaignRunRecord(RUN_TS);

        underTest.addPlayers(
                CAMPAIGN_RUN_ID, newArrayList(new PlayerWithContent(ONE), new PlayerWithContent(ONE)),
                false);
        underTest.addPlayers(
                CAMPAIGN_RUN_ID, newArrayList(new PlayerWithContent(ONE), new PlayerWithContent(BigDecimal.valueOf(7))),
                false);
        underTest.addPlayers(
                CAMPAIGN_RUN_ID,
                newArrayList(new PlayerWithContent(BigDecimal.TEN), new PlayerWithContent(BigDecimal.valueOf(7))),
                false);

        final List<BigDecimal> actualPlayerIds = strip(
                jdbc.queryForList(
                        "SELECT PLAYER_ID FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID = ?", BigDecimal.class,
                        CAMPAIGN_RUN_ID)
        );

        assertThat(actualPlayerIds, hasItems(BigDecimal.valueOf(7), ONE, BigDecimal.TEN));
    }

    private int insertCampaignRunRecord(final Timestamp runTs) {
        return dwJdbcTemplate.update(
                "INSERT INTO CAMPAIGN_RUN (ID, CAMPAIGN_ID, RUN_TS) VALUES (?,?,?)",
                CAMPAIGN_RUN_ID, CAMPAIGN_ID, runTs);
    }

    @Test
    public void testFetchPlayers() throws Exception {
        insertCampaignRunRecord(RUN_TS);
        final Map<String, Object> content = newLinkedHashMap();
        content.put("YOUR MUM", "123");
        jdbc.update("INSERT INTO SEGMENT_SELECTION (CAMPAIGN_RUN_ID, PLAYER_ID) VALUES (?, ?)", CAMPAIGN_RUN_ID, ONE);
        jdbc.update(
                "INSERT INTO SEGMENT_SELECTION (CAMPAIGN_RUN_ID, PLAYER_ID, CONTENT) VALUES (?, ?,?)",
                CAMPAIGN_RUN_ID,
                TEN, jsonHelper.serialize(content));

        final List<PlayerWithContent> actualPlayers = underTest.fetchPlayers(CAMPAIGN_RUN_ID);
        assertThat(actualPlayers, containsInAnyOrder(PLAYER_ONE, PLAYER_TEN));

        for (PlayerWithContent player : actualPlayers) {
            if (player.getPlayerId().compareTo(TEN) == 0) {
                assertThat(player.getContent().get("YOUR MUM"), equalTo("123"));
            }
        }
    }

    @Test
    public void testFetchPlayersReturnsEmptyListOnNoRecords() throws Exception {
        insertCampaignRunRecord(RUN_TS);

        final List<PlayerWithContent> actualPlayerIds = underTest.fetchPlayers(CAMPAIGN_RUN_ID);
        assertThat(actualPlayerIds.size(), equalTo(0));
    }

    @Test
    public void testGetCampaignRunShouldReturnACampaignRun() {
        insertCampaignRunRecord(RUN_TS);

        final CampaignRun actual = underTest.getCampaignRun(CAMPAIGN_RUN_ID);
        final CampaignRun expected = new CampaignRun(CAMPAIGN_RUN_ID, CAMPAIGN_ID, new DateTime(RUN_TS.getTime()));

        assertThat(actual.getCampaignId(), equalTo(expected.getCampaignId()));
        assertThat(actual.getCampaignRunId(), equalTo(expected.getCampaignRunId()));
        assertThat(actual.getRunTimestamp(), equalTo(expected.getRunTimestamp()));
    }

    @Test
    public void addPlayersShouldDefaultValidFrom() {

    }

//    @Test
//    public void getGiftingCampaignsThatHaveRunInLast24HoursShouldReturnSetOfCampaigns(){
//        insertCampaignRunRecord(now().ni);
//        Set<CampaignRun> campaignRuns =underTest.getGiftingCampaignsRunSince(now().minusHours(24));
////        underTest.getGiftingPlayersForCampaigns();
//    }
}
