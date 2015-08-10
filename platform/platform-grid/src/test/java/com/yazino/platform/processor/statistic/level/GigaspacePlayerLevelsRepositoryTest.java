package com.yazino.platform.processor.statistic.level;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.model.statistic.PlayerLevelsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.repository.statistic.GigaspacePlayerLevelsRepository;
import net.jini.core.lease.Lease;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspacePlayerLevelsRepositoryTest {
    public static final BigDecimal LOCAL_PLAYER_ID = BigDecimal.ONE;
    public static final BigDecimal REMOTE_PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal INVALID_PLAYER_ID = BigDecimal.valueOf(20);

    @Mock
    private GigaSpace localGigaSpace;
    @Mock
    private GigaSpace globalGigaSpace;
    @Mock
    private Routing routing;
    @Mock
    private PlayerStatsDAO playerStatsDao;

    private GigaspacePlayerLevelsRepository underTest;

    @Before
    public void setUp() {
        when(routing.isRoutedToCurrentPartition(LOCAL_PLAYER_ID)).thenReturn(true);
        when(routing.isRoutedToCurrentPartition(REMOTE_PLAYER_ID)).thenReturn(false);

        underTest = new GigaspacePlayerLevelsRepository(localGigaSpace, globalGigaSpace, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullLocalGigaSpace() {
        new GigaspacePlayerLevelsRepository(null, globalGigaSpace, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullGlobalGigaSpace() {
        new GigaspacePlayerLevelsRepository(localGigaSpace, null, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullRouting() {
        new GigaspacePlayerLevelsRepository(localGigaSpace, globalGigaSpace, null, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullPlayerStatsDAO() {
        new GigaspacePlayerLevelsRepository(localGigaSpace, globalGigaSpace, routing, null);
    }

    @Test
    public void findShouldReturnLevelsForPlayersRoutedToTheLocalSpace() {
        when(localGigaSpace.readById(PlayerLevels.class, LOCAL_PLAYER_ID, LOCAL_PLAYER_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(aPlayerLevels());

        PlayerLevels actual = underTest.forPlayer(LOCAL_PLAYER_ID);

        assertThat(actual, is(equalTo(aPlayerLevels())));
        verifyZeroInteractions(globalGigaSpace);
    }

    @Test
    public void findShouldReturnLevelsForPlayersNotRoutedToTheLocalSpace() {
        when(globalGigaSpace.readById(PlayerLevels.class, REMOTE_PLAYER_ID, REMOTE_PLAYER_ID, 0, ReadModifiers.DIRTY_READ)).thenReturn(aPlayerLevels());

        PlayerLevels actual = underTest.forPlayer(REMOTE_PLAYER_ID);

        assertThat(actual, is(equalTo(aPlayerLevels())));
        verifyZeroInteractions(localGigaSpace);
    }

    @Test
    public void shouldLoadLevelsForPlayerFromDatabaseIfNotInSpaceAndRoutingMatchesCurrentSpace() {
        when(playerStatsDao.getLevels(LOCAL_PLAYER_ID)).thenReturn(aPlayerLevels());

        PlayerLevels actual = underTest.forPlayer(LOCAL_PLAYER_ID);

        assertThat(actual, is(equalTo(aPlayerLevels())));
    }

    @Test
    public void shouldWriteLevelsForPlayerToTheSpaceIfNotInSpaceAndRoutingMatchesCurrentSpace() {
        when(playerStatsDao.getLevels(LOCAL_PLAYER_ID)).thenReturn(aPlayerLevels());

        underTest.forPlayer(LOCAL_PLAYER_ID);

        verify(localGigaSpace).write(aPlayerLevels(), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void shouldWriteLevelsForPlayerToTheSpaceIfNotInSpaceAndRoutingDoesNotMatchCurrentSpace() {
        when(playerStatsDao.getLevels(REMOTE_PLAYER_ID)).thenReturn(aRemotePlayerLevels());

        underTest.forPlayer(REMOTE_PLAYER_ID);

        verify(globalGigaSpace).write(aRemotePlayerLevels(), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findShouldThrowExceptionIfLevelNotFoundAndIsNotInTheDatabase() {
        underTest.forPlayer(INVALID_PLAYER_ID);
    }

    @Test(expected = NullPointerException.class)
    public void findShouldThrowExceptionIfPlayerIdIsNull() {
        underTest.forPlayer(null);
    }

    @Test
    public void saveWritesALocallyRoutedPlayerLevelsToTheSpace() {
        underTest.save(aPlayerLevels());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(localGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) aPlayerLevels()));
    }

    @Test
    public void saveWritesALocallyRoutedPersistenceRequestToTheSpace() {
        underTest.save(aPlayerLevels());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(localGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) new PlayerLevelsPersistenceRequest(LOCAL_PLAYER_ID)));
    }

    @Test
    public void saveWritesANonLocallyRoutedPlayerLevelsToTheSpace() {
        underTest.save(aRemotePlayerLevels());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(globalGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) aRemotePlayerLevels()));
    }

    @Test
    public void saveWritesANonLocallyRoutedPersistenceRequestToTheSpace() {
        underTest.save(aRemotePlayerLevels());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(globalGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) new PlayerLevelsPersistenceRequest(REMOTE_PLAYER_ID)));
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsAnExceptionForANullPlayerLevels() {
        underTest.save(null);
    }

    private PlayerLevels aPlayerLevels() {
        return new PlayerLevels(LOCAL_PLAYER_ID, new HashMap<String, PlayerLevel>());
    }

    private PlayerLevels aRemotePlayerLevels() {
        return new PlayerLevels(REMOTE_PLAYER_ID, new HashMap<String, PlayerLevel>());
    }

}
