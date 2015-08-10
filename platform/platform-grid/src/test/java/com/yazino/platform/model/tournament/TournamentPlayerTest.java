package com.yazino.platform.model.tournament;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TournamentPlayerTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(234);
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(12);
    private static final long CURRENT_TIMESTAMP = 123L;
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(234999);
    private static final int LEADERBOARD_POSITION = 3;
    private static final String GAME_TYPE = "aGameType";
    private static final int NUMBER_OF_PLAYERS = 5;

    @Mock
    private TournamentHost tournamentHost;
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentPlayer underTest;

    @Before
    public void setUp() {
        underTest = new TournamentPlayer();
        underTest.setPlayerId(PLAYER_ID);
        underTest.setAccountId(BigDecimal.ONE);
        underTest.setTableId(TABLE_ID);
        underTest.setStatus(TournamentPlayerStatus.ACTIVE);
        underTest.setLeaderboardPosition(LEADERBOARD_POSITION);

        final SettableTimeSource timeSource = new SettableTimeSource();
        timeSource.setMillis(CURRENT_TIMESTAMP);

        when(tournamentHost.getInternalWalletService()).thenReturn(internalWalletService);
        when(tournamentHost.getTimeSource()).thenReturn(timeSource);
        when(tournamentHost.getTournamentRepository()).thenReturn(tournamentRepository);
    }

    @Test
    public void eliminateTournamentPlayerShouldSaveAnEliminationRequestToTheRepository() {
        underTest.eliminated(tournamentHost, TOURNAMENT_ID, TournamentPlayer.EliminationReason.OFFLINE, GAME_TYPE, NUMBER_OF_PLAYERS);

        verify(tournamentRepository).playerEliminatedFrom(TOURNAMENT_ID, PLAYER_ID, GAME_TYPE, NUMBER_OF_PLAYERS, LEADERBOARD_POSITION);
    }

    @Test
    public void eliminateTournamentPlayerShouldUpdateStatusToEliminated() {
        underTest.eliminated(tournamentHost, TOURNAMENT_ID, TournamentPlayer.EliminationReason.OFFLINE, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getStatus(), is(equalTo(TournamentPlayerStatus.ELIMINATED)));
    }

    @Test
    public void eliminateTournamentPlayerShouldRecordTheEliminationReason() {
        underTest.eliminated(tournamentHost, TOURNAMENT_ID, TournamentPlayer.EliminationReason.OFFLINE, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getEliminationReason(), is(equalTo(TournamentPlayer.EliminationReason.OFFLINE)));
    }

    @Test
    public void eliminateTournamentPlayerShouldRecordTheEliminationDate() {
        underTest.eliminated(tournamentHost, TOURNAMENT_ID, TournamentPlayer.EliminationReason.OFFLINE, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getEliminationTimestamp(), is(equalTo(new DateTime(CURRENT_TIMESTAMP))));
    }

    @Test
    public void eliminateTournamentPlayerShouldClearTheTableId() {
        underTest.eliminated(tournamentHost, TOURNAMENT_ID, TournamentPlayer.EliminationReason.OFFLINE, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getTableId(), is(nullValue()));
    }

    @Test
    public void terminatingAPlayerShouldClearTheTableId() {
        underTest.terminate(tournamentHost, TOURNAMENT_ID, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getTableId(), is(nullValue()));
    }

    @Test
    public void terminateTournamentPlayerShouldSaveAnEliminationRequestToTheRepository() {
        underTest.terminate(tournamentHost, TOURNAMENT_ID, GAME_TYPE, NUMBER_OF_PLAYERS);

        verify(tournamentRepository).playerEliminatedFrom(TOURNAMENT_ID, PLAYER_ID, GAME_TYPE, NUMBER_OF_PLAYERS, LEADERBOARD_POSITION);
    }

    @Test
    public void terminateTournamentPlayerShouldUpdateStatusToTerminated() {
        underTest.terminate(tournamentHost, TOURNAMENT_ID, GAME_TYPE, NUMBER_OF_PLAYERS);

        assertThat(underTest.getStatus(), is(equalTo(TournamentPlayerStatus.TERMINATED)));
    }

}
