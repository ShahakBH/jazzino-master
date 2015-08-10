package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTrophyLeaderboardResultDAOIntegrationTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(999999999L);
    private static final String SELECT_LEADERBOARD_RESULT
            = "SELECT LEADERBOARD_ID,RESULT_TS,PLAYER_ID,LEADERBOARD_POSITION,PLAYER_POINTS,PLAYER_PAYOUT,PLAYER_NAME,EXPIRY_TS "
            + "FROM LEADERBOARD_RESULT WHERE LEADERBOARD_ID=? ORDER BY LEADERBOARD_POSITION ASC";

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired(required = true)
    private TrophyLeaderboardResultDao trophyLeaderboardResultDAO;

    private DateTime timestamp;
    private List<TrophyLeaderboardPlayerResult> leaderboardPlayerResults;
    private TrophyLeaderboardResult leaderboardResult;

    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM LEADERBOARD_RESULT WHERE LEADERBOARD_ID = ?", LEADERBOARD_ID);
        jdbcTemplate.update("DELETE FROM LEADERBOARD WHERE LEADERBOARD_ID = ?", LEADERBOARD_ID);
        jdbcTemplate.update("DELETE FROM PLAYER WHERE NAME LIKE ?", "trophyTest-p%");
        jdbcTemplate.update("DELETE FROM ACCOUNT WHERE NAME LIKE ?", "trophyTest-a");
    }

    @Before
    @Transactional
    public void setUp() {
        timestamp = new DateTime(2009, 12, 26, 1, 43, 12, 0);

        leaderboardPlayerResults = new ArrayList<TrophyLeaderboardPlayerResult>();

        final BigDecimal accountId = sequenceGenerator.next();
        jdbcTemplate.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) VALUES (?,?)", accountId, "trophyTest-a");

        final int numberOfPlayers = 4;
        for (int i = 1; i <= numberOfPlayers; ++i) {
            final BigDecimal playerId = playerId(9999999 + (i * 10));
            jdbcTemplate.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (?,?)", playerId, "YAZINO");
            jdbcTemplate.update("INSERT INTO PLAYER (PLAYER_ID,NAME,ACCOUNT_ID) VALUES (?,?,?)",
                    playerId, "trophyTest-p" + i, accountId);
            leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(playerId, "trophyTest-p" + i,
                    pointsForPosition(i) + bonusPointsWithPlayers(numberOfPlayers), BigDecimal.valueOf(payoutForPosition(i)), i));
        }

        jdbcTemplate.update("INSERT INTO LEADERBOARD (LEADERBOARD_ID,START_TS,END_TS,CYCLE_LENGTH,CYCLE_END_TS,ACTIVE,GAME_TYPE,POINT_BONUS_PER_PLAYER,LEADERBOARD_NAME) VALUES (?,?,?,?,?,?,?,?,?)",
                LEADERBOARD_ID, null, null, 7, null, true, "TEXAS_HOLDEM", 1, "Texas Holdem Leaderboard");

        leaderboardResult = new TrophyLeaderboardResult(
                LEADERBOARD_ID, timestamp, timestamp.plus(Weeks.weeks(1)), leaderboardPlayerResults);
    }

    @Test
    @Transactional
    public void saveInsertsARowForEachPlayerInResultTable() {
        trophyLeaderboardResultDAO.save(leaderboardResult);

        jdbcTemplate.query(SELECT_LEADERBOARD_RESULT, new Object[]{LEADERBOARD_ID}, new ResultSetExtractor() {
            @Override
            public Object extractData(final ResultSet rs) throws SQLException, DataAccessException {
                int results = 0;

                while (rs.next()) {
                    final TrophyLeaderboardPlayerResult result = leaderboardPlayerResults.get(results);
                    assertThat(rs.getBigDecimal("LEADERBOARD_ID"), comparesEqualTo(LEADERBOARD_ID));
                    assertThat(rs.getTimestamp("RESULT_TS"), is(equalTo(new Timestamp(timestamp.getMillis()))));
                    assertThat(rs.getTimestamp("EXPIRY_TS"), is(equalTo(
                            new Timestamp(timestamp.getMillis() + Weeks.weeks(1).toStandardDuration().getMillis()))));
                    assertThat(rs.getBigDecimal("PLAYER_ID"), is(comparesEqualTo(result.getPlayerId())));
                    assertThat(rs.getLong("PLAYER_POINTS"), is(equalTo(result.getPoints())));
                    assertThat(rs.getBigDecimal("PLAYER_PAYOUT"), is(equalTo(result.getPayout())));
                    assertThat(rs.getString("PLAYER_NAME"), is(equalTo(result.getPlayerName())));

                    ++results;
                }

                assertThat(results, is(equalTo(leaderboardPlayerResults.size())));

                return null;
            }
        });
    }

    @Test
    @Transactional
    public void checkNoInteractionWithDatabaseWhenNoPlayerResults() throws Exception {
        leaderboardResult.getPlayerResults().clear();
        trophyLeaderboardResultDAO.save(leaderboardResult);
    }

    private BigDecimal playerId(final int val) {
        return BigDecimal.valueOf(val);
    }

    private long bonusPointsWithPlayers(final int players) {
        return players;
    }

    private long pointsForPosition(final int position) {
        return Math.max((10 - (position - 1)) * 10, 0);
    }

    private long payoutForPosition(final int position) {
        return pointsForPosition(position) * 100;
    }
}
