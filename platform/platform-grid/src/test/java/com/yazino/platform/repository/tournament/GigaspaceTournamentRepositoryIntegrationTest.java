package com.yazino.platform.repository.tournament;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.persistence.tournament.TournamentDao;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.yazino.platform.tournament.TournamentStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceTournamentRepositoryIntegrationTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(435L);
    private static final int PAGE_SIZE = 20;

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private Routing routing;

    @Mock
    private TournamentDao tournamentDao;
    @Mock
    private TournamentPlayers tournamentPlayers;

    private GigaspaceTournamentRepository underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        gigaSpace.clear(null);

        underTest = new GigaspaceTournamentRepository(gigaSpace, gigaSpace, routing, tournamentDao);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldFindByStatus() {
        final List<Tournament> expectedClosedTournaments = new ArrayList<>();
        final List<Tournament> expectedOpenTournaments = new ArrayList<>();

        final TournamentStatus[] statuses = new TournamentStatus[]{
                REGISTERING, CLOSED, CLOSED, REGISTERING, REGISTERING
        };

        final TournamentVariationTemplate template = aTournamentVariationTemplate();
        for (int i = 0; i < statuses.length; ++i) {
            final Tournament tournament = new Tournament();
            tournament.setTournamentId(BigDecimal.valueOf(i));
            tournament.setName("Tournament " + i);
            tournament.setSignupStartTimeStamp(new DateTime());
            tournament.setTournamentStatus(statuses[i]);
            tournament.setTournamentVariationTemplate(template);

            gigaSpace.write(tournament);

            if (statuses[i] == CLOSED) {
                expectedClosedTournaments.add(tournament);
            } else if (statuses[i] == REGISTERING) {
                expectedOpenTournaments.add(tournament);
            }
        }

        final PagedData<Tournament> closedTournaments = underTest.findByStatus(CLOSED, 0);
        assertThat(closedTournaments.getData(), is(equalTo(expectedClosedTournaments)));

        final PagedData<Tournament> openTournaments = underTest.findByStatus(REGISTERING, 0);
        assertThat(openTournaments.getData(), is(equalTo(expectedOpenTournaments)));
    }

    @Test(expected = NullPointerException.class)
    public void findByStatusShouldThrowExceptionOnNullStatus() {
        underTest.findByStatus(null, 0);
    }

    @Test
    public void findByStatusShouldNotReturnNull() {
        final PagedData<Tournament> tournaments = underTest.findByStatus(CLOSED, 0);

        assertNotNull(tournaments);
        assertEquals(0, tournaments.getSize());
    }

    @Test
    public void shouldFindAllForLeaderboardUpdate() {
        final TournamentStatus[] statuses = {RUNNING, ON_BREAK, ANNOUNCED, FINISHED,
                ON_BREAK, RUNNING, REGISTERING, CLOSED, SETTLED, WAITING_FOR_CLIENTS};

        final Set<Tournament> expectedTournaments = new HashSet<>();

        final TournamentVariationTemplate template = aTournamentVariationTemplate();
        for (int i = 0; i < statuses.length; ++i) {
            final Tournament tournament = new Tournament();
            tournament.setTournamentId(BigDecimal.valueOf(i));
            tournament.setName("Tournament " + i);
            tournament.setTournamentStatus(statuses[i]);
            tournament.setSignupStartTimeStamp(new DateTime());
            tournament.setTournamentVariationTemplate(template);

            gigaSpace.write(tournament);
            if (tournament.getTournamentStatus() == RUNNING
                    || tournament.getTournamentStatus() == WAITING_FOR_CLIENTS) {
                expectedTournaments.add(tournament);
            }
        }

        final Set<Tournament> tournaments = underTest.findLocalForLeaderboardUpdates();

        assertEquals(expectedTournaments, tournaments);
    }

    @Test
    public void findAllForLeaderboardUpdateShouldNotReturnNull() {
        final Set<Tournament> tournaments = underTest.findLocalForLeaderboardUpdates();

        assertNotNull(tournaments);
        assertEquals(0, tournaments.size());
    }

    @Test
    public void findByIdReturnsTournamentIfItExists() {
        Tournament tournament = new Tournament();
        tournament.setTournamentId(TOURNAMENT_ID);

        gigaSpace.write(new Tournament(tournament), 3000);

        tournament = underTest.findById(TOURNAMENT_ID);
        assertNotNull(tournament);
        assertEquals(TOURNAMENT_ID, tournament.getTournamentId());
    }

    @Test
    public void findByIdReturnsNullIfNoTournamentExists() {
        final BigDecimal testId = BigDecimal.valueOf(4354343534543L);

        gigaSpace.take(new Tournament(testId));
        if (gigaSpace.read(new Tournament(testId)) != null) {
            fail("Tournament exists in space and cannot be taken: " + testId);
        }

        assertNull(underTest.findById(testId));
    }

    @Test
    @Transactional
    public void lockReturnsTournamentFromSpace() {
        final Tournament tournament = new Tournament(TOURNAMENT_ID);
        gigaSpace.write(new Tournament(tournament));

        final Tournament readTournament = underTest.lock(TOURNAMENT_ID);

        assertNotNull(readTournament);
        assertEquals(tournament, readTournament);
    }

    @Transactional
    @Test(expected = ConcurrentModificationException.class)
    public void lockWillThrowExceptionIfTournamentNotAvailable() {
        final BigDecimal testId = BigDecimal.valueOf(4354343534543L);
        underTest.setTimeOut(10);
        gigaSpace.take(new Tournament(testId));
        if (gigaSpace.read(new Tournament(testId)) != null) {
            fail("Tournament exists in space and cannot be taken: " + testId);
        }

        underTest.lock(TOURNAMENT_ID);
    }

    @Test
    public void saveAddsTournamentAndInfoToSpace() {
        final BigDecimal tournamentId = BigDecimal.valueOf(9879897L);

        final BigDecimal playerId = BigDecimal.valueOf(12);
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(playerId, "bob", BigDecimal.valueOf(1), TournamentPlayerStatus.ACTIVE);

        final TournamentVariationTemplate template = aTournamentVariationTemplate();

        when(tournamentPlayers.size()).thenReturn(4);
        when(tournamentPlayers.size(TournamentPlayerStatus.ACTIVE)).thenReturn(2);
        when(tournamentPlayers.iterator()).thenReturn(Arrays.asList(tournamentPlayer).iterator());

        final Tournament tournament = new Tournament();
        ReflectionTestUtils.setField(tournament, "players", tournamentPlayers);
        tournament.setTournamentId(tournamentId);
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setStartTimeStamp(new DateTime());
        tournament.setTournamentVariationTemplate(template);
        tournament.setName("bobthebuilder");

        final Tournament expectedTournament = new Tournament(tournament);

        underTest.save(tournament, true);

        final Tournament readTournament = gigaSpace.take(new Tournament(tournamentId));
        assertNotNull(readTournament);
        assertEquals(expectedTournament, readTournament);

        final TournamentPersistenceRequest readTournamentPersistenceRequest = gigaSpace.take(
                new TournamentPersistenceRequest(tournamentId));
        assertNotNull(readTournamentPersistenceRequest);

        final TournamentPlayerInfo tournamentPlayerInfo = gigaSpace.take(
                new TournamentPlayerInfo(playerId, tournamentId, TournamentPlayerStatus.ACTIVE));
        assertNotNull(tournamentPlayerInfo);
    }

    @Test
    public void savingARecurringTournamentDefinitionWritesTheObjectToTheSpace() {
        final RecurringTournamentDefinition definition = aRecurringTournamentDefinition();
        definition.setTournamentDescription("aTournamentDescrption");

        underTest.save(definition);

        final RecurringTournamentDefinition readDefinition = gigaSpace.take(aRecurringTournamentDefinition());
        assertNotNull(readDefinition);
        assertEquals(definition, readDefinition);
    }


    @Test
    public void nonPersistentSaveAddsTournamentToSpaceWithoutAPersistenceRequest() {
        final BigDecimal tournamentId = BigDecimal.valueOf(9879897L);

        when(tournamentPlayers.size()).thenReturn(4);
        when(tournamentPlayers.size(TournamentPlayerStatus.ACTIVE)).thenReturn(2);

        final Tournament tournament = new Tournament();
        ReflectionTestUtils.setField(tournament, "players", tournamentPlayers);
        tournament.setTournamentId(tournamentId);
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setStartTimeStamp(new DateTime());
        tournament.setTournamentVariationTemplate(aTournamentVariationTemplate());
        tournament.setName("bobthebuilder");

        final Tournament expectedTournament = new Tournament(tournament);

        underTest.nonPersistentSave(tournament);

        final Tournament readTournament = gigaSpace.take(new Tournament(tournamentId));
        assertNotNull(readTournament);
        assertEquals(expectedTournament, readTournament);

        final TournamentPersistenceRequest readTournamentPersistenceRequest = gigaSpace.take(
                new TournamentPersistenceRequest(tournamentId));
        assertNull(readTournamentPersistenceRequest);
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsExceptionOnNullTournamentId() {
        final Tournament tournament = new Tournament();
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setTournamentVariationTemplate(aTournamentVariationTemplate());
        tournament.setName("bobthebuilder");

        underTest.save(tournament, true);
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsExceptionOnNullSignUpStartTime() {
        final BigDecimal tournamentId = BigDecimal.valueOf(9879897L);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setTournamentVariationTemplate(aTournamentVariationTemplate());
        tournament.setName("bobthebuilder");

        underTest.save(tournament, true);
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsExceptionOnNullTournamentVariationTemplate() {
        final BigDecimal tournamentId = BigDecimal.valueOf(9879897L);

        final Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setSignupStartTimeStamp(new DateTime());

        tournament.setName("bobthebuilder");

        underTest.save(tournament, true);
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsExceptionOnNullName() {
        final BigDecimal tournamentId = BigDecimal.valueOf(9879897L);
        final TournamentVariationTemplate template = aTournamentVariationTemplate();
        final Tournament tournament = new Tournament();
        tournament.setTournamentId(tournamentId);
        tournament.setSignupStartTimeStamp(new DateTime());

        tournament.setTournamentVariationTemplate(template);

        underTest.save(tournament, true);
    }

    @Test
    public void loadNonClosedTournamentsLoadsTheTournamentsIntoTheSpace() {
        final List<Tournament> tournamentsToLoad = new ArrayList<>();

        for (int i = 0; i < 10; ++i) {
            tournamentsToLoad.add(aTournament(i));
        }

        when(tournamentDao.findNonClosedTournaments()).thenReturn(tournamentsToLoad);

        underTest.loadNonClosedTournamentsIntoSpace();

        for (final Tournament expectedTournament : tournamentsToLoad) {
            final Tournament tournamentFromSpace = gigaSpace.read(new Tournament(expectedTournament.getTournamentId()));
            assertNotNull(tournamentFromSpace);
            assertEquals(expectedTournament, tournamentFromSpace);
        }

        verify(tournamentDao).findNonClosedTournaments();
    }

    @Test
    public void canClearSpace() {
        for (int i = 0; i < 10; ++i) {
            final Tournament tournament = aTournament(i);

            gigaSpace.write(tournament);
        }
        final Tournament[] preMatches = gigaSpace.readMultiple(new Tournament(), Integer.MAX_VALUE);
        assertEquals(10, preMatches.length);

        underTest.clear();

        final Tournament[] postMatches = gigaSpace.takeMultiple(new Tournament(), Integer.MAX_VALUE);
        assertTrue(postMatches == null || postMatches.length == 0);
    }

    @Test
    public void aTournamentCanBeRemovedFromTheSpace() {
        final Tournament tournament = aTournament(1);
        gigaSpace.write(tournament);

        underTest.remove(tournament);

        assertThat(gigaSpace.read(new Tournament(tournament.getTournamentId())), is(nullValue()));
    }

    @Test
    public void existingTournamentsAreWrittenToTheDataStoreBeforeBeingRemoved() {
        final Tournament tournament = aTournament(1);
        gigaSpace.write(tournament);

        underTest.remove(tournament);

        verify(tournamentDao).save(tournament);
    }

    @Test
    public void ifPersistenceFailsBeforeTournamentRemovalThenTheTournamentRemainsInTheSpaceInAnErrorState() {
        final Tournament tournament = aTournament(1);
        gigaSpace.write(tournament);

        doThrow(new RuntimeException("aTestException")).when(tournamentDao).save(tournament);

        underTest.remove(tournament);

        final Tournament tournamentInSpace = gigaSpace.read(new Tournament(tournament.getTournamentId()));
        assertThat(tournamentInSpace, is(not(nullValue())));
        assertThat(tournamentInSpace.getTournamentStatus(), is(equalTo(TournamentStatus.ERROR)));
    }

    @Test
    public void whenATournamentIsRemovedFromTheSpaceTheMatchingPlayerInfoIsAlsoRemoved() {
        final Tournament tournament = aTournament(1);
        gigaSpace.write(tournament);
        gigaSpace.write(aPlayerInfoWithStatus(tournament, TournamentPlayerStatus.ACTIVE));
        gigaSpace.write(aPlayerInfoWithStatus(tournament, TournamentPlayerStatus.ADDITION_PENDING));

        underTest.remove(tournament);

        final TournamentPlayerInfo playerInfoTemplate = new TournamentPlayerInfo();
        playerInfoTemplate.setTournamentId(tournament.getTournamentId());
        assertThat(gigaSpace.readMultiple(playerInfoTemplate, Integer.MAX_VALUE).length, is(equalTo(0)));
    }

    @Test
    public void whenATournamentIsRemovedFromTheSpaceAnyMatchingPersistenceRequestsAreAlsoRemoved() {
        final Tournament tournament = aTournament(1);
        gigaSpace.write(tournament);
        gigaSpace.write(new TournamentPersistenceRequest(tournament.getTournamentId()));

        underTest.remove(tournament);

        assertThat(gigaSpace.readMultiple(new TournamentPersistenceRequest(tournament.getTournamentId()), Integer.MAX_VALUE).length, is(equalTo(0)));
    }

    @Test
    public void findAllReturnsPagedDataForFirstPage() {
        final int totalSize = 45;
        final int pageSize = 20;
        for (int i = 0; i < totalSize; ++i) {
            gigaSpace.write(aTournament(1000 + i));
        }

        final PagedData<Tournament> tournaments = underTest.findAll(0);

        assertThat(tournaments.getStartPosition(), is(equalTo(0)));
        assertThat(tournaments.getSize(), is(equalTo(pageSize)));
        assertThat(tournaments.getTotalSize(), is(equalTo(totalSize)));
        for (int i = 0; i < pageSize; ++i) {
            assertThat(tournaments, hasItem(aTournament(1000 + i)));
        }
    }

    @Test
    public void findAllReturnsPagedDataForMiddlePage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            gigaSpace.write(aTournament(1000 + i));
        }

        final PagedData<Tournament> tournaments = underTest.findAll(1);

        assertThat(tournaments.getStartPosition(), is(equalTo(PAGE_SIZE)));
        assertThat(tournaments.getSize(), is(equalTo(PAGE_SIZE)));
        assertThat(tournaments.getTotalSize(), is(equalTo(totalSize)));
        for (int i = PAGE_SIZE; i < PAGE_SIZE * 2; ++i) {
            assertThat(tournaments, hasItem(aTournament(1000 + i)));
        }
    }

    @Test
    public void findAllReturnsPagedDataForLastPage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            gigaSpace.write(aTournament(1000 + i));
        }

        final PagedData<Tournament> tournaments = underTest.findAll(2);

        assertThat(tournaments.getStartPosition(), is(equalTo(2 * PAGE_SIZE)));
        assertThat(tournaments.getSize(), is(equalTo(totalSize % PAGE_SIZE)));
        assertThat(tournaments.getTotalSize(), is(equalTo(totalSize)));
        for (int i = PAGE_SIZE * 2; i < totalSize; ++i) {
            assertThat(tournaments, hasItem(aTournament(1000 + i)));
        }
    }

    @Test
    public void findByTypesReturnsEmptyPagedDataForAnInvalidPage() {
        final int totalSize = 45;
        for (int i = 0; i < totalSize; ++i) {
            gigaSpace.write(aTournament(1000 + i));
        }

        final PagedData<Tournament> tournaments = underTest.findAll(600);

        assertThat(tournaments.getStartPosition(), is(equalTo(600 * PAGE_SIZE)));
        assertThat(tournaments.getSize(), is(equalTo(0)));
        assertThat(tournaments.getTotalSize(), is(equalTo(totalSize)));
        assertThat(tournaments.getData().size(), is(equalTo(0)));
    }

    @Test
    public void playerEliminationWritesAnEliminationRequestToTheSpace() {
        underTest.playerEliminatedFrom(TOURNAMENT_ID, BigDecimal.valueOf(89893), "aGameType", 2, 23);

        final TournamentPlayerEliminationRequest request = gigaSpace.readById(TournamentPlayerEliminationRequest.class, TOURNAMENT_ID + "-89893");
        assertThat(request, is(not(nullValue())));
        assertThat(request.getTournamentId(), is(equalTo(TOURNAMENT_ID)));
        assertThat(request.getPlayerId(), is(equalTo(BigDecimal.valueOf(89893))));
        assertThat(request.getGameType(), is(equalTo("aGameType")));
        assertThat(request.getLeaderBoardPosition(), is(equalTo(23)));
        assertThat(request.getNumberOfPlayers(), is(equalTo(2)));
    }

    private TournamentPlayerInfo aPlayerInfoWithStatus(final Tournament tournament,
                                                       final TournamentPlayerStatus status) {
        return new TournamentPlayerInfo(BigDecimal.valueOf(10), tournament.getTournamentId(), status);
    }

    private Tournament aTournament(final int i) {
        final Tournament tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.valueOf(i));
        tournament.setName("Tournament " + i);
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setTournamentVariationTemplate(aTournamentVariationTemplate());
        return tournament;
    }

    private RecurringTournamentDefinition aRecurringTournamentDefinition() {
        final RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.valueOf(20));
        return definition;
    }

    private TournamentVariationTemplate aTournamentVariationTemplate() {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(4325454354L))
                .setTournamentType(TournamentType.SITNGO)
                .setTemplateName("Bob the template")
                .setGameType("BLACKJACK")
                .toTemplate();
    }
}
