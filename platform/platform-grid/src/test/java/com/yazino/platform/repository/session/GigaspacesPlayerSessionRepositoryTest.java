package com.yazino.platform.repository.session;

import com.gigaspaces.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.session.GlobalPlayerListUpdateRequest;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.table.TableType.PRIVATE;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspacesPlayerSessionRepositoryTest {
    private static final BigDecimal LOCAL_ID = BigDecimal.valueOf(1);
    private static final BigDecimal REMOTE_ID = BigDecimal.valueOf(2);
    private static final int SESSION_TIMEOUT_MINUTES = 12;
    private static final String SESSION_KEY = "aSessionKey";

    @Mock
    private GigaSpace localSpace;
    @Mock
    private GigaSpace globalSpace;
    @Mock
    private GigaSpace injectedSpace;
    @Mock
    private Routing routing;

    private GigaspacesPlayerSessionRepository underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        when(routing.isRoutedToCurrentPartition(LOCAL_ID)).thenReturn(true);

        final Map<String, Object> injectedServices = new HashMap<>();
        injectedServices.put("gigaSpace", injectedSpace);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(2, injectedServices);

        underTest = new GigaspacesPlayerSessionRepository(localSpace, globalSpace, routing, executor);
        underTest.setSessionTimeoutMinutes(SESSION_TIMEOUT_MINUTES);
    }

    @After
    public void resetDate() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullLocalSpace() {
        new GigaspacesPlayerSessionRepository(null, globalSpace, routing, mock(Executor.class));
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullGlobalSpace() {
        new GigaspacesPlayerSessionRepository(localSpace, null, routing, mock(Executor.class));
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullRouting() {
        new GigaspacesPlayerSessionRepository(localSpace, globalSpace, null, mock(Executor.class));
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullExecutor() {
        new GigaspacesPlayerSessionRepository(localSpace, globalSpace, routing, null);
    }

    @Test(expected = NullPointerException.class)
    public void findingByANullPlayerIdThrowsANullPointerException() {
        underTest.findAllByPlayer(null);
    }

    @Test
    public void aPlayersSessionsAreReadFromTheLocalSpaceWhenTheIdRoutesLocally() {
        when(localSpace.readMultiple(new PlayerSession(LOCAL_ID), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ))
                .thenReturn(new PlayerSession[]{aSessionWithId(LOCAL_ID)});

        assertThat(underTest.findAllByPlayer(LOCAL_ID), is(equalTo((Collection<PlayerSession>) newHashSet(aSessionWithId(LOCAL_ID)))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aSessionIsReadFromTheLocalSpaceWhenTheIdRoutesLocally() {
        when(localSpace.read(new PlayerSession(LOCAL_ID, SESSION_KEY), 0, ReadModifiers.DIRTY_READ)).thenReturn(aSessionWithId(LOCAL_ID));

        assertThat(underTest.findByPlayerAndSessionKey(LOCAL_ID, SESSION_KEY), is(equalTo(aSessionWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aPlayersSessionAreReadFromTheGlobalSpaceWhenTheIdDoesNotRouteLocally() {
        when(globalSpace.readMultiple(new PlayerSession(REMOTE_ID), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ))
                .thenReturn(new PlayerSession[]{aSessionWithId(REMOTE_ID)});

        assertThat(underTest.findAllByPlayer(REMOTE_ID), is(equalTo((Collection<PlayerSession>) newHashSet(aSessionWithId(REMOTE_ID)))));
        verifyZeroInteractions(localSpace);
    }


    @Test
    public void aSessionIsReadFromTheGlobalSpaceWhenTheIdDoesNotRouteLocally() {
        when(globalSpace.read(new PlayerSession(REMOTE_ID, SESSION_KEY), 0, ReadModifiers.DIRTY_READ))
                .thenReturn(aSessionWithId(REMOTE_ID));

        assertThat(underTest.findByPlayerAndSessionKey(REMOTE_ID, SESSION_KEY), is(equalTo(aSessionWithId(REMOTE_ID))));
        verifyZeroInteractions(localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void removingSessionsForANullPlayerThrowsANullPointerException() {
        underTest.removeAllByPlayer(null);
    }

    @Test
    public void removingSessionsForAPlayerRoutedToTheLocalSpaceRemovesThemFromTheLocalSpace() {
        underTest.removeAllByPlayer(LOCAL_ID);

        verify(localSpace).clear(aSessionWithId(LOCAL_ID));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void removingSessionForAPlayerNotRoutedToTheLocalSpaceRemovesThemFromTheGlobalSpace() {
        underTest.removeAllByPlayer(REMOTE_ID);

        verify(globalSpace).clear(aSessionWithId(REMOTE_ID));
        verifyZeroInteractions(localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void removingASessionForANullPlayerIdThrowsANullPointerException() {
        underTest.removeByPlayerAndSessionKey(null, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void removingASessionFOrANullSessionKeyThrowsANullPointerException() {
        underTest.removeByPlayerAndSessionKey(LOCAL_ID, null);
    }

    @Test
    public void removingASessionRoutedToTheLocalSpaceRemovesItFromTheLocalSpace() {
        underTest.removeByPlayerAndSessionKey(LOCAL_ID, SESSION_KEY);

        verify(localSpace).clear(new PlayerSession(LOCAL_ID, SESSION_KEY));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void removingASessionNotRoutedToTheLocalSpaceRemovesItFromTheGlobalSpace() {
        underTest.removeByPlayerAndSessionKey(REMOTE_ID, SESSION_KEY);

        verify(globalSpace).clear(new PlayerSession(REMOTE_ID, SESSION_KEY));
        verifyZeroInteractions(localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void lockingASessionWithANullPlayerIdThrowsANullPointerException() {
        underTest.lock(null, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void lockingASessionWithANullSessionKeyThrowsANullPointerException() {
        underTest.lock(LOCAL_ID, null);
    }

    @Test
    public void aSessionFromTheLocalSpaceCanBeLocked() {
        when(localSpace.read(new PlayerSession(LOCAL_ID, SESSION_KEY), 500, ReadModifiers.EXCLUSIVE_READ_LOCK))
                .thenReturn(aSessionWithId(LOCAL_ID));

        assertThat(underTest.lock(LOCAL_ID, SESSION_KEY), is(equalTo(aSessionWithId(LOCAL_ID))));
        verifyZeroInteractions(globalSpace);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void lockingASessionThatDoesNotExistOrIsLockedInTheLocalSpaceCausesAConcurrentModificationException() {
        underTest.lock(LOCAL_ID, SESSION_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lockingASessionThatIsNotRoutedLocallyCausesAnIllegalArgumentException() {
        underTest.lock(REMOTE_ID, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void savingANullSessionThrowsANullPointerException() {
        underTest.save(null);
    }

    @Test
    public void savingaSessionRoutedToTheLocalSpaceWritesToTheLocalSpaceWithTheSessionTimeout() {
        underTest.save(aSessionWithId(LOCAL_ID));

        verify(localSpace).write(aSessionWithId(LOCAL_ID), SESSION_TIMEOUT_MINUTES * 60000);
    }

    @Test
    public void savingASessionThatIsNotRoutedToTheLocalSpaceWritesItTheTheGlobalSpaceWithTheSessionTimeout() {
        underTest.save(aSessionWithId(REMOTE_ID));

        verify(globalSpace).write(aSessionWithId(REMOTE_ID), SESSION_TIMEOUT_MINUTES * 60000);
    }

    @Test(expected = NullPointerException.class)
    public void extendingASessionWithANullPlayerIdThrowsANullPointerException() {
        underTest.extendCurrentSession(null, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void extendingASessionWithANullSessionKeyThrowsANullPointerException() {
        underTest.extendCurrentSession(LOCAL_ID, null);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void extendingASessionThrowsAConcurrentModificationExceptionIfTheSessionDoesNotExistOrIsLocked() {
        underTest.extendCurrentSession(LOCAL_ID, SESSION_KEY);
    }

    @Test
    public void extendingALocallyRoutedSessionRewritesTheSessionToTheLocalSpaceWithAFullLease() {
        when(localSpace.read(new PlayerSession(LOCAL_ID, SESSION_KEY), 500, ReadModifiers.EXCLUSIVE_READ_LOCK))
                .thenReturn(aSessionWithId(LOCAL_ID));

        underTest.extendCurrentSession(LOCAL_ID, SESSION_KEY);

        verify(localSpace).write(aSessionWithId(LOCAL_ID), SESSION_TIMEOUT_MINUTES * 60000);
        verifyZeroInteractions(globalSpace);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extendingANotLocallyRoutedSessionThrowsAnIllegalArgumentException() {
        underTest.extendCurrentSession(REMOTE_ID, SESSION_KEY);
    }

    @Test(expected = NullPointerException.class)
    public void findingOnlinePlayersWithANullFilterListThrowsANullPointerException() {
        underTest.findOnlinePlayers(null);
    }

    @Test
    public void findingOnlinePlayersReturnsTheListOfFilteredPlayersFromTheGlobalSpace() {
        when(injectedSpace.readMultiple(any(SQLQuery.class), eq(500), eq(ReadModifiers.DIRTY_READ)))
                .thenReturn(new Object[]{new PlayerSession(BigDecimal.valueOf(1)), new PlayerSession(BigDecimal.valueOf(3))})
                .thenReturn(new Object[]{new PlayerSession(BigDecimal.valueOf(2))});

        final Set<BigDecimal> onlinePlayers = underTest.findOnlinePlayers(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));

        assertThat(onlinePlayers.size(), is(equalTo(3)));
        assertThat(onlinePlayers, containsInAnyOrder(BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    }

    @Test(expected = NullPointerException.class)
    public void findingOnlinePlayerSessionsWithANullFilterListThrowsANullPointerException() {
        underTest.findOnlinePlayerSessions(null);
    }

    @Test
    public void findingOnlinePlayerSessionsReturnsTheListOfFilteredPlayersFromTheGlobalSpace() {
        when(globalSpace.readMultiple(any(SQLQuery.class), eq(500), eq(ReadModifiers.DIRTY_READ)))
                .thenReturn(new Object[]{aSession(1), aSession(3)});

        final Map<BigDecimal, PlayerSessionsSummary> onlinePlayers
                = underTest.findOnlinePlayerSessions(newHashSet(BigDecimal.valueOf(1), BigDecimal.valueOf(2)));

        assertThat(onlinePlayers.size(), is(equalTo(2)));
        assertThat(onlinePlayers.get(BigDecimal.valueOf(1)), is(equalTo(aSessionsSummary(1))));
        assertThat(onlinePlayers.get(BigDecimal.valueOf(3)), is(equalTo(aSessionsSummary(3))));
    }

    @Test
    public void countingPlayingOnlinePlayerSessionsCountsOnTheGlobalSpace() {
        final PlayerSession template = new PlayerSession();
        template.setPlaying(true);
        when(globalSpace.count(template)).thenReturn(8);

        assertThat(underTest.countPlayerSessions(true), is(equalTo(8)));
        verifyZeroInteractions(localSpace);
    }

    @Test
    public void countingAllOnlinePlayerSessionsCountsOnTheGlobalSpace() {
        when(globalSpace.count(new PlayerSession())).thenReturn(8);

        assertThat(underTest.countPlayerSessions(false), is(equalTo(8)));
        verifyZeroInteractions(localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void updatingTheGlobalPlayerListWithANullPlayerThrowsANullPointerException() {
        underTest.updateGlobalPlayerList(null);
    }

    @Test
    public void updateTheGlobalPlayerListWithALocallyRoutedPlayerWritesARequestToTheLocalSpace() {
        underTest.updateGlobalPlayerList(LOCAL_ID);

        verify(localSpace).write(new GlobalPlayerListUpdateRequest(LOCAL_ID));
        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void updateTheGlobalPlayerListWithANonLocallyRoutedPlayerWritesARequestToTheGlobalSpace() {
        underTest.updateGlobalPlayerList(REMOTE_ID);

        verify(globalSpace).write(new GlobalPlayerListUpdateRequest(REMOTE_ID));
        verifyZeroInteractions(localSpace);
    }

    @Test(expected = NullPointerException.class)
    public void aNullOnlinePlayerQueryThrowsANullPointerException() {
        underTest.isOnline(null);
    }

    @Test
    public void aLocallyRoutedPlayerIsOnlineWhenTheyArePresentInTheLocalSpace() {
        when(localSpace.readMultiple(new PlayerSession(LOCAL_ID), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ))
                .thenReturn(new PlayerSession[]{aSessionWithId(LOCAL_ID)});

        assertThat(underTest.isOnline(LOCAL_ID), is(true));

        verifyZeroInteractions(globalSpace);
    }


    @Test
    public void aLocallyRoutedPlayerIsOfflineWhenTheyAreNotPresentInTheLocalSpace() {
        assertThat(underTest.isOnline(LOCAL_ID), is(false));

        verifyZeroInteractions(globalSpace);
    }

    @Test
    public void aNonLocallyRoutedPlayerIsOnlineWhenTheyArePresentInTheGlobalSpace() {
        when(globalSpace.readMultiple(new PlayerSession(REMOTE_ID), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ))
                .thenReturn(new PlayerSession[]{aSessionWithId(REMOTE_ID)});

        assertThat(underTest.isOnline(REMOTE_ID), is(true));

        verifyZeroInteractions(localSpace);
    }

    @Test
    public void aNonLocallyRoutedPlayerIsOfflineWhenTheyAreNotPresentInTheGlobalSpace() {
        assertThat(underTest.isOnline(REMOTE_ID), is(false));

        verifyZeroInteractions(localSpace);
    }

    @Test
    public void sessionStatusesAreRetrievedFromAllPartitions() {
        when(injectedSpace.readMultiple(new PlayerSession(), Integer.MAX_VALUE))
                .thenReturn(new PlayerSession[]{aSessionWithId(BigDecimal.valueOf(1)), aSessionWithId(BigDecimal.valueOf(3))})
                .thenReturn(new PlayerSession[]{aSessionWithId(BigDecimal.valueOf(4)), aSessionWithId(BigDecimal.valueOf(8))});

        final Set<PlayerSessionStatus> allSessionStatuses = underTest.findAllSessionStatuses();

        assertThat(allSessionStatuses.size(), is(equalTo(4)));
        assertThat(allSessionStatuses, containsInAnyOrder(new PlayerSessionStatus(BigDecimal.valueOf(1)),
                new PlayerSessionStatus(BigDecimal.valueOf(3)), new PlayerSessionStatus(BigDecimal.valueOf(4)), new PlayerSessionStatus(BigDecimal.valueOf(8))));
    }

    private PlayerSession aSession(final int id) {
        final PlayerSession playerSession = new PlayerSession(BigDecimal.valueOf(id));
        playerSession.setSessionId(BigDecimal.valueOf(100 + id));
        playerSession.setNickname("nickname" + id);
        playerSession.setPictureUrl("picture" + id);
        playerSession.setLocations(newHashSet(
                new Location("location1-" + id, "locationname1-" + id, "gametype1-" + id, BigDecimal.valueOf(id), PRIVATE),
                new Location("location2-" + id, "locationname2-" + id, "gametype2-" + id, BigDecimal.valueOf(id), PRIVATE)));
        playerSession.setBalanceSnapshot(BigDecimal.valueOf(1000 + id));
        return playerSession;
    }

    private PlayerSessionsSummary aSessionsSummary(final int id) {
        return new PlayerSessionsSummary("nickname" + id, "picture" + id, BigDecimal.valueOf(1000 + id),
                newHashSet(new Location("location1-" + id, "locationname1-" + id, "gametype1-" + id, BigDecimal.valueOf(id), PRIVATE),
                        new Location("location2-" + id, "locationname2-" + id, "gametype2-" + id, BigDecimal.valueOf(id), PRIVATE)));
    }

    private PlayerSession aSessionWithId(final BigDecimal id) {
        return new PlayerSession(id);
    }

}

