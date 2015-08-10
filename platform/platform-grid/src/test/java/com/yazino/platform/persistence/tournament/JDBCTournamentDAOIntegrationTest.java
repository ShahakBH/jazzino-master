package com.yazino.platform.persistence.tournament;

import com.gigaspaces.datasource.DataIterator;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;
import com.yazino.platform.persistence.DataIterable;
import com.yazino.platform.persistence.PrimaryKeyLoader;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.tournament.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static com.yazino.platform.tournament.TournamentStatus.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCTournamentDAOIntegrationTest {
    private static final String TOURNAMENT_SELECT = "SELECT * FROM TOURNAMENT WHERE TOURNAMENT_ID=?";
    private static final String TOURNAMENT_TABLE_COUNT = "SELECT count(TOURNAMENT_ID) AS COUNT FROM TOURNAMENT_TABLE WHERE TOURNAMENT_ID=?";

    // this needs to use the interface to avoid autowiring errors from the proxies
    @Autowired
    private TournamentDao underTest;

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private SequenceGenerator sequenceGenerator;

    private final String PARTNER_ID = "INTERNAL";

    private BigDecimal gameVariationTemplateId;
    private BigDecimal playerId = BigDecimal.valueOf(45345L);
    private TournamentVariationTemplate tournamentVariationTemplate;
    private static final BigDecimal TOURNAMENT_POT = new BigDecimal("181.0000");
    private static final DateTime SIGNUP_START_TIME = new DateTime(2008, 2, 4, 13, 0, 0, 0);

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static Integer i(int i) {
        //noinspection UnnecessaryBoxing
        return new Integer(i);
    }

    @Before
    public void setUp() {
        final BigDecimal playerAccountId = sequenceGenerator.next();
        jdbc.update("INSERT INTO ACCOUNT(ACCOUNT_ID,NAME) values (?,?)", playerAccountId, "test2");
        jdbc.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) values (?,?)", playerId, "YAZINO");
        jdbc.update("insert into PLAYER(PLAYER_ID,ACCOUNT_ID) values (?,?)", playerId, playerAccountId);

        gameVariationTemplateId = jdbc.execute(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                return con.prepareStatement("INSERT INTO GAME_VARIATION_TEMPLATE(GAME_TYPE, NAME, VERSION) VALUES ('BLACKJACK','Test variation',0)", PreparedStatement.RETURN_GENERATED_KEYS);
            }
        }, new PrimaryKeyLoader()); // ensure there is an entry in GAME_VARIATION_TEMPLATE

        tournamentVariationTemplate = new TournamentVariationTemplateBuilder(jdbc)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("templ1")
                .setEntryFee(new BigDecimal("20.0000"))
                .setServiceFee(new BigDecimal("1.0000"))
                .setStartingChips(new BigDecimal("100.0000"))
                .setMinPlayers(1)
                .setMaxPlayers(10)
                .setGameType("BLACKJACK")
                .saveToDatabase();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionForNullTournament() {
        underTest.save(null);
    }

    @Test
    public void shouldCreateTournament() {
        final DateTime startTime = new DateTime(2008, 12, 26, 12, 0, 0, 0);
        final DateTime signupStartTime = new DateTime(2008, 12, 20, 12, 0, 0, 0);
        final DateTime signupEndTime = new DateTime(2008, 12, 26, 10, 0, 0, 0);
        final long nextEvent = 453345000L;
        final BigDecimal id = BigDecimal.valueOf(80000L);

        final Tournament tournament = new Tournament(id);
        tournament.setTournamentId(BigDecimal.valueOf(80000L));
        tournament.setPot(TOURNAMENT_POT);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setStartTimeStamp(startTime);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setSignupEndTimeStamp(signupEndTime);
        tournament.setTournamentStatus(CLOSED);
        tournament.setName("bob the tourney");
        tournament.setNextEvent(nextEvent);
        tournament.setPartnerId(PARTNER_ID);
        final BigDecimal settledPrizePot = new BigDecimal("10.0000");
        tournament.setSettledPrizePot(settledPrizePot);

        underTest.save(tournament);
        assertNotNull(tournament.getTournamentId()); // ID should have been updated

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(
                TOURNAMENT_SELECT, tournament.getTournamentId());

        assertEquals(id, BigDecimal.valueOf(((BigDecimal) map.get("TOURNAMENT_ID")).longValue()));
        assertTrue(TOURNAMENT_POT.compareTo(new BigDecimal(map.get("POT").toString())) == 0);
        assertEquals(tournamentVariationTemplate.getTournamentVariationTemplateId(),
                BigDecimal.valueOf(((Long) map.get("TOURNAMENT_VARIATION_TEMPLATE_ID")).longValue()));
        assertEquals(startTime, new DateTime(((Timestamp) map.get("TOURNAMENT_START_TS")).getTime()));
        assertEquals(signupStartTime, new DateTime(((Timestamp) map.get("TOURNAMENT_SIGNUP_START_TS")).getTime()));
        assertEquals(signupEndTime, new DateTime(((Timestamp) map.get("TOURNAMENT_SIGNUP_END_TS")).getTime()));
        assertEquals(CLOSED, getById(map.get("TOURNAMENT_STATUS").toString()));
        assertEquals(PARTNER_ID, map.get("PARTNER_ID"));
        assertEquals("bob the tourney", map.get("TOURNAMENT_NAME"));
        assertEquals(nextEvent, ((Timestamp) map.get("NEXT_EVENT_TS")).getTime());
        assertEquals(settledPrizePot, new BigDecimal(map.get("SETTLED_PRIZE_POT").toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldUpdateTournament() {
        final DateTime startTime = new DateTime(2008, 12, 26, 12, 0, 0, 0);
        final DateTime signupStartTime = new DateTime(2008, 12, 20, 12, 0, 0, 0);
        final DateTime signupEndTime = new DateTime(2008, 12, 26, 10, 0, 0, 0);
        final long nextEvent = 453345000L;
        final long secondEvent = 890988000L;


        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(3141592L));
        tournament.setPot(TOURNAMENT_POT);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setStartTimeStamp(startTime);
        tournament.setSignupStartTimeStamp(signupStartTime);
        tournament.setSignupEndTimeStamp(signupEndTime);
        tournament.setTournamentStatus(CLOSED);
        tournament.setName("bob the tourney");
        tournament.setNextEvent(nextEvent);
        tournament.setPartnerId(PARTNER_ID);

        @SuppressWarnings({"unchecked"}) Map<String, Object> map = jdbc.queryForMap(
                "SELECT count(TOURNAMENT_ID) AS count FROM TOURNAMENT WHERE TOURNAMENT_ID=?", tournament.getTournamentId());
        assertEquals(0L, map.get("COUNT"));
        map = jdbc.queryForMap(TOURNAMENT_TABLE_COUNT, tournament.getTournamentId());
        assertEquals(0L, map.get("COUNT"));

        underTest.save(tournament);


        tournament.setNextEvent(secondEvent);
        final BigDecimal prizePot = new BigDecimal("1.2345");
        tournament.setSettledPrizePot(prizePot);
        tournament.setTables(Arrays.asList(bd("1.0"), bd("2.0"), bd("3.0")));
        BigDecimal updatedPot = new BigDecimal(99911);
        tournament.setPot(updatedPot);
        underTest.save(tournament);

        map = jdbc.queryForMap(TOURNAMENT_SELECT, tournament.getTournamentId());

        assertTrue(updatedPot.compareTo(new BigDecimal(map.get("POT").toString())) == 0);
        assertEquals(tournamentVariationTemplate.getTournamentVariationTemplateId(),
                BigDecimal.valueOf(((Long) map.get("TOURNAMENT_VARIATION_TEMPLATE_ID")).longValue()));
        assertEquals(startTime, new DateTime(((Timestamp) map.get("TOURNAMENT_START_TS")).getTime()));
        assertEquals(signupStartTime, new DateTime(((Timestamp) map.get("TOURNAMENT_SIGNUP_START_TS")).getTime()));
        assertEquals(signupEndTime, new DateTime(((Timestamp) map.get("TOURNAMENT_SIGNUP_END_TS")).getTime()));
        assertEquals(CLOSED, getById(map.get("TOURNAMENT_STATUS").toString()));
        assertEquals("bob the tourney", map.get("TOURNAMENT_NAME"));
        assertEquals(secondEvent, ((Timestamp) map.get("NEXT_EVENT_TS")).getTime());
        assertEquals(prizePot, new BigDecimal(map.get("SETTLED_PRIZE_POT").toString()));

        map = jdbc.queryForMap(TOURNAMENT_TABLE_COUNT, tournament.getTournamentId());
        assertEquals(3L, map.get("COUNT"));
    }

    @Test
    public void shouldSelectAllTournamentsForPlayer() {
        final List<Tournament> expectedTournaments = new ArrayList<Tournament>();

        final TournamentStatus[] statuses = new TournamentStatus[]{
                REGISTERING, CLOSED, REGISTERING,
                REGISTERING, CLOSED
        };

        for (int i = 0; i < statuses.length; ++i) {
            final Tournament tournament = tournament(i, statuses[i]);
            underTest.save(tournament);

            if (statuses[i] != CLOSED) {
                expectedTournaments.add(tournament);
            }
        }

        final List<Tournament> nonClosedTournaments = underTest.findNonClosedTournaments();
        assertEquals(expectedTournaments.size(), nonClosedTournaments.size());
        for (final Tournament tournament : nonClosedTournaments) {
            assertTrue("Tournament " + tournament + " not found in expected tournamnents", expectedTournaments.contains(tournament));


            expectedTournaments.remove(tournament);
        }
        assertEquals(0, expectedTournaments.size()); // all matched
    }

    @Test
    public void selectAllTournamentsReturnsNonNullForEmptyResults() {
        final List<Tournament> tournaments = underTest.findNonClosedTournaments();
        assertNotNull(tournaments);
        assertEquals(0, tournaments.size());
    }

    @Test
    public void testTournamentPropertiesAreMappedCorrectly() {
        List<TournamentVariationPayout> expectedPayoutList = Arrays.asList(
                new TournamentVariationPayout(1, new BigDecimal("0.50000000")),
                new TournamentVariationPayout(2, new BigDecimal("0.35000000")),
                new TournamentVariationPayout(3, new BigDecimal("0.15000000"))
        );
        List<TournamentVariationRound> expectedRoundList = Arrays.asList(
                new TournamentVariationRound(1, 5000L, 20000L, gameVariationTemplateId, "Red Blackjack", new BigDecimal("20.0000"), "20"),
                new TournamentVariationRound(2, 4000L, 30000L, gameVariationTemplateId, "Red Blackjack", BigDecimal.ZERO, "0")
        );
        tournamentVariationTemplate = new TournamentVariationTemplateBuilder(jdbc)
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("temp1")
                .setEntryFee(BigDecimal.valueOf(20))
                .setServiceFee(BigDecimal.valueOf(12))
                .setStartingChips(BigDecimal.valueOf(10))
                .setMinPlayers(20)
                .setMaxPlayers(30)
                .setGameType("BLACKJACK")
                .setTournamentPayouts(expectedPayoutList)
                .setTournamentRounds(expectedRoundList)
                .saveToDatabase();
        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(108000L));
        tournament.setPot(TOURNAMENT_POT);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setStartTimeStamp(new DateTime(2008, 12, 26, 12, 0, 0, 0));
        tournament.setSignupStartTimeStamp(new DateTime(2008, 12, 20, 12, 0, 0, 0));
        tournament.setSignupEndTimeStamp(new DateTime(2008, 12, 26, 10, 0, 0, 0));
        tournament.setTournamentStatus(REGISTERING);
        tournament.setName("bob the tourney");
        tournament.setPartnerId(PARTNER_ID);
        tournament.setTables(Arrays.asList(bd("1.0"), bd("2.0"), bd("3.0")));
        underTest.save(tournament);

        final List<Tournament> nonClosedTournaments = underTest.findNonClosedTournaments();
        assertEquals(1, nonClosedTournaments.size());
        final Tournament tournamentFromDB = nonClosedTournaments.iterator().next();
        assertEquals(3, tournamentFromDB.getTables().size());
        assertNotNull(tournamentFromDB.getTournamentVariationTemplate());
        final TournamentVariationTemplate tournamentVariationTemplate = tournamentFromDB.getTournamentVariationTemplate();
        assertEquals("temp1", tournamentVariationTemplate.getTemplateName());
        assertEquals(bd("20.0000"), tournamentVariationTemplate.getEntryFee());
        assertEquals(bd("12.0000"), tournamentVariationTemplate.getServiceFee());
        assertEquals(bd("10.0000"), tournamentVariationTemplate.getStartingChips());
        assertEquals(i(20), tournamentVariationTemplate.getMinPlayers());
        assertEquals(i(30), tournamentVariationTemplate.getMaxPlayers());
        assertEquals(2, tournamentVariationTemplate.getTournamentRounds().size());
        TournamentVariationRound firstRound = tournamentVariationTemplate.getTournamentRounds().get(0);
        assertEquals(1, firstRound.getRoundNumber());
        assertEquals(5000L, firstRound.getRoundEndInterval());
        assertEquals(20000L, firstRound.getRoundLength());
        assertEquals(gameVariationTemplateId, firstRound.getGameVariationTemplateId());
        assertEquals("Red Blackjack", firstRound.getClientPropertiesId());
        assertEquals(bd("20.0000"), firstRound.getMinimumBalance());
        assertEquals(3, tournamentVariationTemplate.getTournamentPayouts().size());
        TournamentVariationPayout rank1Payout = tournamentVariationTemplate.getTournamentPayouts().get(0);
        assertEquals(1, rank1Payout.getRank());
        assertEquals(new BigDecimal("0.50000000"), rank1Payout.getPayout());
    }

    @Test
    public void iterateAllTournamentsReturnsAllOpenTournaments() {
        final Set<Tournament> expected = createTournaments(5);

        assertThat(toSet(((DataIterable<Tournament>) underTest).iterateAll()), is(equalTo(expected)));
    }

    @Test
    public void iterateAllTournamentsExcludesClosedTournaments() {
        final Set<Tournament> expected = createTournaments(5);
        underTest.save(tournament(6, CLOSED));

        assertThat(toSet(((DataIterable<Tournament>) underTest).iterateAll()), is(equalTo(expected)));
    }

    @Test
    public void iterateAllTournamentsExcludesErrorTournaments() {
        final Set<Tournament> expected = createTournaments(5);
        underTest.save(tournament(6, ERROR));

        assertThat(toSet(((DataIterable<Tournament>) underTest).iterateAll()), is(equalTo(expected)));
    }

    private Set<Tournament> createTournaments(final int count) {
        final Set<Tournament> expected = new HashSet<Tournament>();
        for (int i = 0; i < count; i++) {
            final Tournament tournament = tournament(i, RUNNING);
            underTest.save(tournament);
            expected.add(tournament);
        }
        return expected;
    }

    private <T> HashSet<T> toSet(final DataIterator<T> dataIterator) {
        final HashSet<T> results = new HashSet<T>();
        while (dataIterator.hasNext()) {
            results.add(dataIterator.next());
        }
        dataIterator.close();
        return results;
    }

    private Tournament tournament(final int i, final TournamentStatus status) {
        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(100000L + i));
        tournament.setPot(TOURNAMENT_POT);
        tournament.setName("TestTournament" + i);
        tournament.setTournamentVariationTemplate(tournamentVariationTemplate);
        tournament.setSignupStartTimeStamp(SIGNUP_START_TIME);
        tournament.setPartnerId(PARTNER_ID);
        tournament.setTournamentStatus(status);
        tournament.setTables(new ArrayList<BigDecimal>());
        return tournament;
    }

}
