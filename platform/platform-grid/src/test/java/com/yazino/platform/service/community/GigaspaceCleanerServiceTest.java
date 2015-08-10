package com.yazino.platform.service.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceCleanerServiceTest {
    private static final BigDecimal OFFLINE_PLAYER_ID = BigDecimal.valueOf(2);

    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private PlayerSessionRepository playerSessionRepository;

    private GigaspaceCleanerService underTest;

    private Player offlinePlayer;

    @Before
    public void setup() {
        underTest = new GigaspaceCleanerService(gigaSpace, playerSessionRepository);
        offlinePlayer = new Player(OFFLINE_PLAYER_ID);
        final Player player1 = new Player(BigDecimal.ONE);
        final Player player3 = new Player(BigDecimal.valueOf(3));
        final Player player4 = new Player(BigDecimal.valueOf(4));

        when(gigaSpace.readMultiple(new Player(), Integer.MAX_VALUE)).thenReturn(new Player[]{player1, offlinePlayer, player3});
        when(playerSessionRepository.isOnline(player1.getPlayerId())).thenReturn(true);
        when(playerSessionRepository.isOnline(player3.getPlayerId())).thenReturn(true);
        when(playerSessionRepository.isOnline(player4.getPlayerId())).thenReturn(true);
    }

    @Test
    public void removeOfflinePlayersDoesNotRemovePlayersWithSession() {
        final Player player1 = new Player(BigDecimal.ONE);
        final Player player3 = new Player(BigDecimal.valueOf(3));
        when(gigaSpace.readMultiple(new Player(), Integer.MAX_VALUE)).thenReturn(new Player[]{player1, player3});

        underTest.removeOfflinePlayers();
        verify(gigaSpace, times(0)).clear(any());
    }

    @Test
    public void removeOfflinePlayersRemovesOfflinePlayersFromTheSpace() {
        underTest.removeOfflinePlayers();

        verify(gigaSpace, times(1)).clear(new Player(offlinePlayer.getPlayerId()));
    }

    @Test
    public void removeOfflinePlayersOfflinePlayersAchievementsFromTheSpace() {
        underTest.removeOfflinePlayers();

        verify(gigaSpace).clear(new PlayerAchievements(offlinePlayer.getPlayerId()));
    }

    @Test
    public void removeOfflinePlayersOfflinePlayersLevelsFromTheSpace() {
        underTest.removeOfflinePlayers();

        verify(gigaSpace).clear(new PlayerLevels(offlinePlayer.getPlayerId()));
    }
}
