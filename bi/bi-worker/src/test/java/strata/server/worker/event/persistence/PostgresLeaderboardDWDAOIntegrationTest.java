package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.LeaderboardEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAPlayer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class PostgresLeaderboardDWDAOIntegrationTest {

    private static final BigDecimal ID_1 = BigDecimal.valueOf(-1000);
    private static final BigDecimal ID_2 = BigDecimal.valueOf(-2000);
    private static final String DEFAULT_GAME_TYPE = "gameType";
    private static final BigDecimal PLAYER_1_ID = BigDecimal.valueOf(-1);
    private static final BigDecimal PLAYER_2_ID = BigDecimal.valueOf(-2);

    private static final Integer DEFAULT_POSITION_PLAYER_1 = 1;
    private static final Integer DEFAULT_POSITION_PLAYER_2 = 2;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresLeaderboardDWDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM LEADERBOARD ");
        jdbc.update("DELETE FROM LEADERBOARD_POSITION ");
        createAPlayer(jdbc, PLAYER_1_ID, PLAYER_2_ID);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresLeaderboardDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aLeaderboardEvent(ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresLeaderboardDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aLeaderboardEvent(ID_1)));
    }

    @Test
    public void aLeaderboardEventSavesALeaderboardToTheDatabase() {
        underTest.saveAll(newArrayList(aLeaderboardEvent(ID_1)));

        verifyLeaderboardRecordMatches(aLeaderboardEvent(ID_1), readLeaderboardById(ID_1));
    }

    @Test
    public void leaderboardEventMayContainMultiplePositions() {
        LeaderboardEvent event = aLeaderboardEvent(ID_1);
        underTest.saveAll(newArrayList(event));
        verifyEntireLeaderboardWasSaved(event);
    }

    @Test
    public void multipleLeaderboardEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aLeaderboardEvent(ID_1), aLeaderboardEvent(ID_2)));

        verifyEntireLeaderboardWasSaved(aLeaderboardEvent(ID_1));
        verifyEntireLeaderboardWasSaved(aLeaderboardEvent(ID_2));
    }

    private void verifyEntireLeaderboardWasSaved(final LeaderboardEvent event) {
        Map<String, Object> board = readLeaderboardById(event.getLeaderboardId());
        verifyLeaderboardRecordMatches(event, board);

        // Now verify all the positions too
        List<Map<String, Object>> positions = readPositionsByLeaderboardId(event.getLeaderboardId());
        assertThat(positions.size(), is(equalTo(event.getPlayerPositions().size())));
        for (Map<String, Object> p : positions) {
            BigDecimal playerId = new BigDecimal(p.get("PLAYER_ID").toString());
            Integer savedPosition = (Integer) p.get("LEADERBOARD_POSITION");
            BigDecimal expectedPlayer = event.getPlayerPositions().get(savedPosition);
            assertThat(playerId, is(comparesEqualTo(expectedPlayer)));
        }
    }

    private LeaderboardEvent aLeaderboardEvent(final BigDecimal id) {
        Map<Integer, BigDecimal> positions = new HashMap<Integer, BigDecimal>();
        positions.put(DEFAULT_POSITION_PLAYER_1, PLAYER_1_ID);
        positions.put(DEFAULT_POSITION_PLAYER_2, PLAYER_2_ID);
        return new LeaderboardEvent(id, DEFAULT_GAME_TYPE, new DateTime(), positions);
    }

    private void verifyLeaderboardRecordMatches(final LeaderboardEvent leaderboardEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("LEADERBOARD_ID").toString()), is(comparesEqualTo(leaderboardEvent.getLeaderboardId())));
        assertThat(record.get("GAME_TYPE").toString(), is(equalTo(leaderboardEvent.getGameType())));
    }

    private Map<String, Object> readLeaderboardById(final BigDecimal leaderboardId) {
        return jdbc.queryForMap("SELECT * FROM LEADERBOARD WHERE LEADERBOARD_ID=?", leaderboardId);
    }

    private List<Map<String, Object>> readPositionsByLeaderboardId(final BigDecimal leaderboardId) {
        return jdbc.queryForList("SELECT * FROM LEADERBOARD_POSITION WHERE LEADERBOARD_ID=?", leaderboardId);
    }
}

