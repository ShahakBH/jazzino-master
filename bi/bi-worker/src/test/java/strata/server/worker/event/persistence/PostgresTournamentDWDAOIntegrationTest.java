package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.TournamentPlayerSummary;
import com.yazino.platform.event.message.TournamentSummaryEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAPlayer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class PostgresTournamentDWDAOIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PostgresTournamentDWDAO underTest;

    private static final BigDecimal TOURNAMENT_ID_1 = BigDecimal.valueOf(-1);
    private static final BigDecimal TOURNAMENT_ID_2 = BigDecimal.valueOf(-2);
    private static final BigDecimal DEFAULT_TEMPLATE_ID = BigDecimal.TEN;
    private static final String DEFAULT_GAME_TYPE = "aGameType";
    private static final TournamentPlayerSummary PLAYER_ID_1 = new TournamentPlayerSummary(BigDecimal.ONE, 1, BigDecimal.TEN);
    private static final TournamentPlayerSummary PLAYER_ID_2 = new TournamentPlayerSummary(BigDecimal.TEN, 1, BigDecimal.ONE);
    private static final String TOURNAMENT_NAME = "TournamentName";

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM TOURNAMENT_SUMMARY WHERE TOURNAMENT_ID IN (?,?)", TOURNAMENT_ID_1, TOURNAMENT_ID_2);
        jdbc.update("DELETE FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID IN (?,?)", TOURNAMENT_ID_1, TOURNAMENT_ID_2);
        jdbc.update("DELETE FROM TOURNAMENT WHERE TOURNAMENT_ID IN (?,?)", TOURNAMENT_ID_1, TOURNAMENT_ID_2);
        jdbc.update("DELETE FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID =?", DEFAULT_TEMPLATE_ID);
        createAPlayer(jdbc, BigDecimal.ONE, BigDecimal.TEN);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheExceptionForTournamentSummaryEvent() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresTournamentDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.save(newHashSet(aTournamentSummaryEvent(TOURNAMENT_ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheExceptionForTournamentSummaryEvent() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresTournamentDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.save(newHashSet(aTournamentSummaryEvent(TOURNAMENT_ID_1)));
    }

    @Test
    public void tournamentSummaryIsSavedAcrossMultipleTables() throws Exception {
        TournamentSummaryEvent tournamentSummaryEvent = aTournamentSummaryEvent(TOURNAMENT_ID_1);
        underTest.save(newHashSet(tournamentSummaryEvent));

        /* TOURNAMENT table */
        Timestamp startTime = (Timestamp) readByTournamentId(TOURNAMENT_ID_1).get("TOURNAMENT_START_TS");
        checkDateTime(tournamentSummaryEvent.getStartTs(), startTime);

        /* TOURNAMENT_PLAYER table */
        List<Map<String,Object>> playersRecord = readTournamentPlayerByTournamentId(TOURNAMENT_ID_1);
        for (Map<String, Object> player : playersRecord) {
            assertThat(new BigDecimal(player.get("TOURNAMENT_ID").toString()), is(comparesEqualTo(tournamentSummaryEvent.getTournamentId())));
            boolean found = false;
            for (TournamentPlayerSummary playerSummary : tournamentSummaryEvent.getPlayers()) {
                if (playerSummary.getId().compareTo(new BigDecimal(player.get("PLAYER_ID").toString()))==0) {
                    found = true;
                }
            }
            assertTrue("Player " + player.get("PLAYER_ID") + " was not found", found);
        }
        assertThat(playersRecord.size(), is(equalTo(tournamentSummaryEvent.getPlayers().size())));

        /* TOURNAMENT_VARIATION_TEMPLATE table */
        Map<String, Object> tvtRecord = readVariationTemplateById(tournamentSummaryEvent.getTemplateId());
        assertThat((String) tvtRecord.get("GAME_TYPE"), is(equalTo(tournamentSummaryEvent.getGameType())));

        /* TOURNAMENT_SUMMARY table */
        Map<String, Object> tournamentSummaryRecord = readTournamentSummaryById(TOURNAMENT_ID_1);
        assertThat(tournamentSummaryEvent.getTournamentId(), is(comparesEqualTo(new BigDecimal(tournamentSummaryRecord.get("TOURNAMENT_ID").toString()))));
        assertThat(tournamentSummaryEvent.getTournamentName(), is(equalTo((String) tournamentSummaryRecord.get("TOURNAMENT_NAME"))));
        Timestamp completionTime = (Timestamp) tournamentSummaryRecord.get("TOURNAMENT_FINISHED_TS");
        checkDateTime(tournamentSummaryEvent.getFinishedTs(), completionTime);
    }


    @Test
    public void tournamentSummaryUpdatesExistingTournamentVariations() throws Exception {
        jdbc.update("INSERT INTO TOURNAMENT_VARIATION_TEMPLATE (TOURNAMENT_VARIATION_TEMPLATE_ID,GAME_TYPE) VALUES (?,?)", DEFAULT_TEMPLATE_ID, DEFAULT_GAME_TYPE);

        TournamentSummaryEvent tournamentSummaryEvent = aTournamentSummaryEvent(TOURNAMENT_ID_1, new Date(), "aNewGameType");
        underTest.save(newHashSet(tournamentSummaryEvent));

        Map<String, Object> tvtRecord = readVariationTemplateById(tournamentSummaryEvent.getTemplateId());
        assertThat((String) tvtRecord.get("GAME_TYPE"), is(equalTo("aNewGameType")));
    }

    private void checkDateTime(final DateTime expected, final Timestamp actual) {
        DateTime dateTime = new DateTime(actual);
        dateTime = dateTime.minus(dateTime.getMillisOfSecond());
        DateTime rounded = expected.minusMillis(expected.getMillisOfSecond()); // remove millis, because TimeStamp does not same millis
        assertEquals(rounded, dateTime);
    }

    private TournamentSummaryEvent aTournamentSummaryEvent(BigDecimal tournamentId) {
        return aTournamentSummaryEvent(tournamentId, new Date(), DEFAULT_GAME_TYPE);
    }

    private TournamentSummaryEvent aTournamentSummaryEvent(BigDecimal tournamentId, Date completionTime, final String gameType) {
        return new TournamentSummaryEvent(tournamentId, TOURNAMENT_NAME, DEFAULT_TEMPLATE_ID, gameType, new DateTime(), new DateTime(completionTime.getTime()), asList(PLAYER_ID_1, PLAYER_ID_2));
    }

    private Map<String, Object> readByTournamentId(final BigDecimal tournamentId) {
        return jdbc.queryForMap("SELECT * FROM TOURNAMENT WHERE TOURNAMENT_ID=?", tournamentId);
    }

    private List<Map<String, Object>> readTournamentPlayerByTournamentId(final BigDecimal tournamentId) {
        return jdbc.queryForList("SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=?", tournamentId);
    }
    
    private Map<String, Object> readVariationTemplateById(final BigDecimal templateId) {
        return jdbc.queryForMap("SELECT * FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?", templateId);
    }

    private Map<String, Object> readTournamentSummaryById(final BigDecimal templateId) {
        return jdbc.queryForMap("SELECT * FROM TOURNAMENT_SUMMARY WHERE TOURNAMENT_ID=?", templateId);
    }
}
