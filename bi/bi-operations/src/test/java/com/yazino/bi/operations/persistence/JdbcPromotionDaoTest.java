package com.yazino.bi.operations.persistence;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.apache.commons.collections.Predicate;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;
import strata.server.lobby.api.promotion.helper.BuyChipsPromotionBuilder;
import strata.server.lobby.api.promotion.helper.DailyAwardPromotionBuilder;
import com.yazino.bi.operations.model.PromotionPlayer;
import com.yazino.bi.operations.view.PromotionSearchType;

import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@SuppressWarnings({"ConstantConditions, NullableProblems"})
public class JdbcPromotionDaoTest {
    public static final String PROMOTION_ARCHIVE_INSERT_SQL =
            "insert into PROMOTION_ARCHIVE(promo_id, name, target_clients, start_date, end_date, all_players, player_count, type, "
                    + "priority, seed, control_group_percentage, cg_function) values(?,?,?,?,?,?,?,?,?,?,?,?)";
    public static final String PROMOTION_INSERT_SQL =
            "insert into PROMOTION(promo_id, name, target_clients, start_date, end_date, all_players, player_count, type, priority, "
                    + "seed, control_group_percentage, cg_function) values(?,?,?,?,?,?,?,?,?,?,?,?)";
    @Autowired
    @Qualifier("dwJdbcTemplate")
    private JdbcTemplate dwJdbcTemplate;

    @Autowired
    @Qualifier("replicaJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcBackOfficePromotionDao underTest;

    @Test(expected = NullPointerException.class)
    public void findWithNullSearchTypeFails() {
        underTest.find(null, PromotionType.BUY_CHIPS);
    }

    @Test(expected = NullPointerException.class)
    public void findWithNullPromotionTypeFails() {
        underTest.find(PromotionSearchType.LIVE, null);
    }

    @Before
    public void setCurrentTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void resetCurrentTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Before
    @After
    public void cleanup() {
        jdbcTemplate.update("delete from PROMOTION_CONFIG where promo_id < 0");
        jdbcTemplate.update("delete from PROMOTION_PLAYER_REWARD where promo_id < 0");
        jdbcTemplate.update("delete from PROMOTION_PLAYER where promo_id < 0");
        jdbcTemplate.update("delete from PROMOTION where promo_id < 0");
        dwJdbcTemplate.update("delete from PROMOTION_CONFIG_ARCHIVE where promo_id < 0");
        dwJdbcTemplate.update("delete from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id < 0");
        dwJdbcTemplate.update("delete from PROMOTION_PLAYER_ARCHIVE where promo_id < 0");
        dwJdbcTemplate.update("delete from PROMOTION_ARCHIVE where promo_id < 0");
    }

    @Test
    public void findWithSearchTypeLiveShouldReturnLivePromos() {
        // GIVEN known set of promotions
        List<Promotion> livePromotions = createKnownSetOfPromotions(generateArchivePromotions(), generateLivePromotions()).get(PromotionSearchType.LIVE);
        List<Promotion> expectedPromotions = new ArrayList<Promotion>(livePromotions);
        filterByPromotionType(expectedPromotions, PromotionType.DAILY_AWARD);


        // WHEN loading current promotions
        List<Promotion> actualPromotions = underTest.find(PromotionSearchType.LIVE, PromotionType.DAILY_AWARD);
        filterNonTestData(actualPromotions);

        // THEN all live promos should be returned
        assertTrue(actualPromotions.containsAll(expectedPromotions));
    }

    @Test
    public void findArchivedBuyChipsShouldReturnArchivedBuyChipPromos() {
        // GIVEN known set of promotions
        List<Promotion> archivedPromotions = createKnownSetOfPromotions(generateArchivePromotions(), generateLivePromotions()).get(PromotionSearchType.ARCHIVED);
        List<Promotion> buyChipPromotions = new ArrayList<Promotion>(archivedPromotions);
        filterByPromotionType(buyChipPromotions, PromotionType.BUY_CHIPS);

        // WHEN loading past promotions
        List<Promotion> actualPromotions = underTest.find(PromotionSearchType.ARCHIVED, PromotionType.BUY_CHIPS);
        filterNonTestData(actualPromotions);

        // THEN only archived, buy chip promotions before current date should be returned
        assertEquals(2, actualPromotions.size());
        assertThat(actualPromotions, hasItems(buyChipPromotions.get(0), buyChipPromotions.get(1)));
    }

    @Test(expected = NullPointerException.class)
    public void findByIdWithNullPromotionIdFails() {
        underTest.findById(PromotionSearchType.LIVE, null);
    }

    @Test(expected = NullPointerException.class)
    public void findByIdWithNullSearchTypeFails() {
        underTest.findById(null, 1l);
    }

    @Test
    public void findByIdShouldFindLivePromotionWithPriority() {
        // GIVEN known set of promotions
        List<Promotion> expectedPromotions = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE);
        final Promotion expectedPromoWithPriority = expectedPromotions.get(2);
        assertThat(expectedPromoWithPriority.getPriority(), notNullValue());

        //WHEN searching by id
        Promotion actualPromotion = underTest.findById(PromotionSearchType.LIVE, expectedPromoWithPriority.getId());

        //THEN correct promotion with id should be found
        assertThat(actualPromotion, is(expectedPromoWithPriority));
    }

