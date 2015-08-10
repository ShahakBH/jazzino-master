package com.yazino.platform.processor.statistic.achievement;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerAchievementsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.repository.statistic.GigaspacePlayerAchievementsRepository;
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
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspacePlayerAchievementsRepositoryTest {
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

    private GigaspacePlayerAchievementsRepository underTest;

    @Before
    public void setUp() {
        when(routing.isRoutedToCurrentPartition(LOCAL_PLAYER_ID)).thenReturn(true);
        when(routing.isRoutedToCurrentPartition(REMOTE_PLAYER_ID)).thenReturn(false);

        underTest = new GigaspacePlayerAchievementsRepository(localGigaSpace, globalGigaSpace, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullLocalGigaSpace() {
        new GigaspacePlayerAchievementsRepository(null, globalGigaSpace, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullGlobalGigaSpace() {
        new GigaspacePlayerAchievementsRepository(localGigaSpace, null, routing, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullRouting() {
        new GigaspacePlayerAchievementsRepository(localGigaSpace, globalGigaSpace, null, playerStatsDao);
    }

    @Test(expected = NullPointerException.class)
    public void theRepositoryCannotBeCreatedWithANullPlayerStatsDAO() {
        new GigaspacePlayerAchievementsRepository(localGigaSpace, globalGigaSpace, routing, null);
    }

    @Test
    public void shouldReturnAchievementsForALocallyRoutedPlayer() {
        when(localGigaSpace.readById(PlayerAchievements.class, LOCAL_PLAYER_ID, LOCAL_PLAYER_ID, 0, ReadModifiers.DIRTY_READ))
                .thenReturn(aPlayerAchievements());

        PlayerAchievements actual = underTest.forPlayer(LOCAL_PLAYER_ID);

        assertThat(actual, is(equalTo(aPlayerAchievements())));
    }

    @Test
    public void shouldReturnAchievementsForANonLocallyRoutedPlayer() {
        when(globalGigaSpace.readById(PlayerAchievements.class, REMOTE_PLAYER_ID, REMOTE_PLAYER_ID, 0, ReadModifiers.DIRTY_READ))
                .thenReturn(aRemotePlayerAchievements());

        PlayerAchievements actual = underTest.forPlayer(REMOTE_PLAYER_ID);

        assertThat(actual, is(equalTo(aRemotePlayerAchievements())));
    }

    @Test
    public void shouldLoadAchievementsForPlayerFromDatabaseIfNotInSpaceAndRoutingMatchesCurrentSpace() {
        when(playerStatsDao.getAchievements(LOCAL_PLAYER_ID)).thenReturn(aPlayerAchievements());

        PlayerAchievements actual = underTest.forPlayer(LOCAL_PLAYER_ID);

        assertThat(actual, is(equalTo(aPlayerAchievements())));
    }

    @Test
    public void shouldWriteAchievementsForPlayerToTheSpaceIfNotInSpaceAndRoutingMatchesCurrentSpace() {
        when(playerStatsDao.getAchievements(LOCAL_PLAYER_ID)).thenReturn(aPlayerAchievements());

        underTest.forPlayer(LOCAL_PLAYER_ID);

        verify(localGigaSpace).write(aPlayerAchievements(), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test
    public void shouldLoadAchievementsForPlayerFromDatabaseIfNotInSpaceAndRoutingDoesNotMatchCurrentSpace() {
        when(playerStatsDao.getAchievements(REMOTE_PLAYER_ID)).thenReturn(aRemotePlayerAchievements());

        PlayerAchievements actual = underTest.forPlayer(REMOTE_PLAYER_ID);

        assertThat(actual, is(equalTo(aRemotePlayerAchievements())));
    }

    @Test
    public void shouldWriteAchievementsForPlayerToTheSpaceIfNotInSpaceAndRoutingDoesNotMatchCurrentSpace() {
        when(playerStatsDao.getAchievements(REMOTE_PLAYER_ID)).thenReturn(aRemotePlayerAchievements());

        underTest.forPlayer(REMOTE_PLAYER_ID);

        verify(globalGigaSpace).write(aRemotePlayerAchievements(), Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfAchievementsNotFoundAndIsNotInTheDatabase() {
        underTest.forPlayer(INVALID_PLAYER_ID);
    }

    @Test(expected = NullPointerException.class)
    public void findShouldThrowExceptionIfPlayerIdIsNull() {
        underTest.forPlayer(null);
    }

    @Test
    public void saveWritesALocallyRoutedPlayerAchievementsToTheSpace() {
        underTest.save(aPlayerAchievements());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(localGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) aPlayerAchievements()));
    }

    @Test
    public void saveWritesALocallyRoutedPersistenceRequestToTheSpace() {
        underTest.save(aPlayerAchievements());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(localGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) new PlayerAchievementsPersistenceRequest(LOCAL_PLAYER_ID)));
    }

    @Test
    public void saveWritesANonLocallyRoutedPlayerAchievementsToTheSpace() {
        underTest.save(aRemotePlayerAchievements());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(globalGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) aRemotePlayerAchievements()));
    }

    @Test
    public void saveWritesANonLocallyRoutedPersistenceRequestToTheSpace() {
        underTest.save(aRemotePlayerAchievements());

        final ArgumentCaptor<Object[]> objectCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(globalGigaSpace).writeMultiple(objectCaptor.capture(), eq(Lease.FOREVER), eq(WriteModifiers.UPDATE_OR_WRITE));
        assertThat(asList(objectCaptor.getValue()), hasItem((Object) new PlayerAchievementsPersistenceRequest(REMOTE_PLAYER_ID)));
    }

    @Test(expected = NullPointerException.class)
    public void saveThrowsAnExceptionForANullPlayerLevels() {
        underTest.save(null);
    }

    private PlayerAchievements aPlayerAchievements() {
        return new PlayerAchievements(LOCAL_PLAYER_ID, new HashSet<String>(), new HashMap<String, String>());
    }

    private PlayerAchievements aRemotePlayerAchievements() {
        return new PlayerAchievements(REMOTE_PLAYER_ID, new HashSet<String>(), new HashMap<String, String>());
    }
}
