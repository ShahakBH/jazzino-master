package com.yazino.platform.service.tournament;


import com.gigaspaces.client.WriteModifiers;
import com.j_spaces.core.LeaseContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.conversion.TournamentMonitorViewTransformer;
import com.yazino.platform.model.conversion.TournamentViewFactory;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.*;
import com.yazino.platform.service.tournament.transactional.TransactionalTournamentService;
import com.yazino.platform.tournament.*;
import com.yazino.test.ThreadLocalDateTimeUtils;
import net.jini.core.lease.Lease;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.yazino.platform.model.tournament.TournamentPlayerProcessingType.ADD;
import static com.yazino.platform.model.tournament.TournamentPlayerProcessingType.REMOVE;
import static com.yazino.platform.tournament.TournamentStatus.*;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class GigaspaceRemotingTournamentServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(458095L);
    private static final int TOURNAMENT_ID = 1;
    private static final String LEASE_UID = "aUID";

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentSummaryRepository tournamentSummaryRepository;
    @Mock
    private TournamentInfoRepository tournamentInfoRepository;
    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private TournamentViewFactory tournamentViewFactory;
    @Mock
    private TransactionalTournamentService transactionalTournamentService;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TournamentScheduleRepository tournamentScheduleRepository;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private LeaseContext<TournamentPlayerProcessingRequest> leaseContext;

    private GigaspaceRemotingTournamentService underTest;
    private final Map<Pair<String, String>, TournamentSchedule> schedules = new ConcurrentHashMap<Pair<String, String>, TournamentSchedule>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new GigaspaceRemotingTournamentService(tournamentRepository, tournamentSummaryRepository,
                sequenceGenerator, transactionalTournamentService, playerRepository, tournamentViewFactory, tournamentScheduleRepository, gigaSpace);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());

        when(sequenceGenerator.next()).thenReturn(bd(TOURNAMENT_ID));
        when(leaseContext.getUID()).thenReturn(LEASE_UID);
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void tournamentsStatusSearchesShouldBeExecutedOnTheRepository() {
        when(tournamentRepository.findByStatus(TournamentStatus.CLOSED, 3)).thenReturn(new PagedData<Tournament>(3, 10, 40, asList(
                aTournamentWithStatus(1, TournamentStatus.CLOSED), aTournamentWithStatus(2, TournamentStatus.CLOSED))));

        final PagedData<TournamentMonitorView> tournaments = underTest.findByStatus(TournamentStatus.CLOSED, 3);

        assertThat(tournaments.getSize(), is(equalTo(10)));
        assertThat(tournaments.getTotalSize(), is(equalTo(40)));
        assertThat(tournaments.getStartPosition(), is(equalTo(3)));
        assertThat(tournaments, hasItem(aMonitorViewOf(aTournamentWithStatus(1, TournamentStatus.CLOSED))));
        assertThat(tournaments, hasItem(aMonitorViewOf(aTournamentWithStatus(2, TournamentStatus.CLOSED))));
    }

    @Test
    public void findByStatusShouldNeverReturnNull() {
        when(tournamentRepository.findByStatus(TournamentStatus.REGISTERING, 5)).thenReturn(null);

        final PagedData<TournamentMonitorView> tournaments = underTest.findByStatus(TournamentStatus.REGISTERING, 5);

        assertThat(tournaments, is(not(nullValue())));
        assertThat(tournaments.getSize(), is(equalTo(0)));
    }

    @Test(expected = NullPointerException.class)
    public void findByStatusShouldThrowExceptionOnNullStatus() {
        underTest.findByStatus(null, 0);
    }

    @Test
    public void findAllShouldBeExecutedOnTheRepository() {
        when(tournamentRepository.findAll(7)).thenReturn(
                new PagedData<Tournament>(7, 10, 70, asList(
                        aTournamentWithStatus(1, RUNNING), aTournamentWithStatus(2, TournamentStatus.CLOSED))));

        final PagedData<TournamentMonitorView> tournaments = underTest.findAll(7);

        assertThat(tournaments.getSize(), is(equalTo(10)));
        assertThat(tournaments.getTotalSize(), is(equalTo(70)));
        assertThat(tournaments.getStartPosition(), is(equalTo(7)));
        assertThat(tournaments.getData(), hasItem(aMonitorViewOf(aTournamentWithStatus(1, RUNNING))));
        assertThat(tournaments.getData(), hasItem(aMonitorViewOf(aTournamentWithStatus(2, TournamentStatus.CLOSED))));
    }

    @Test
    public void findAllShouldNeverReturnNull() {
        when(tournamentRepository.findAll(7)).thenReturn(null);

        final PagedData<TournamentMonitorView> tournaments = underTest.findAll(7);

        assertThat(tournaments, is(not(nullValue())));
        assertThat(tournaments.getSize(), is(equalTo(0)));
    }

    @Test(expected = NullPointerException.class)
    public void createShouldThrowExceptionForANullTournament() throws TournamentException {
        underTest.createTournament(null);
    }

    @Test
    public void tournamentByPlayerSearchesShouldBeExecutedOnTheRepository() throws TournamentException {
        when(tournamentRepository.findByPlayer(PLAYER_ID)).thenReturn(set(
                aTournamentWithStatus(1, RUNNING),
                aTournamentWithStatus(2, WAITING_FOR_CLIENTS)));

        final Set<Tournament> tournaments = underTest.findByPlayer(PLAYER_ID);

        assertThat(tournaments.size(), is(equalTo(2)));
        assertThat(tournaments, hasItem(aTournamentWithStatus(1, RUNNING)));
        assertThat(tournaments, hasItem(aTournamentWithStatus(2, WAITING_FOR_CLIENTS)));
    }

    @Test(expected = NullPointerException.class)
    public void tournamentByPlayerSearchesShouldThrowAnExceptionOnANullPlayerId() throws TournamentException {
        underTest.findByPlayer(null);
    }

    @Test
    public void populationRequestsShouldBeExecutedOnTheRepository() {
        underTest.populateSpaceWithNonClosedTournaments();

        verify(tournamentRepository).loadNonClosedTournamentsIntoSpace();
    }

    @Test
    public void clearRequestsShouldBeExecutedOnTheRepository() {
        underTest.clearSpace();

        verify(tournamentRepository).clear();
    }

    @Test
    public void recurringTournamentDefinitionsAreWrittenToTheSpace() {
        underTest.saveRecurringTournamentDefinition(aRecurringTournament());

        verify(tournamentRepository).save(aRecurringTournamentDefinition());
    }

    @Test(expected = NullPointerException.class)
    public void savingANullRecurringTournamentDefinitionThrowsAnException() {
        underTest.saveRecurringTournamentDefinition(null);
    }

    @Test
    public void aCreationRequestSetsTheId()
            throws WalletServiceException, TournamentException {
        underTest.createTournament(aDefinitionOf(aTournamentWithStatus(null, ANNOUNCED)));

        final Tournament createdTournament = aTournamentWithStatus(TOURNAMENT_ID, ANNOUNCED);
        createdTournament.setNextEvent(createdTournament.getSignupStartTimeStamp().getMillis());
        verify(tournamentRepository).save(createdTournament, true);
    }

    @Test
    public void aCreationRequestSetsPotToZeroOnATournament()
            throws WalletServiceException, TournamentException {
        final Tournament tournament = aTournamentWithStatus(null, ANNOUNCED);
        tournament.setPot(BigDecimal.valueOf(3040304));
        underTest.createTournament(aDefinitionOf(tournament));

        final ArgumentCaptor<Tournament> tournamentCaptor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(tournamentCaptor.capture(), eq(true));

        assertThat(tournamentCaptor.getValue().getPot(), is(equalTo(BigDecimal.ZERO)));
    }

    @Test
    public void aCreationRequestSetsTheNextEventToTheSignUpStartTime()
            throws WalletServiceException, TournamentException {
        final Tournament tournament = aTournamentWithStatus(null, ANNOUNCED);
        underTest.createTournament(aDefinitionOf(tournament));

        final ArgumentCaptor<Tournament> tournamentCaptor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(tournamentCaptor.capture(), eq(true));

        assertThat(tournamentCaptor.getValue().getNextEvent(), is(equalTo(tournament.getSignupStartTimeStamp().getMillis())));
    }

    @Test(expected = TournamentException.class)
    public void presetCreationRequestsWithoutAStartTimeStampAreRejected() throws TournamentException {
        final Tournament tournament = aTournamentWithStatus(null, ANNOUNCED);
        tournament.setStartTimeStamp(null);
        underTest.createTournament(aDefinitionOf(tournament));
    }

    @Test(expected = IllegalStateException.class)
    public void tournamentValidationIsPerformedBeforeCreation() throws TournamentException {
        final Tournament tournament = aTournamentWithStatus(null, CLOSED);
        tournament.setStartTimeStamp(null);

        underTest.createTournament(aDefinitionOf(tournament));
    }

    @Test
    public void cancelDelegatesToTheTransactionalService() {
        underTest.cancelTournament(bd(15));

        verify(transactionalTournamentService).cancelTournament(bd(15));
    }

    @Test(expected = NullPointerException.class)
    public void registerRejectsANullTournamentId() {
        underTest.register(null, PLAYER_ID, false);
    }

    @Test(expected = NullPointerException.class)
    public void registerRejectsANullPlayerId() {
        underTest.register(BigDecimal.valueOf(TOURNAMENT_ID), null, false);
    }

    @Test
    public void asynchronousRegistrationWritesARequestAndReturns() {
        final TournamentOperationResult result = underTest.register(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, true);

        assertThat(result, is(nullValue()));
        verify(gigaSpace).write(aRequestFor(ADD, true));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void synchronousRegistrationWritesARequestAndWaitsForAResponse() {
        when(gigaSpace.write(aRequestFor(ADD, false), Lease.FOREVER, 5000L, WriteModifiers.WRITE_ONLY))
                .thenReturn(leaseContext);
        when(gigaSpace.take(aResponseTemplate(), 5000)).thenReturn(aResponseWithResult(
                TournamentOperationResult.AFTER_SIGNUP_TIME));

        final TournamentOperationResult result = underTest.register(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, false);

        assertThat(result, is(equalTo(TournamentOperationResult.AFTER_SIGNUP_TIME)));
    }

    @Test
    public void ifSynchronousRegistrationTimesOutThenNoResponseIsReturned() {
        when(gigaSpace.write(aRequestFor(ADD, false), Lease.FOREVER, 5000L, WriteModifiers.WRITE_ONLY))
                .thenReturn(leaseContext);
        when(gigaSpace.take(aResponseTemplate(), 5000)).thenReturn(null);

        final TournamentOperationResult result = underTest.register(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, false);

        assertThat(result, is(equalTo(TournamentOperationResult.NO_RESPONSE_RETURNED)));
    }

    @Test(expected = NullPointerException.class)
    public void deregisterRejectsANullTournamentId() {
        underTest.deregister(null, PLAYER_ID, false);
    }

    @Test(expected = NullPointerException.class)
    public void deregisterRejectsANullPlayerId() {
        underTest.deregister(BigDecimal.valueOf(TOURNAMENT_ID), null, false);
    }

    @Test
    public void asynchronousDeregistrationWritesARequestAndReturns() {
        final TournamentOperationResult result = underTest.deregister(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, true);

        assertThat(result, is(nullValue()));
        verify(gigaSpace).write(aRequestFor(REMOVE, true));
        verifyNoMoreInteractions(gigaSpace);
    }

    @Test
    public void synchronousDeregistrationWritesARequestAndWaitsForAResponse() {
        when(gigaSpace.write(aRequestFor(REMOVE, false), Lease.FOREVER, 5000L, WriteModifiers.WRITE_ONLY))
                .thenReturn(leaseContext);
        when(gigaSpace.take(aResponseTemplate(), 5000)).thenReturn(aResponseWithResult(
                TournamentOperationResult.SUCCESS));

        final TournamentOperationResult result = underTest.deregister(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, false);

        assertThat(result, is(equalTo(TournamentOperationResult.SUCCESS)));
    }

    @Test
    public void ifSynchronousDeregistrationTimesOutThenNoResponseIsReturned() {
        when(gigaSpace.write(aRequestFor(REMOVE, false), Lease.FOREVER, 5000L, WriteModifiers.WRITE_ONLY))
                .thenReturn(leaseContext);
        when(gigaSpace.take(aResponseTemplate(), 5000)).thenReturn(null);

        final TournamentOperationResult result = underTest.deregister(BigDecimal.valueOf(TOURNAMENT_ID), PLAYER_ID, false);

        assertThat(result, is(equalTo(TournamentOperationResult.NO_RESPONSE_RETURNED)));
    }

    @Test
    public void findingTablesForAnExistingTournamentReturnsASetOfTheTournamentIDs() {
        final Tournament tournament = aTournamentWithStatus(10, TournamentStatus.RUNNING);
        tournament.setTables(asList(bd(11), bd(12), bd(13)));
        when(tournamentRepository.findById(bd(10))).thenReturn(tournament);

        final Set<BigDecimal> tableIds = underTest.findTableIdsFor(bd(10));

        assertThat(tableIds, is(equalTo(set(bd(11), bd(12), bd(13)))));
    }

    @Test
    public void findingTablesForANonExistentTournamentReturnsAnEmptySet() {
        when(tournamentRepository.findById(bd(10))).thenReturn(null);

        final Set<BigDecimal> tableIds = underTest.findTableIdsFor(bd(10));

        assertThat(tableIds, is(not(nullValue())));
        assertThat(tableIds.size(), is(equalTo(0)));
    }

    private TournamentPlayerProcessingResponse aResponseWithResult(
            final TournamentOperationResult result) {
        return new TournamentPlayerProcessingResponse(LEASE_UID, bd(TOURNAMENT_ID), result);
    }

    private TournamentPlayerProcessingResponse aResponseTemplate() {
        final TournamentPlayerProcessingResponse response = new TournamentPlayerProcessingResponse();
        response.setRequestSpaceId(LEASE_UID);
        response.setTournamentId(bd(TOURNAMENT_ID));
        return response;
    }

    private TournamentPlayerProcessingRequest aRequestFor(final TournamentPlayerProcessingType type, final boolean async) {
        return new TournamentPlayerProcessingRequest(
                PLAYER_ID, bd(TOURNAMENT_ID), type, async);
    }

    private TournamentVariationTemplate aTournamentTemplate() {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(4325454354L))
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("aTournamentTemplate")
                .setGameType("BLACKJACK")
                .toTemplate();
    }

    private RecurringTournamentDefinition aRecurringTournamentDefinition() {
        final RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setId(BigInteger.valueOf(20));
        definition.setEnabled(false);
        return definition;
    }

    private RecurringTournament aRecurringTournament() {
        return new RecurringTournament(BigInteger.valueOf(20), null, null, null, null, null, null, null, null, false);
    }

    private BigDecimal bd(final int i) {
        return BigDecimal.valueOf(i);
    }

    private <T> Set<T> set(T... values) {
        return new HashSet<T>(Arrays.asList(values));
    }

    private TournamentMonitorView aMonitorViewOf(final Tournament tournament) {
        return new TournamentMonitorViewTransformer().apply(tournament);
    }

    private TournamentDefinition aDefinitionOf(final Tournament tournament) {
        return new TournamentDefinition(
                tournament.getTournamentId(),
                tournament.getName(),
                tournament.getTournamentVariationTemplate(),
                tournament.getSignupStartTimeStamp(),
                tournament.getSignupEndTimeStamp(),
                tournament.getStartTimeStamp(),
                tournament.getTournamentStatus(),
                tournament.getPartnerId(),
                tournament.getDescription());
    }

    private Tournament aTournamentWithStatus(final Integer id, final TournamentStatus status) {
        final Tournament tournament = new Tournament();
        if (id != null) {
            tournament.setTournamentId(BigDecimal.valueOf(id));
        }
        tournament.setName("aTournament");
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setStartTimeStamp(new DateTime().plusMinutes(10));
        tournament.setTournamentStatus(status);
        tournament.setTournamentVariationTemplate(aTournamentTemplate());
        tournament.setPot(BigDecimal.ZERO);
        return tournament;
    }
}
