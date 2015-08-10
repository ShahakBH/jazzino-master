package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.model.statistic.PlayerLevelsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerLevelsPersisterTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1003);

    @Mock
    private PlayerStatsDAO playerStatsDao;
    @Mock
    private PlayerLevelsRepository playerLevelsRepository;

    private PlayerLevelsPersister underTest;

    @Before
    public void setUp() {
        underTest = new PlayerLevelsPersister(playerStatsDao, playerLevelsRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPlayerStatsDAO() {
        new PlayerLevelsPersister(null, playerLevelsRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPlayerAchievementsRepository() {
        new PlayerLevelsPersister(playerStatsDao, null);
    }

    @Test
    public void theProcessorIgnoresNullRequests() {
        assertThat(underTest.persist(null), is(nullValue()));

        verifyZeroInteractions(playerLevelsRepository, playerStatsDao);
    }

    @Test
    public void theProcessorIgnoresRequestsWithANullPlayerId() {
        assertThat(underTest.persist(new PlayerLevelsPersistenceRequest()), is(nullValue()));

        verifyZeroInteractions(playerLevelsRepository, playerStatsDao);
    }

    @Test
    public void theProcessorIgnoresRequestsForPlayersWithNoLevelsInTheSpace() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenThrow(new IllegalArgumentException("anException"));

        assertThat(underTest.persist(aPersistenceRequest()), is(nullValue()));

        verify(playerLevelsRepository).forPlayer(PLAYER_ID);
        verifyZeroInteractions(playerStatsDao);
    }

    @Test
    public void theProcessorSavesThePlayerAchievementsToTheDAO() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());

        assertThat(underTest.persist(aPersistenceRequest()), is(nullValue()));

        verify(playerStatsDao).saveLevels(aPlayerLevels());
    }

    @Test
    public void theProcessorReturnsTheRequestToTheSpaceInAnErrorStateWhenAnExceptionOccursDuringPersistence() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());
        doThrow(new RuntimeException("anException")).when(playerStatsDao).saveLevels(aPlayerLevels());

        assertThat(underTest.persist(aPersistenceRequest()), is(equalTo(aPersistenceRequestInErrorState())));
    }

    private PlayerLevels aPlayerLevels() {
        return new PlayerLevels(PLAYER_ID, new HashMap<String, PlayerLevel>());
    }

    private PersistenceRequest<BigDecimal> aPersistenceRequest() {
        return new PlayerLevelsPersistenceRequest(PLAYER_ID);
    }

    private PersistenceRequest<BigDecimal> aPersistenceRequestInErrorState() {
        final PlayerLevelsPersistenceRequest request = new PlayerLevelsPersistenceRequest(PLAYER_ID);
        request.setStatus(PersistenceRequest.Status.ERROR);
        return request;
    }
}
