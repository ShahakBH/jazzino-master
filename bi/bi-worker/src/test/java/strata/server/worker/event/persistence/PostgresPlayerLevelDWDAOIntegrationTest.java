package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.PlayerLevelEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import utils.PostgresTestValueHelper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresPlayerLevelDWDAOIntegrationTest {

    private static final String PLAYER_ID_1 = "1";
    private static final String PLAYER_ID_2 = "2";
    private static final String DEFAULT_GAME_TYPE = "1";
    private static final String DEFAULT_LEVEL = "2";

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;


    @Autowired
    private PostgresPlayerLevelDWDAO underTest;

    @Before
    public void setup() {
        jdbc.update("DELETE FROM PLAYER_LEVEL WHERE PLAYER_ID IN (?,?)", new BigDecimal(PLAYER_ID_1), new BigDecimal(PLAYER_ID_2));
        PostgresTestValueHelper.createAPlayer(jdbc, new BigDecimal(PLAYER_ID_1), new BigDecimal(PLAYER_ID_2));
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerLevelDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerLevelEvent(PLAYER_ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {

        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerLevelDWDAO(mockTemplate);
        when(mockTemplate.update(Mockito.anyString(), Mockito.<PreparedStatementSetter>any())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerLevelEvent(PLAYER_ID_1)));
    }

    @Test
    public void aPlayerEventIsSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerLevelEvent(PLAYER_ID_1)));

        verifyRecordMatches(aPlayerLevelEvent(PLAYER_ID_1), readRecordByPlayerId(PLAYER_ID_1));
    }

    @Test
    public void multiplePlayerLevelsEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerLevelEvent(PLAYER_ID_1), aPlayerLevelEvent(PLAYER_ID_2)));

        verifyRecordMatches(aPlayerLevelEvent(PLAYER_ID_1), readRecordByPlayerId(PLAYER_ID_1));
        verifyRecordMatches(aPlayerLevelEvent(PLAYER_ID_2), readRecordByPlayerId(PLAYER_ID_2));
    }

    @Test
    public void playerLevelsGainedUpdateTheRecordInTheDatabase() {

        PlayerLevelEvent firstPlayerLevelEvent = new PlayerLevelEvent(PLAYER_ID_1, DEFAULT_GAME_TYPE, DEFAULT_LEVEL);
        PlayerLevelEvent secondPlayerLevelEvent = new PlayerLevelEvent(PLAYER_ID_1, DEFAULT_GAME_TYPE, DEFAULT_LEVEL + 1);

        underTest.saveAll(newArrayList(firstPlayerLevelEvent));
        underTest.saveAll(newArrayList(secondPlayerLevelEvent));

        Map<String, Object> countOfRecords = jdbc.queryForMap("SELECT COUNT(*) AS num FROM PLAYER_LEVEL WHERE PLAYER_ID=?", new BigDecimal(PLAYER_ID_1));
        assertThat(Integer.valueOf(countOfRecords.get("num").toString()), equalTo(1));
        verifyRecordMatches(secondPlayerLevelEvent, readRecordByPlayerId(PLAYER_ID_1));
    }

    private PlayerLevelEvent aPlayerLevelEvent(final String id) {
        return new PlayerLevelEvent(id, DEFAULT_GAME_TYPE, DEFAULT_LEVEL);
    }

    private void verifyRecordMatches(final PlayerLevelEvent playerEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("PLAYER_ID").toString()), is(comparesEqualTo(new BigDecimal(playerEvent.getPlayerId()))));
        assertThat(record.get("GAME_TYPE").toString(), is(equalTo(playerEvent.getGameType())));
        assertThat(record.get("LEVEL").toString(), is(equalTo(playerEvent.getLevel())));
    }

    private Map<String, Object> readRecordByPlayerId(final String playerId) {
        return jdbc.queryForMap("SELECT * FROM PLAYER_LEVEL WHERE PLAYER_ID=?", new BigDecimal(playerId));
    }
}