    @Test
    public void findByIdShouldFindLivePromotionWithNullPrority() {
        // GIVEN known set of promotions
        List<Promotion> expectedPromotions = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE);
        final Promotion expectedPromoWithNullPriority = expectedPromotions.get(0);
        assertThat(expectedPromoWithNullPriority.getPriority(), nullValue());

        //WHEN searching by id
        Promotion actualPromotion = underTest.findById(PromotionSearchType.LIVE, expectedPromoWithNullPriority.getId());

        //THEN correct promotion with id should be found
        assertThat(actualPromotion, is(expectedPromoWithNullPriority));
    }

    @Test
    public void findByIdShouldFindArchivedPromotion() {
        // GIVEN known set of promotions
        List<Promotion> expectedPromotions = createKnownSetOfPromotions(generateArchivePromotions(), null).get(PromotionSearchType.ARCHIVED);

        //WHEN searching by id
        Promotion actualPromotion = underTest.findById(PromotionSearchType.ARCHIVED, expectedPromotions.get(1).getId());

        //THEN correct promotion with id should be found
        assertThat(actualPromotion, is(expectedPromotions.get(1)));
    }

    @Test
    public void findByIdShouldNotFindLivePromotionBySearchingForArchivedPromotion() {
        // GIVEN known set of promotions
        Map<PromotionSearchType, List<Promotion>> knownSetOfPromotions = createKnownSetOfPromotions(generateArchivePromotions(), generateLivePromotions());

        //WHEN searching for live promotion with archived id
        Long archivedPromotionId = knownSetOfPromotions.get(PromotionSearchType.ARCHIVED).get(0).getId();
        Promotion actualPromotion = underTest.findById(PromotionSearchType.LIVE, archivedPromotionId);

        //THEN correct no promotion should be found
        assertNull(actualPromotion);
    }

    @Test(expected = NullPointerException.class)
    public void findPlayersWithNullPromotionIdFails() {
        underTest.findPlayers(PromotionSearchType.LIVE, null, 0, 25);
    }

    @Test(expected = NullPointerException.class)
    public void findPlayersWithNullOffsetFails() {
        underTest.findPlayers(PromotionSearchType.LIVE, 23L, null, 25);
    }

    @Test(expected = NullPointerException.class)
    public void findPlayersWithNullNumberOfPlayersFails() {
        underTest.findPlayers(PromotionSearchType.LIVE, 24L, 0, null);
    }

    @Test
    public void findPlayersShouldReturnFirst3() {
        // GIVEN a know promotion and players
        List<Promotion> promotions = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE);
        Promotion promotion = promotions.get(0);
        List<PromotionPlayer> players = createPlayersForPromotion(PromotionSearchType.LIVE, promotion.getId(), 10);

        // WHEN requesting the first 3 players
        List<PromotionPlayer> actualPlayers = underTest.findPlayers(PromotionSearchType.LIVE, promotion.getId(), 0, 3);

        // THEN the first 3 players (ordered by lastRewardDate, number of times) are returned
        assertThat(actualPlayers, is(Arrays.asList(players.get(9), players.get(8), players.get(7))));
    }

    @Test
    public void findPlayersShouldReturnLast3() {
        // GIVEN a know promotion and players
        List<Promotion> promotions = createKnownSetOfPromotions(generateArchivePromotions(), null).get(PromotionSearchType.ARCHIVED);
        Promotion promotion = promotions.get(0);
        List<PromotionPlayer> players = createPlayersForPromotion(PromotionSearchType.ARCHIVED, promotion.getId(), 10);

        // WHEN requesting the first 3 players
        List<PromotionPlayer> actualPlayers = underTest.findPlayers(PromotionSearchType.ARCHIVED, promotion.getId(), 7, 3);

        // THEN the first 3 players (ordered by lastRewardDate, number of times) are returned
        assertThat(actualPlayers, is(Arrays.asList(players.get(2), players.get(1), players.get(0))));
    }

    @Test(expected = NullPointerException.class)
    public void countPlayersInPromotionWithNullPromotionIdFails() {
        underTest.countPlayersInPromotion(PromotionSearchType.LIVE, null);
    }

    @Test
    public void countPlayersInPromotionShouldReturnCount() {
        // GIVEN a know promotion and players
        List<Promotion> promotions = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE);
        Promotion promotion = promotions.get(0);
        List<PromotionPlayer> players = createPlayersForPromotion(PromotionSearchType.LIVE, promotion.getId(), 10);

        // WHEN counting players in promotion
        Integer actualCount = underTest.countPlayersInPromotion(PromotionSearchType.LIVE, promotion.getId());

        //THEN correct count should be returned
        assertEquals(10, actualCount.intValue());
    }

    @Test
    public void shouldReturnListOfExpiredPromotions() {
        // GIVEN Known set of promotions
        Map<PromotionSearchType, List<Promotion>> knownSetOfPromotions = createKnownSetOfPromotions(generateArchivePromotions(), generateLivePromotions());
        // AND current time of

        // WHEN querying expired promotions
        List<Long> expiredPromotions = underTest.findPromotionsOlderThan(28);

        // THEN expect promos with end time > 28 days ago
        assertThat(expiredPromotions, hasItems(-5L));
        assertThat(expiredPromotions, not(hasItems(-4L, -6L, -7L)));
    }

    @Test
    public void shouldReturnEmptyListOfExpiredPromotions() {
        // GIVEN Known set of promotions
        Map<PromotionSearchType, List<Promotion>> knownSetOfPromotions = createKnownSetOfPromotions(generateArchivePromotions(), generateLivePromotions());
        // AND current time of something way in the past
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().minusYears(10).getMillis());

        // WHEN querying expired promotions
        List<Long> expiredPromotions = underTest.findPromotionsOlderThan(28);

        // THEN expect promos
        assertEquals(0, expiredPromotions.size());
    }

    @Test(expected = NullPointerException.class)
    public void archivePromotionsWithNullPromotionIdFails() {
        underTest.archivePromotion(null);
    }

    @Test
    public void shouldArchivePromotionWithPriority() {
        // GIVEN a known promotion
        Promotion promotionToArchive = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE).get(2);
        assertThat(promotionToArchive.getPriority(), Matchers.notNullValue());
        Long promotionId = promotionToArchive.getId();
        // with players
        List<PromotionPlayer> playersInPromotion = createPlayersForPromotion(PromotionSearchType.LIVE, promotionId, 3);
        // and player rewards
        final DateTime now = new DateTime().withMillisOfSecond(0);
        final Object[][] playerRewards = {{promotionId, -1l, now, null, Boolean.FALSE},
                {promotionId, -1l, now.minusDays(2), null, Boolean.TRUE},
                {promotionId, -2l, now.minusYears(1), null, Boolean.TRUE}};
        addPlayerRewards(playerRewards);

        // WHEN copying live promo to archive
        underTest.archivePromotion(promotionId);

        // THEN the promotion and daily config should be copied
        Promotion archivedPromo = underTest.findById(PromotionSearchType.ARCHIVED, promotionId);
        assertThat(archivedPromo, is(promotionToArchive));

        // AND the promotion players should have been copied
        List<PromotionPlayer> archivedPromotionPlayers = underTest.findPlayers(PromotionSearchType.ARCHIVED, promotionId, 0, Integer.MAX_VALUE);
        assertTrue(archivedPromotionPlayers.containsAll(playersInPromotion));

        // AND player rewards should have been copied
        for (Object[] playerReward : playerRewards) {
            int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                    "and player_id = ? and rewarded_date = ?", playerReward[0], playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()));
            assertEquals(1, exists);
        }
    }

    @Test
    public void shouldArchivePromotionWithNullPriority() {
        // GIVEN a known promotion
        Promotion promotionToArchive = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE).get(0);
        assertThat(promotionToArchive.getPriority(), Matchers.nullValue());
        Long promotionId = promotionToArchive.getId();
        // with players
        List<PromotionPlayer> playersInPromotion = createPlayersForPromotion(PromotionSearchType.LIVE, promotionId, 3);
        // and player rewards
        final DateTime currentDateTime = new DateTime().withMillisOfSecond(0);
        final Object[][] playerRewards = {{promotionId, -1l, currentDateTime, "hello", Boolean.TRUE},
                {promotionId, -1l, currentDateTime.minusDays(2), null, Boolean.FALSE},
                {promotionId, -2l, currentDateTime.minusYears(1), null, Boolean.FALSE}};
        addPlayerRewards(playerRewards);

        // WHEN copying live promo to archive
        underTest.archivePromotion(promotionId);

        // THEN the promotion and daily config should be copied
        Promotion archivedPromo = underTest.findById(PromotionSearchType.ARCHIVED, promotionId);
        assertThat(archivedPromo, is(promotionToArchive));

        // AND the promotion players should have been copied
        List<PromotionPlayer> archivedPromotionPlayers = underTest.findPlayers(PromotionSearchType.ARCHIVED, promotionId, 0, Integer.MAX_VALUE);
        assertTrue(archivedPromotionPlayers.containsAll(playersInPromotion));

        // AND player rewards should have been copied
        for (Object[] playerReward : playerRewards) {
            int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                    "and player_id = ? and rewarded_date = ?", playerReward[0], playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()));
            assertEquals(1, exists);
        }
    }

    @Test
    public void shouldArchivePromotionWithPlayerRewards() {
        // GIVEN a known promotion
        Promotion promotionToArchive = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE).get(2);
        assertThat(promotionToArchive.getPriority(), Matchers.notNullValue());
        Long promotionId = promotionToArchive.getId();
        // with players
        List<PromotionPlayer> playersInPromotion = createPlayersForPromotion(PromotionSearchType.LIVE, promotionId, 3);
        // and player rewards
        final DateTime now = new DateTime().withMillisOfSecond(0);
        final Object[][] playerRewards = {{promotionId, -1l, now, "details", Boolean.FALSE},
                {promotionId, -1l, now.minusDays(2), null, Boolean.TRUE},
                {promotionId, -2l, now.minusYears(1), null, Boolean.FALSE}};
        addPlayerRewards(playerRewards);

        // WHEN copying live promo to archive
        underTest.archivePromotion(promotionId);

        // THEN the promotion and daily config should be copied
        Promotion archivedPromo = underTest.findById(PromotionSearchType.ARCHIVED, promotionId);
        assertThat(archivedPromo, is(promotionToArchive));

        // AND the promotion players should have been copied
        List<PromotionPlayer> archivedPromotionPlayers = underTest.findPlayers(PromotionSearchType.ARCHIVED, promotionId, 0, Integer.MAX_VALUE);
        assertTrue(archivedPromotionPlayers.containsAll(playersInPromotion));

        // AND player rewards should have been copied
        for (Object[] playerReward : playerRewards) {
            if (playerReward[3] == null) {
                int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                        "and player_id = ? and rewarded_date = ? and details is null",
                        playerReward[0],  playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()));
                assertEquals(1, exists);
            } else {
                int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                        "and player_id = ? and rewarded_date = ? and details = ?",
                         playerReward[0], playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()), playerReward[3]);
                assertEquals(1, exists);
            }
        }
    }

    @Test
    public void archivingAPromotionAlreadyArchivedShouldLeaveArchivedPromotionUnchanged() {
        // GIVEN a archived promotion
        Promotion promotionToArchive = createKnownSetOfPromotions(null, generateLivePromotions()).get(PromotionSearchType.LIVE).get(2);
        assertThat(promotionToArchive.getPriority(), Matchers.notNullValue());
        Long promotionId = promotionToArchive.getId();
        // with players
        List<PromotionPlayer> playersInPromotion = createPlayersForPromotion(PromotionSearchType.LIVE, promotionId, 3);
        // and player rewards
        final DateTime now = new DateTime().withMillisOfSecond(0);
        final Object[][] playerRewards = {{promotionId, -1l, now, "details", Boolean.FALSE},
                {promotionId, -1l, now.minusDays(2), null, Boolean.TRUE},
                {promotionId, -2l, now.minusYears(1), null, Boolean.FALSE}};
        addPlayerRewards(playerRewards);
        underTest.archivePromotion(promotionId);

        // when re archiving the promotion
        underTest.archivePromotion(promotionId);

        // THEN the archived promotion should remain unchanged
        Promotion archivedPromo = underTest.findById(PromotionSearchType.ARCHIVED, promotionId);
        assertThat(archivedPromo, is(promotionToArchive));
        // AND the promotion players should remain the same
        List<PromotionPlayer> archivedPromotionPlayers = underTest.findPlayers(PromotionSearchType.ARCHIVED, promotionId, 0, Integer.MAX_VALUE);
        assertTrue(archivedPromotionPlayers.containsAll(playersInPromotion));
        // AND player rewards should remain the same
        for (Object[] playerReward : playerRewards) {
            if (playerReward[3] == null) {
                int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                        "and player_id = ? and rewarded_date = ? and details is null",
                        playerReward[0],  playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()));
                assertEquals(1, exists);
            } else {
                int exists = dwJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD_ARCHIVE where promo_id = ? " +
                        "and player_id = ? and rewarded_date = ? and details = ?",
                         playerReward[0],  playerReward[1], new Timestamp(((DateTime) playerReward[2]).getMillis()), playerReward[3]);
                assertEquals(1, exists);
            }
        }
    }

    private void addPlayerRewards(final Object[][] playerRewards) {
        jdbcTemplate.batchUpdate(
                "insert into PROMOTION_PLAYER_REWARD(promo_id, player_id, rewarded_date, details, control_group) "
                        + "values(?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setLong(1, (Long) playerRewards[i][0]);
                        ps.setLong(2, (Long) playerRewards[i][1]);
                        ps.setTimestamp(3, new Timestamp(((DateTime) playerRewards[i][2]).getMillis()));
                        ps.setString(4, (String) playerRewards[i][3]);
                        ps.setBoolean(5, (Boolean) playerRewards[i][4]);
                    }

                    public int getBatchSize() {
                        return playerRewards.length;
                    }
                });
    }

    private List<PromotionPlayer> createPlayersForPromotion(PromotionSearchType searchType, Long promotionId, int numberOfplayers) {
        final List<PromotionPlayer> players = new ArrayList<PromotionPlayer>(numberOfplayers);
        for (long i = 1; i <= numberOfplayers; i++) {
            PromotionPlayer player = new PromotionPlayer();
            player.setPromotionId(promotionId);
            player.setPlayerId(BigInteger.valueOf(-i));
            players.add(player);
        }
        if (PromotionSearchType.LIVE == searchType) {
            jdbcTemplate.batchUpdate(
                    "insert into strataprod.PROMOTION_PLAYER(promo_id, player_id) values(?,?)",
                    new BatchPreparedStatementSetter() {
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setLong(1, players.get(i).getPromotionId());
                            ps.setLong(2, players.get(i).getPlayerId().longValue());
                        }

                        public int getBatchSize() {
                            return players.size();
                        }
                    });
        } else {
            dwJdbcTemplate.batchUpdate(
                    "insert into PROMOTION_PLAYER_ARCHIVE(promo_id, player_id) values(?,?)",
                    new BatchPreparedStatementSetter() {
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setLong(1, players.get(i).getPromotionId());
                            ps.setLong(2, players.get(i).getPlayerId().longValue());
                        }

                        public int getBatchSize() {
                            return players.size();
                        }
                    });
        }
        return players;
    }

    private List<Promotion> generateLivePromotions() {
        Promotion p4 = new DailyAwardPromotionBuilder().withId(-4L)
                .withPriority(null)
                .withStartDate(new DateTime().minusDays(1).withMillisOfSecond(0))
                .withEndDate(new DateTime().plusMinutes(1).withMillisOfSecond(0))
                .getPromotion();
        Promotion p5 = new DailyAwardPromotionBuilder().withId(-5L)
                .withStartDate(new DateTime().minusDays(26).withMillisOfSecond(0))
                .withEndDate(new DateTime().minusDays(29).withMillisOfSecond(0))
                .getPromotion();
        // this one actually has priority whoa
        Promotion p6 = new DailyAwardPromotionBuilder()
                .withId(-6L).withStartDate(new DateTime().minusDays(10).withMillisOfSecond(0))
                .withEndDate(new DateTime().plusHours(1).withMillisOfSecond(0))
                .withPriority(89).getPromotion();
        Promotion p7 = new BuyChipsPromotionBuilder()
                .withId(-7L)
                .withStartDate(new DateTime().minusDays(34).withMillisOfSecond(0))
                .withEndDate(new DateTime().plusMinutes(65).withMillisOfSecond(0))
                .getPromotion();
        return Arrays.asList(p4, p5, p6, p7);
    }

    private List<Promotion> generateArchivePromotions() {
        Promotion p1 = new DailyAwardPromotionBuilder().withId(-1L)
                .withStartDate(new DateTime().minusDays(10).withMillisOfSecond(0))
                .withEndDate(new DateTime().minusDays(1).withMillisOfSecond(0))
                .getPromotion();
        Promotion p2 = new DailyAwardPromotionBuilder()
                .withId(-2L)
                .withStartDate(new DateTime().minusDays(21).withMillisOfSecond(0))
                .withEndDate(new DateTime().minusDays(2).withMillisOfSecond(0))
                .getPromotion();
        Promotion p3 = new BuyChipsPromotionBuilder().withId(-3L)
                .withStartDate(new DateTime().minusDays(10).withMillisOfSecond(0))
                .withEndDate(new DateTime().minusDays(1).withMillisOfSecond(0))
                .getPromotion();
        Promotion p4 = new BuyChipsPromotionBuilder()
                .withId(-4L)
                .withStartDate(new DateTime().minusDays(21).withMillisOfSecond(0))
                .withEndDate(new DateTime().minusDays(2).withMillisOfSecond(0))
                .getPromotion();
        return Arrays.asList(p1, p2, p3, p4);
    }

    private void filterByPromotionType(List<Promotion> promotions, final PromotionType promotionType) {
        org.apache.commons.collections.CollectionUtils.filter(promotions, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((Promotion) object).getPromotionType() == promotionType;

            }
        });
    }

    private void filterNonTestData(List<Promotion> promotions) {
        org.apache.commons.collections.CollectionUtils.filter(promotions, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((Promotion) object).getId() < 0;

            }
        });
    }

    private Map<PromotionSearchType, List<Promotion>> createKnownSetOfPromotions(final List<Promotion> archivePromotions, final List<Promotion> livePromotions) {
        if (!CollectionUtils.isEmpty(archivePromotions)) {
            dwJdbcTemplate.batchUpdate(
                    PROMOTION_ARCHIVE_INSERT_SQL,
                    new BatchPreparedStatementSetter() {
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            final Promotion archivedPromotion = archivePromotions.get(i);
                            ps.setLong(1, archivedPromotion.getId());
                            ps.setString(2, archivedPromotion.getName());
                            ps.setString(3, archivedPromotion.getPlatformsAsString());
                            ps.setTimestamp(4, new Timestamp(archivedPromotion.getStartDate().getMillis()));
                            ps.setTimestamp(5, new Timestamp(archivedPromotion.getEndDate().getMillis()));
                            ps.setBoolean(6, archivedPromotion.isAllPlayers());
                            ps.setInt(7, archivedPromotion.getPlayerCount());
                            ps.setString(8, archivedPromotion.getPromotionType().name());
                            if (archivedPromotion.getPriority() == null) {
                                ps.setNull(9, Types.INTEGER);
                            } else {
                                ps.setInt(9, archivedPromotion.getPriority());
                            }
                            ps.setInt(10, archivedPromotion.getSeed());
                            ps.setInt(11, archivedPromotion.getControlGroupPercentage());
                            ps.setString(12, archivedPromotion.getControlGroupFunction().name());
                        }

                        public int getBatchSize() {
                            return archivePromotions.size();
                        }
                    });
            for (Promotion archivePromotion : archivePromotions) {
                createDailyAwardConfiguration(archivePromotion.getId(), archivePromotion.getConfiguration(), PromotionSearchType.ARCHIVED);
            }
        }
        if (!CollectionUtils.isEmpty(livePromotions)) {
            jdbcTemplate.batchUpdate(PROMOTION_INSERT_SQL,
                    new BatchPreparedStatementSetter() {
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            final Promotion livePromotion = livePromotions.get(i);
                            ps.setLong(1, livePromotion.getId());
                            ps.setString(2, livePromotion.getName());
                            ps.setString(3, livePromotion.getPlatformsAsString());
                            ps.setTimestamp(4, new Timestamp(livePromotion.getStartDate().getMillis()));
                            ps.setTimestamp(5, new Timestamp(livePromotion.getEndDate().getMillis()));
                            ps.setBoolean(6, livePromotion.isAllPlayers());
                            ps.setInt(7, livePromotion.getPlayerCount());
                            ps.setString(8, livePromotion.getPromotionType().name());
                            if (livePromotion.getPriority() == null) {
                                ps.setNull(9, Types.INTEGER);
                            } else {
                                ps.setInt(9, livePromotion.getPriority());
                            }
                            ps.setInt(10, livePromotion.getSeed());
                            ps.setInt(11, livePromotion.getControlGroupPercentage());
                            ps.setString(12, livePromotion.getControlGroupFunction().name());
                        }

                        public int getBatchSize() {
                            return livePromotions.size();
                        }
                    });
            for (Promotion livePromotion : livePromotions) {
                createDailyAwardConfiguration(livePromotion.getId(), livePromotion.getConfiguration(), PromotionSearchType.LIVE);
            }
        }
        Map<PromotionSearchType, List<Promotion>> promotions = new HashMap<PromotionSearchType, List<Promotion>>();
        promotions.put(PromotionSearchType.LIVE, livePromotions);
        promotions.put(PromotionSearchType.ARCHIVED, archivePromotions);
        return promotions;
    }

    private void createDailyAwardConfiguration(final Long promotionId, final PromotionConfiguration promotionConfiguration, PromotionSearchType searchType) {
        if (promotionConfiguration == null) {
            return;
        }
        Set<Map.Entry<String, String>> entries = promotionConfiguration.getConfiguration().entrySet();
        String[] stmts = new String[entries.size()];

        int i = 0;
        for (Map.Entry<String, String> entry : entries) {
            stmts[i++] = String.format("insert into " +
                    (searchType == PromotionSearchType.LIVE ? "PROMOTION_CONFIG " : "PROMOTION_CONFIG_ARCHIVE ") +
                    "(promo_id, config_key, config_value) values (%s,'%s','%s')", promotionId, entry.getKey(), entry.getValue());
        }
        if (stmts.length > 0) {
            if (PromotionSearchType.LIVE == searchType) {
                jdbcTemplate.batchUpdate(stmts);
            } else {
                dwJdbcTemplate.batchUpdate(stmts);
            }
        }
    }
}
