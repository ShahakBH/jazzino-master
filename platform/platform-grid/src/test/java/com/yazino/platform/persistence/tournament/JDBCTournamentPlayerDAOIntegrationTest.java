package com.yazino.platform.persistence.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTournamentPlayerDAOIntegrationTest {

    // this needs to use the interface to avoid autowiring errors from the proxies
    @Autowired(required = true)
    private TournamentPlayerDao jdbcTournamentPlayerDAO;

    @Autowired(required = true)
    private TournamentDao jdbcTournamentDAO;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    private static final String PLAYER_NAME = "player name";

    private BigDecimal tournamentAccountId;
    private BigDecimal playerId = BigDecimal.valueOf(45345L);
    private TournamentVariationTemplate tournamentVariationTemplate;
    private DateTime eliminationTime;

    @Before
    @Transactional
    public void setUp() {
        tournamentAccountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'tournament test account 1')", tournamentAccountId);
        final BigDecimal playerAccountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'test2')", playerAccountId);
        jdbc.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) VALUES (?,?)", playerId, "YAZINO");
        jdbc.update("INSERT INTO PLAYER(PLAYER_ID,ACCOUNT_ID) VALUES (?,?)", playerId, playerAccountId);

        final String gameType = "BLACKJACK";
        tournamentVariationTemplate = new TournamentVariationTemplateBuilder(jdbc)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("templ1")
                .setEntryFee(new BigDecimal("20"))
                .setServiceFee(BigDecimal.valueOf(1))
                .setStartingChips(BigDecimal.valueOf(100))
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setGameType(gameType)
                .saveToDatabase();
        eliminationTime = new DateTime(2008, 12, 26, 12, 0, 0, 0);
    }

    private Map<String, String> getPlayerProperties() {
        final Map<String, String> properties = new HashMap<>();

        properties.put("property1", "");
        properties.put("property2", null);
        properties.put("property3", "value3");

        return properties;
    }

    private String getPlayerPropertiesAsString() {
        final StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : getPlayerProperties().entrySet()) {
            out.append(entry.getKey());
            if (entry.getValue() != null) {
                out.append("=").append(entry.getValue());
            }
            out.append("\n");
        }
        return out.toString();
    }

    @Test
    @Transactional
    public void shouldCreateTournamentPlayer() {
        final DateTime signupStartTime = new DateTime(2008, 9, 10, 12, 0, 0, 0);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(999999L));
        tournament.setPot(BigDecimal.ZERO);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setTournamentStatus(TournamentStatus.CLOSED);
        tournament.setName("test1");
        tournament.setPartnerId("INTERNAL");

        jdbcTournamentDAO.save(tournament);

        final TournamentPlayer player = new TournamentPlayer(
                playerId, PLAYER_NAME, tournamentAccountId, TournamentPlayerStatus.ADDITION_PENDING);

        player.setName(PLAYER_NAME);
        player.setEliminationTimestamp(eliminationTime);
        player.setEliminationReason(TournamentPlayer.EliminationReason.NOT_ENOUGH_CHIPS_FOR_ROUND);
        player.setProperties(getPlayerProperties());
        jdbcTournamentPlayerDAO.save(tournament.getTournamentId(), player);

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(
                "SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=? AND PLAYER_ID=?",
                tournament.getTournamentId(), playerId);

        assertEquals(playerId, BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()));
        assertEquals(tournament.getTournamentId().longValue(), ((BigDecimal) map.get("TOURNAMENT_ID")).longValue());
        assertEquals(tournamentAccountId, BigDecimal.valueOf(((BigDecimal) map.get("TOURNAMENT_ACCOUNT_ID")).intValue()));
        assertEquals(TournamentPlayerStatus.ADDITION_PENDING, TournamentPlayerStatus.getById(map.get("PLAYER_STATUS").toString()));
        assertEquals(0, ((Integer) map.get("LEADERBOARD_POSITION")).intValue());
        assertEquals(eliminationTime, new DateTime(((Timestamp) map.get("ELIMINATION_TS")).getTime()));
        assertEquals(TournamentPlayer.EliminationReason.NOT_ENOUGH_CHIPS_FOR_ROUND.name(), map.get("ELIMINATION_REASON"));
        assertEquals(getPlayerPropertiesAsString(), map.get("PLAYER_PROPERTIES"));
    }

    @Test
    @Transactional
    public void shouldUpdateTournamentPlayer() {
        final DateTime signupStartTime = new DateTime(2008, 9, 10, 12, 0, 0, 0);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(999999L));
        tournament.setPot(BigDecimal.ZERO);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setTournamentStatus(TournamentStatus.CLOSED);
        tournament.setName("test1");
        tournament.setPartnerId("INTERNAL");

        jdbcTournamentDAO.save(tournament);

        final TournamentPlayer player = new TournamentPlayer(
                playerId, PLAYER_NAME, tournamentAccountId, TournamentPlayerStatus.ADDITION_PENDING);

        player.setName(PLAYER_NAME);
        jdbcTournamentPlayerDAO.save(tournament.getTournamentId(), player);

        player.setStatus(TournamentPlayerStatus.ACTIVE);
        player.setLeaderboardPosition(7);
        final BigDecimal settledPrize = BigDecimal.TEN;
        player.setSettledPrize(settledPrize);
        player.setEliminationTimestamp(eliminationTime);
        player.setEliminationReason(TournamentPlayer.EliminationReason.OFFLINE);
        player.setProperties(getPlayerProperties());

        jdbcTournamentPlayerDAO.save(tournament.getTournamentId(), player);

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(
                "SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=? AND PLAYER_ID=?",
                tournament.getTournamentId(), playerId);

        assertEquals(playerId, BigDecimal.valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()));
        assertEquals(tournament.getTournamentId().longValue(), ((BigDecimal) map.get("TOURNAMENT_ID")).longValue());
        assertEquals(tournamentAccountId, BigDecimal.valueOf(((BigDecimal) map.get("TOURNAMENT_ACCOUNT_ID")).longValue()));
        assertEquals(TournamentPlayerStatus.ACTIVE, TournamentPlayerStatus.getById(map.get("PLAYER_STATUS").toString()));
        assertEquals(0, settledPrize.compareTo(new BigDecimal((map.get("SETTLED_PRIZE").toString()))));
        assertEquals(7, ((Integer) map.get("LEADERBOARD_POSITION")).intValue());
        assertEquals(eliminationTime, new DateTime(((Timestamp) map.get("ELIMINATION_TS")).getTime()));
        assertEquals(TournamentPlayer.EliminationReason.OFFLINE.name(), map.get("ELIMINATION_REASON"));
        assertEquals(getPlayerPropertiesAsString(), map.get("PLAYER_PROPERTIES"));
    }

    @Test
    @Transactional
    public void shouldDeleteTournamentPlayer() {
        final DateTime signupStartTime = new DateTime(2008, 9, 10, 12, 0, 0, 0);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(999910L));
        tournament.setPot(BigDecimal.ZERO);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setTournamentStatus(TournamentStatus.CLOSED);
        tournament.setName("test1");
        tournament.setPartnerId("INTERNAL");
        jdbcTournamentDAO.save(tournament);

        final TournamentPlayer player = new TournamentPlayer(
                playerId, PLAYER_NAME, tournamentAccountId, TournamentPlayerStatus.ADDITION_PENDING);

        player.setName(PLAYER_NAME);
        jdbcTournamentPlayerDAO.save(tournament.getTournamentId(), player);

        @SuppressWarnings({"unchecked"}) Map<String, Object> saveMap = jdbc.queryForMap(
                "SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=? AND PLAYER_ID=?",
                tournament.getTournamentId(), playerId);

        assertEquals(playerId, BigDecimal.valueOf(((BigDecimal) saveMap.get("PLAYER_ID")).longValue()));
        assertEquals(tournament.getTournamentId().longValue(), ((BigDecimal) saveMap.get("TOURNAMENT_ID")).longValue());
        assertEquals(tournamentAccountId, BigDecimal.valueOf(((BigDecimal) saveMap.get("TOURNAMENT_ACCOUNT_ID")).longValue()));

        jdbcTournamentPlayerDAO.remove(tournament.getTournamentId(), player);

        try {
            jdbc.queryForMap("SELECT * FROM TOURNAMENT_PLAYER WHERE TOURNAMENT_ID=? AND PLAYER_ID=?",
                    tournament.getTournamentId(), playerId);
            fail("Exception not thrown");
        } catch (EmptyResultDataAccessException e) {
            // pass
        } catch (Exception e) {
            fail("Incorrect exception thrown: " + e.getClass().getName());
        }
    }

    @Test(expected = NullPointerException.class)
    @Transactional
    public void shouldThrowExceptionforNullTournamentPlayer() {
        jdbcTournamentPlayerDAO.save(null, null);
    }

    @Test
    @Transactional
    public void shouldSelectAllPlayersForTournament() {
        final int playersPerTournament = 4;

        final DateTime signupStartTime = new DateTime(2008, 2, 4, 13, 0, 0, 0);

        final Set<TournamentPlayer> expectedPlayers = new HashSet<>();

        final BigDecimal tournamentAccountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) VALUES (?,'testv')", tournamentAccountId);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(99999L));
        tournament.setPot(BigDecimal.ZERO);
        tournament.setName("TestTourn");
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setTournamentStatus(TournamentStatus.ANNOUNCED);
        tournament.setPartnerId("INTERNAL");

        jdbcTournamentDAO.save(tournament);

        for (int j = 0; j < playersPerTournament; ++j) {
            final BigDecimal localPlayerId = BigDecimal.valueOf(1000 + j);

            final BigDecimal playerAccountId = sequenceGenerator.next();
            jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) values (?,'testvp" + j + "')", playerAccountId);
            jdbc.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) VALUES (?,?)", localPlayerId, "YAZINO");
            jdbc.update("INSERT INTO PLAYER(PLAYER_ID,ACCOUNT_ID) VALUES (?,?)", localPlayerId, playerAccountId);

            final TournamentPlayer player = new TournamentPlayer(localPlayerId, PLAYER_NAME, tournamentAccountId, TournamentPlayerStatus.ACTIVE);
            player.setLeaderboardPosition(j);
            player.setName(PLAYER_NAME + j);
            player.setEliminationTimestamp(eliminationTime);

            jdbcTournamentPlayerDAO.save(tournament.getTournamentId(), player);
            expectedPlayers.add(player);
        }

        final Set<TournamentPlayer> playersReturned = jdbcTournamentPlayerDAO.findByTournamentId(tournament.getTournamentId());
        assertEquals(expectedPlayers.size(), playersReturned.size());
        for (final TournamentPlayer tournamentPlayer : playersReturned) {
            assertTrue(expectedPlayers.contains(tournamentPlayer));
            expectedPlayers.remove(tournamentPlayer);
        }
        assertEquals(0, expectedPlayers.size()); // all matched
    }

}
