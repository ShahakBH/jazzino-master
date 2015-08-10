package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.tournament.TournamentPlayerEliminationRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceTournamentPlayerEliminationProcessorTest {
    private static final int LEADERBOARD_POSITION = 13;
    private static final String GAME_TYPE = "aGameType";
    private static final int NUMBER_OF_PLAYERS = 2;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(890789);
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(234234);

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;

    private GigaspaceTournamentPlayerEliminationProcessor underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis());

        when(playerRepository.findById(PLAYER_ID)).thenReturn(aPlayer());
        when(trophyLeaderboardRepository.findCurrentAndActiveWithGameType(new DateTime(), GAME_TYPE))
                .thenReturn(newHashSet(aTrophyLeaderboard(1), aTrophyLeaderboard(2)));

        underTest = new GigaspaceTournamentPlayerEliminationProcessor(playerRepository, trophyLeaderboardRepository);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void anUninitialisedClassIsIgnored() {
        new GigaspaceTournamentPlayerEliminationProcessor().process(aRequest());

        verifyZeroInteractions(playerRepository, trophyLeaderboardRepository);
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(playerRepository, trophyLeaderboardRepository);
    }

    @Test
    public void anUpdateRequestForEachValidLeaderboardIsWrittenToTheRepository() {
        underTest.process(aRequest());

        verify(trophyLeaderboardRepository).requestUpdate(
                BigDecimal.valueOf(1), TOURNAMENT_ID, PLAYER_ID, "aName", "aPictureUrl", LEADERBOARD_POSITION, NUMBER_OF_PLAYERS);
        verify(trophyLeaderboardRepository).requestUpdate(
                BigDecimal.valueOf(2), TOURNAMENT_ID, PLAYER_ID, "aName", "aPictureUrl", LEADERBOARD_POSITION, NUMBER_OF_PLAYERS);
    }

    private TrophyLeaderboard aTrophyLeaderboard(final int id) {
        return new TrophyLeaderboard(BigDecimal.valueOf(id));
    }

    private Player aPlayer() {
        return new Player(PLAYER_ID, "aName", BigDecimal.ONE, "aPictureUrl", null, null, null);
    }

    private TournamentPlayerEliminationRequest aRequest() {
        return new TournamentPlayerEliminationRequest(TOURNAMENT_ID, PLAYER_ID, GAME_TYPE, NUMBER_OF_PLAYERS, LEADERBOARD_POSITION);
    }

}
