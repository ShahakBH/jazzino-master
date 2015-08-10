package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerAchievementsPersistenceRequest;
import com.yazino.platform.persistence.statistic.PlayerStatsDAO;
import com.yazino.platform.processor.PersistenceRequest;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerAchievementsPersisterTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1003);

    @Mock
    private PlayerStatsDAO playerStatsDao;
    @Mock
    private PlayerAchievementsRepository playerAchievementsRepository;

    private PlayerAchievementsPersister underTest;

    @Before
    public void setUp() {
        underTest = new PlayerAchievementsPersister(playerStatsDao, playerAchievementsRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPlayerStatsDAO() {
        new PlayerAchievementsPersister(null, playerAchievementsRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPlayerAchievementsRepository() {
        new PlayerAchievementsPersister(playerStatsDao, null);
    }

    @Test
    public void theProcessorIgnoresNullRequests() {
        assertThat(underTest.persist(null), is(nullValue()));

        verifyZeroInteractions(playerAchievementsRepository, playerStatsDao);
    }

    @Test
    public void theProcessorIgnoresRequestsWithANullPlayerId() {
        assertThat(underTest.persist(new PlayerAchievementsPersistenceRequest()), is(nullValue()));

        verifyZeroInteractions(playerAchievementsRepository, playerStatsDao);
    }

    @Test
    public void theProcessorIgnoresRequestsForPlayersWithNoAchievementsInTheSpace() {
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenThrow(new IllegalArgumentException("anException"));

        assertThat(underTest.persist(aPersistenceRequest()), is(nullValue()));

        verify(playerAchievementsRepository).forPlayer(PLAYER_ID);
        verifyZeroInteractions(playerStatsDao);
    }

    @Test
    public void theProcessorSavesThePlayerAchievementsToTheDAO() {
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerAchievements());

        assertThat(underTest.persist(aPersistenceRequest()), is(nullValue()));

        verify(playerStatsDao).saveAchievements(aPlayerAchievements());
    }

    @Test
    public void theProcessorReturnsTheRequestToTheSpaceInAnErrorStateWhenAnExceptionOccursDuringPersistence() {
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerAchievements());
        doThrow(new RuntimeException("anException")).when(playerStatsDao).saveAchievements(aPlayerAchievements());

        assertThat(underTest.persist(aPersistenceRequest()), is(equalTo(aPersistenceRequestInErrorState())));
    }

    private PlayerAchievements aPlayerAchievements() {
        return new PlayerAchievements(PLAYER_ID, new HashSet<String>(), new HashMap<String, String>());
    }

    private PlayerAchievementsPersistenceRequest aPersistenceRequest() {
        return new PlayerAchievementsPersistenceRequest(PLAYER_ID);
    }

    private PersistenceRequest<BigDecimal> aPersistenceRequestInErrorState() {
        final PlayerAchievementsPersistenceRequest request = new PlayerAchievementsPersistenceRequest(PLAYER_ID);
        request.setStatus(PersistenceRequest.Status.ERROR);
        return request;
    }

}
