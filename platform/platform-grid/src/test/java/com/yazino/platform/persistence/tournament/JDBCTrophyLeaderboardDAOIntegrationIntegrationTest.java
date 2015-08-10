package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class JDBCTrophyLeaderboardDAOIntegrationIntegrationTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(9999999L);
    private static final BigDecimal LEADERBOARD2_ID = BigDecimal.valueOf(7777777L);
    private static final BigDecimal LEADERBOARD3_ID = BigDecimal.valueOf(8888888L);
    private static final BigDecimal LEADERBOARD4_ID = BigDecimal.valueOf(6666666L);
    private static final String SELECT_LEADERBOARD_PLAYERS = "SELECT * FROM LEADERBOARD_PLAYER WHERE LEADERBOARD_ID=? ORDER BY PLAYER_POINTS DESC";
    private static final String SELECT_LEADERBOARD_POSITIONS
            = "SELECT * FROM LEADERBOARD_POSITION WHERE LEADERBOARD_ID=? ORDER BY LEADERBOARD_POSITION ASC";
    private static final String SELECT_LEADERBOARD = "SELECT * FROM LEADERBOARD WHERE LEADERBOARD_ID=?";
    private static final int NUMBER_OF_PLAYERS = 5;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired(required = true)
    private TrophyLeaderboardDao trophyLeaderboardDAO;

    @Value("${strata.server.lobby.ssl.content}")
    private String contentUrl;

    private TrophyLeaderboard trophyLeaderboard;
    private BigDecimal accountId;

    @Before
    public void setUp() {

        accountId = sequenceGenerator.next();
        jdbcTemplate.update("DELETE FROM LEADERBOARD");
        jdbcTemplate.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) VALUES (?,?)", accountId, "trophyTest-a");
        trophyLeaderboard = createTrophyLeaderboard(LEADERBOARD_ID);
    }

    private TrophyLeaderboard createTrophyLeaderboard(final BigDecimal leaderboardId) {
        final TrophyLeaderboard trophyLeaderboard = new TrophyLeaderboard(leaderboardId, "Test leaderboard", "BLACKJACK",
                new Interval(new DateTime(2009, 10, 1, 1, 1, 1, 0), new DateTime(2010, 10, 1, 1, 1, 1, 0)),
                new Period(Days.THREE).toStandardDuration());
        trophyLeaderboard.setCurrentCycleEnd(new DateTime(2009, 11, 1, 1, 1, 1, 0));
        trophyLeaderboard.setPointBonusPerPlayer(2L);

        final TrophyLeaderboardPlayers trophyLeaderboardPlayers = new TrophyLeaderboardPlayers();
        trophyLeaderboard.setPlayers(trophyLeaderboardPlayers);
        for (int i = NUMBER_OF_PLAYERS; i >= 1; --i) {
            final BigDecimal playerId = playerId(leaderboardId.intValue() + (i * 10));
            jdbcTemplate.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (?,?)", playerId, "YAZINO");
            jdbcTemplate.update("INSERT INTO PLAYER (PLAYER_ID,NAME,ACCOUNT_ID,PICTURE_LOCATION) VALUES (?,?,?,?)",
                    playerId, "trophyTest-p" + i, accountId, "%CONTENT%/picture" + i);
            trophyLeaderboardPlayers.addPlayer(
                    new TrophyLeaderboardPlayer(i, playerId, "trophyTest-p" + i, i * 100, "%CONTENT%/picture" + i));
        }


        for (int i = 1; i <= NUMBER_OF_PLAYERS; ++i) {
            final TrophyLeaderboardPosition position = new TrophyLeaderboardPosition(
                    i, pointsForPosition(i), payoutForPosition(i), null);
            trophyLeaderboard.addPosition(position);
        }

        return trophyLeaderboard;
    }

    @Test
    public void leaderboardIsCreatedIfDatabaseIfItDoesNotExist() {
        trophyLeaderboardDAO.save(trophyLeaderboard);

        verifyLeaderboard();
        verifyPlayers();
        verifyPositions();
    }

    @Test
    public void leaderboardIsUpdatedInTheDatabaseIfItAlreadyExists() {
        trophyLeaderboardDAO.save(trophyLeaderboard);

        trophyLeaderboard.setName("Test update");
        trophyLeaderboard.setPointBonusPerPlayer(4L);

        final int index = NUMBER_OF_PLAYERS + 1;
        jdbcTemplate.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (?,?)", playerId(index), "YAZINO");
        jdbcTemplate.update("INSERT INTO PLAYER (PLAYER_ID,NAME,ACCOUNT_ID) VALUES (?,?,?)",
                playerId(index), "trophyTest-p" + index, accountId);
        trophyLeaderboard.getOrderedByPosition().add(new TrophyLeaderboardPlayer(
                index, playerId(index), "trophyTest-p" + index * 100, 0, "picture" + index));

        trophyLeaderboard.getPositionData().remove(NUMBER_OF_PLAYERS); // remove last item

        assertThat(trophyLeaderboard.getOrderedByPosition().size(), is(equalTo(NUMBER_OF_PLAYERS + 1)));
        assertThat(trophyLeaderboard.getPositionData().size(), is(equalTo(NUMBER_OF_PLAYERS - 1)));

        trophyLeaderboardDAO.save(trophyLeaderboard);

        verifyLeaderboard();
        verifyPlayers();
        verifyPositions();
    }

    @Test
    public void inactiveLeaderboardIsNotMatchedByFindActive() {
        trophyLeaderboard.setActive(false);
        trophyLeaderboardDAO.save(trophyLeaderboard);

        final DataIterator<TrophyLeaderboard> dataIterator = ((JDBCTrophyLeaderboardDAO) trophyLeaderboardDAO).iterateAll();
        final List<TrophyLeaderboard> activeLeaderboards = toList(dataIterator);
        assertThat(activeLeaderboards.size(), is(equalTo(0)));
    }

    private <T> List<T> toList(final DataIterator<T> dataIterator) {
        final List<T> dataList = new ArrayList<T>();
        while (dataIterator.hasNext()) {
            dataList.add(dataIterator.next());
        }
        return dataList;
    }

    @Test
    public void activeLeaderboardIsMatchedByFinder() {
        trophyLeaderboardDAO.save(trophyLeaderboard);

        final DataIterator<TrophyLeaderboard> dataIterator = ((JDBCTrophyLeaderboardDAO) trophyLeaderboardDAO).iterateAll();
        final List<TrophyLeaderboard> activeLeaderboards = toList(dataIterator);
        assertThat(activeLeaderboards.size(), is(equalTo(1)));
        assertThat(activeLeaderboards, hasItems(trophyLeaderboard));
    }


    @Test
    public void leaderboardPlayersContainDetokenisedPictureUrls() {
        trophyLeaderboardDAO.save(trophyLeaderboard);

        final DataIterator<TrophyLeaderboard> dataIterator = ((JDBCTrophyLeaderboardDAO) trophyLeaderboardDAO).iterateAll();

        final List<TrophyLeaderboard> activeLeaderboards = toList(dataIterator);
        boolean found = false;
        for (TrophyLeaderboard activeLeaderboard : activeLeaderboards) {
            if (activeLeaderboard.getId().equals(trophyLeaderboard.getId())) {
                found = true;
                for (TrophyLeaderboardPlayer player : activeLeaderboard.getPlayers().getOrderedByPosition()) {
                    assertThat(player.getPictureUrl(),
                            is(equalTo(contentUrl + "/picture" + player.getLeaderboardPosition())));
                }
            }
        }

        if (!found) {
            fail("Expected leaderboard not found in leaderboards");
        }
    }

    @Test
    public void allActiveLeaderboardsAreMatchedByFinder() {
        final TrophyLeaderboard active1 = trophyLeaderboard = createTrophyLeaderboard(LEADERBOARD2_ID);
        final TrophyLeaderboard active2 = trophyLeaderboard = createTrophyLeaderboard(LEADERBOARD3_ID);

        final TrophyLeaderboard inactive = trophyLeaderboard = createTrophyLeaderboard(LEADERBOARD4_ID);
        inactive.setActive(false);

        trophyLeaderboardDAO.save(active1);
        trophyLeaderboardDAO.save(active2);
        trophyLeaderboardDAO.save(inactive);

        final DataIterator<TrophyLeaderboard> dataIterator = ((JDBCTrophyLeaderboardDAO) trophyLeaderboardDAO).iterateAll();
        final List<TrophyLeaderboard> activeLeaderboards = toList(dataIterator);
        assertThat(activeLeaderboards.size(), is(equalTo(2)));
        assertThat(activeLeaderboards, hasItems(active1));
        assertThat(activeLeaderboards, hasItems(active2));
    }

    private void verifyLeaderboard() {
        jdbcTemplate.query(SELECT_LEADERBOARD, new ResultSetExtractor<Object>() {
            @Override
            public Object extractData(final ResultSet rs) throws SQLException, DataAccessException {
                if (rs.next()) {
                    assertThat(rs.getBigDecimal("LEADERBOARD_ID"), is(equalTo(trophyLeaderboard.getId())));
                    assertThat(rs.getTimestamp("START_TS"), is(equalToDateTime(trophyLeaderboard.getStartTime())));
                    assertThat(rs.getTimestamp("END_TS"), is(equalToDateTime(trophyLeaderboard.getEndTime())));
                    assertThat(rs.getTimestamp("CYCLE_END_TS"), is(equalToDateTime(trophyLeaderboard.getCurrentCycleEnd())));
                    assertThat(rs.getLong("CYCLE_LENGTH"), is(equalTo(trophyLeaderboard.getCycle().getMillis())));
                    assertThat(rs.getBoolean("ACTIVE"), is(equalTo(trophyLeaderboard.getActive())));
                    assertThat(rs.getString("GAME_TYPE"), is(equalTo(trophyLeaderboard.getGameType())));
                    assertThat(rs.getLong("POINT_BONUS_PER_PLAYER"), is(equalTo(trophyLeaderboard.getPointBonusPerPlayer())));
                    assertThat(rs.getString("LEADERBOARD_NAME"), is(equalTo(trophyLeaderboard.getName())));
                }

                return null;
            }
        }, LEADERBOARD_ID);
    }

    private void verifyPlayers() {
        jdbcTemplate.query(SELECT_LEADERBOARD_PLAYERS, new ResultSetExtractor<Object>() {
            @Override
            public Object extractData(final ResultSet rs) throws SQLException, DataAccessException {
                int count = 0;
                while (rs.next()) {
                    final TrophyLeaderboardPlayer player = trophyLeaderboard.getOrderedByPosition().get(count);

                    assertThat(rs.getBigDecimal("LEADERBOARD_ID"), is(comparesEqualTo(trophyLeaderboard.getId())));
                    assertThat(rs.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(player.getPlayerId())));
                    assertThat(rs.getString("PLAYER_NAME"), is(equalTo(player.getPlayerName())));
                    assertThat(rs.getLong("PLAYER_POINTS"), is(equalTo(player.getPoints())));

                    ++count;
                }

                assertThat(count, is(equalTo(trophyLeaderboard.getOrderedByPosition().size())));

                return null;
            }
        }, LEADERBOARD_ID);
    }

    private void verifyPositions() {
        jdbcTemplate.query(SELECT_LEADERBOARD_POSITIONS, new ResultSetExtractor<Object>() {
            @Override
            public Object extractData(final ResultSet rs) throws SQLException, DataAccessException {
                int positionCount = 1;
                while (rs.next()) {
                    final TrophyLeaderboardPosition position = trophyLeaderboard.getPositionData().get(positionCount);

                    assertThat(rs.getBigDecimal("LEADERBOARD_ID"), is(equalTo(trophyLeaderboard.getId())));
                    assertThat(rs.getInt("LEADERBOARD_POSITION"), is(equalTo(position.getPosition())));
                    assertThat(rs.getLong("AWARD_POINTS"), is(equalTo(position.getAwardPoints())));
                    assertThat(rs.getLong("AWARD_PAYOUT"), is(equalTo(position.getAwardPayout())));
                    assertThat(rs.getBigDecimal("TROPHY_ID"), is(equalTo(position.getTrophyId())));

                    ++positionCount;
                }

                assertThat(positionCount - 1, is(equalTo(trophyLeaderboard.getPositionData().size())));

                return null;
            }
        }, LEADERBOARD_ID);
    }

    private Matcher<Timestamp> equalToDateTime(final DateTime dateTime) {
        return new TypeSafeMatcher<Timestamp>() {
            @Override
            public boolean matchesSafely(final Timestamp item) {
                if (item == null && dateTime == null) {
                    return true;
                } else if (item != null && dateTime != null && new Timestamp(dateTime.withMillisOfSecond(0).getMillis()).equals(item)) {
                    return true;
                }

                return false;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is equal to ").appendValue(dateTime);
            }
        };
    }

    private BigDecimal playerId(final int val) {
        return BigDecimal.valueOf(val);
    }

    private long pointsForPosition(final int position) {
        return Math.max((10 - (position - 1)) * 10, 0);
    }

    private long payoutForPosition(final int position) {
        return pointsForPosition(position) * 100;
    }

}
