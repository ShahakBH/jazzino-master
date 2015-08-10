package com.yazino.platform.processor.tournament;

import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentLeaderboardUpdateRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.tournament.TournamentTableService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TournamentLeaderboardUpdateProcessorTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(17L);

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private Tournament tournament;

    private TournamentHost tournamentHost;
    private TournamentLeaderboardUpdateProcessor unit;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tournamentHost = new TournamentHost(mock(TimeSource.class),
                mock(InternalWalletService.class),
                mock(TournamentTableService.class),
                tournamentRepository,
                mock(TableAllocatorFactory.class),
                mock(PlayerRepository.class),
                mock(PlayerSessionRepository.class),
                mock((DocumentDispatcher.class)),
                mock(TournamentPlayerStatisticPublisher.class));
        unit = new TournamentLeaderboardUpdateProcessor();
        unit.setTournamentHost(tournamentHost);
    }

    @Test
    public void runningTournamentsHaveLeaderboardsUpdated() {
        final TournamentLeaderboardUpdateRequest request = new TournamentLeaderboardUpdateRequest(TOURNAMENT_ID);
        when(tournamentRepository.findById(eq(TOURNAMENT_ID))).thenReturn(tournament);
        when(tournamentRepository.lock(eq(TOURNAMENT_ID))).thenReturn(tournament);

        tournament.updateLeaderboard(tournamentHost);
        unit.process(request);

        verify(tournamentRepository).save(eq(tournament), anyBoolean());
    }

}
