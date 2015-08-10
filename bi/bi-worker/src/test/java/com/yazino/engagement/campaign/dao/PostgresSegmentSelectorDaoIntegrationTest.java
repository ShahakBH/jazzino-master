package com.yazino.engagement.campaign.dao;

import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.platform.Platform;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import utils.PlayerBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableSet.of;
import static com.yazino.platform.Platform.IOS;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.joda.time.DateTime.now;
import static utils.PlayerBuilder.*;


@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(defaultRollback = true)
//@Transactional
@DirtiesContext
public class PostgresSegmentSelectorDaoIntegrationTest {

    private static final DateTime TODAY = new DateTime();
    private static final DateTime YESTERDAY = TODAY.minusDays(1);
    private static final DateTime DAY_BEFORE_YESTERDAY = YESTERDAY.minusDays(2);
    private static final BigDecimal ACTIVE_PLAYER_ID = BigDecimal.ONE;
    public static final String SLOTS = "SLOTS";

    private static final String PLAYERS_THAT_PLAYED_YESTERDAY =
            "SELECT player_id, game FROM PLAYER_ACTIVITY_DAILY "
                    + "WHERE activity_ts >= CURRENT_DATE - INTERVAL '1 day' "
                    + "and activity_ts < CURRENT_DATE";

    PostgresSegmentSelectorDao underTest;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;
    @Autowired
    private JdbcTemplate template;
    @Autowired
    private NamedParameterJdbcTemplate namedTemplate;

    @Before
    public void setUp() throws Exception {

        underTest = new PostgresSegmentSelectorDao(namedTemplate, yazinoConfiguration);
        template.update("DELETE FROM PLAYER_ACTIVITY_DAILY");
        template.update("DELETE FROM SEGMENT_SELECTION");
        namedTemplate = new NamedParameterJdbcTemplate(template.getDataSource());
        PlayerBuilder.initialise(namedTemplate);
    }

    @Test(expected = RuntimeException.class)
    public void constructorShouldThrowExceptionIfJdbcTemplateIsNull() {
        underTest = new PostgresSegmentSelectorDao(null, yazinoConfiguration);
    }

    @Test(expected = RuntimeException.class)
    public void constructorShouldThrowExceptionIfYazinoConfigurationIsNull() {
        underTest = new PostgresSegmentSelectorDao(namedTemplate, null);
    }

    @Test
    public void fetchPlayersForCampaignRunCorrectSqlAndInvokeVisitorWithCorrectResults() {
        insertIntoPlayerActivityDaily(ACTIVE_PLAYER_ID, "SLOTS", IOS, YESTERDAY, YESTERDAY);
        insertIntoPlayerActivityDaily(BigDecimal.TEN, "SLOTS", IOS, TODAY, TODAY);
        insertIntoPlayerActivityDaily(valueOf(123), "SLOTS", IOS, DAY_BEFORE_YESTERDAY, DAY_BEFORE_YESTERDAY);
        final List<PlayerWithContent> playerList = new ArrayList<>();

        final int resultCount = underTest.fetchSegment(
                PLAYERS_THAT_PLAYED_YESTERDAY, TODAY, new BatchVisitor<PlayerWithContent>() {
                    @Override
                    public void processBatch(final List<PlayerWithContent> batch) {
                        playerList.addAll(batch);
                    }
                });

        assertThat(resultCount, is(equalTo(1)));
        assertThat(playerList.size(), equalTo(1));
        assertThat(playerList.get(0).getPlayerId(), comparesEqualTo(ACTIVE_PLAYER_ID));
        assertThat(playerList.get(0).getContent().get("GAME"), comparesEqualTo("SLOTS"));
    }

    @Test
    public void fetchPlayersForCampaignRunCorrectSqlAndInvokeVisitorWithCorrectResultsPart2() {
        final List<PlayerWithContent> playerList = new ArrayList<>();

        // for was this throwing a class cast exception because player_id is not typed in the DB.
        final int resultCount = underTest.fetchSegment(
                "SELECT 1 as player_id", TODAY, new BatchVisitor<PlayerWithContent>() {
                    @Override
                    public void processBatch(final List<PlayerWithContent> batch) {
                        playerList.addAll(batch);
                    }
                });

        assertThat(resultCount, is(equalTo(1)));
        assertThat(playerList.size(), equalTo(1));
        assertThat(playerList.get(0).getPlayerId(), comparesEqualTo(BigDecimal.valueOf(1)));
    }

