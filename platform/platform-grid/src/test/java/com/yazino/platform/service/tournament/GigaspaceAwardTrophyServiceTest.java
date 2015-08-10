package com.yazino.platform.service.tournament;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceAwardTrophyServiceTest {
    private static final BigDecimal PLAYER_ID_1 = BigDecimal.ONE;
    private static final BigDecimal TROPHY_ID_1 = BigDecimal.valueOf(100);

    @Mock
    private PlayerTrophyRepository playerTrophyRepository;
    @Mock
    private TrophyRepository trophyRepository;
    @Mock
    private PlayerRepository playerRepository;

    private final TimeSource timeSource = new SettableTimeSource(100);

    private GigaspaceAwardTrophyService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceAwardTrophyService(playerTrophyRepository, trophyRepository, playerRepository, timeSource);

        when(playerRepository.findById(PLAYER_ID_1)).thenReturn(
                new Player(PLAYER_ID_1, "aPlayer", BigDecimal.valueOf(20), null, null, null, null));
        when(trophyRepository.findById(TROPHY_ID_1)).thenReturn(
                new Trophy(TROPHY_ID_1, "aTrophy", "aGameType", "anImage"));
    }

    @Test
    public void ensureTrophyRepositoryIsUpdated() throws Exception {
        final DateTime awardTime = new DateTime(timeSource.getCurrentTimeStamp());
        PlayerTrophy playerTrophy = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, awardTime);

        underTest.awardTrophy(PLAYER_ID_1, TROPHY_ID_1);

        verify(playerTrophyRepository).save(playerTrophy);
    }

    @Test(expected = NullPointerException.class)
    public void ensureExceptionThrownWhenPlayerDoesntExist() throws Exception {
        reset(playerRepository);
        when(playerRepository.findById(PLAYER_ID_1)).thenReturn(null);

        underTest.awardTrophy(PLAYER_ID_1, TROPHY_ID_1);
    }

    @Test(expected = NullPointerException.class)
    public void ensureExceptionThrownWhenTrophyDoesNotExist() throws Exception {
        reset(trophyRepository);
        when(trophyRepository.findById(TROPHY_ID_1)).thenReturn(null);

        underTest.awardTrophy(PLAYER_ID_1, TROPHY_ID_1);
    }
}