    @Test
    public void fetchPlayersForCampaignShouldMapColumnsToPlayerShouldMapAllColumns() {
        final List<PlayerWithContent> playerList = new ArrayList<>();

        final int resultCount = underTest.fetchSegment(
                "SELECT 1 as player_id, 'testdata1' as victory, 'testdata2' as yousuck", TODAY,
                new BatchVisitor<PlayerWithContent>() {
                    @Override
                    public void processBatch(final List<PlayerWithContent> batch) {
                        playerList.addAll(batch);
                    }
                });

        assertThat(resultCount, is(equalTo(1)));
        assertThat(playerList.size(), equalTo(1));
        PlayerWithContent playerWithContent = playerList.get(0);
        assertThat(playerWithContent.getPlayerId(), comparesEqualTo(BigDecimal.valueOf(1)));
        assertThat(playerWithContent.getContent().get("VICTORY"), is(equalTo("testdata1")));
        assertThat(playerWithContent.getContent().get("YOUSUCK"), is(equalTo("testdata2")));
    }

    @Test
    public void updateSegmentSelectsShouldUpdatePendingSegmentsForPeopleWithMatchingLastPlayed() {

        createPlayer(ANDY).withCampaignRunSegmentFor(-666L).whoLoggedIn(now().minusDays(1).minusMinutes(1)).storeIn(namedTemplate);
        createPlayer(BOB).withCampaignRunSegmentFor(-666L).whoLoggedIn(now().minusDays(1).minusHours(1).plusMinutes(1)).storeIn(namedTemplate);
        createPlayer(CHAZ).withCampaignRunSegmentFor(-666L).whoLoggedIn(now().minusDays(1).minusHours(2)).storeIn( namedTemplate);
        createPlayer(DAVE).withCampaignRunSegmentFor(-666L).whoLoggedIn(now().minusHours(1)).storeIn(namedTemplate);
        createPlayer(ERNIE).withCampaignRunSegmentFor(-666L).whoLoggedIn(now().minusDays(3).minusMinutes(1)).storeIn(namedTemplate);

        assertThat(template.queryForInt("select count(*) from segment_selection "), is(5));
        assertThat(template.queryForInt("select count(*) from segment_selection where valid_from is not null "), is(0));
        underTest.updateSegmentDelaysForCampaignRuns(of(-666L), now());

        assertThat(template.queryForInt("select count(*) from segment_selection "), is(5));
        assertThat(template.queryForInt("select count(*) from segment_selection where valid_from is not null "), is(3));

        assertThat(getValidFromFor(ANDY), equalTo(now().withMillisOfSecond(0)));
        assertThat(getValidFromFor(BOB), equalTo(now().withMillisOfSecond(0)));
        assertThat(getValidFromFor(ERNIE), equalTo(now().withMillisOfSecond(0)));

    }

    @Test
    public void updateSegmentShouldRerunOnlyForSegmentsWithNullValidFrom() {

        createPlayer(ANDY).whoLoggedIn(now().minusDays(1).minusMinutes(1)).storeIn(namedTemplate);
        createPlayer(BOB).whoLoggedIn(now().minusDays(1).minusHours(1).plusMinutes(1)).storeIn(namedTemplate);

        template.update(
                "insert into segment_selection (campaign_run_id, player_id,valid_from) values (-666,1,'2014-01-01')");
        template.update("insert into segment_selection (campaign_run_id, player_id,valid_from) values (-666,2,null)");

        underTest.updateSegmentDelaysForCampaignRuns(of(-666L), now());

        assertThat(template.queryForInt("select count(*) from segment_selection where valid_from is not null "), is(2));

        assertThat(getValidFromFor(BOB), equalTo(now().withMillisOfSecond(0)));
        assertThat(getValidFromFor(ANDY), equalTo(new DateTime(2014, 01, 01, 00, 00)));

    }

    private DateTime getValidFromFor(final int playerId) {
        return new DateTime(
                template.queryForMap("select * from segment_selection where player_id=" + playerId).get(
                        "valid_from")).withMillisOfSecond(0);
    }

    private void insertIntoPlayerActivityDaily(BigDecimal playerId,
                                               String gameType,
                                               Platform platform,
                                               DateTime activityTs,
                                               DateTime reg_ts) {
        template.update(
                "INSERT INTO PLAYER_ACTIVITY_DAILY (player_id, game, platform, activity_ts, referrer, reg_ts) VALUES (?,?,?,?,?,?)"
                , playerId,
                gameType,
                platform.name(),
                new Timestamp(activityTs.getMillis()),
                "referrer",
                new Timestamp(reg_ts.getMillis())
        );
    }


}
