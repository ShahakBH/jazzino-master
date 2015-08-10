package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;

public class TournamentSettlementProcessorTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.ONE;
    private static final String GAME_TYPE = "BLACKJACK";

    private final Tournament tournament = mock(Tournament.class);
    private final TournamentHost tournamentHost = mock(TournamentHost.class);
    private final TournamentRepository tournamentRepository = mock(TournamentRepository.class);
    private final TournamentSummaryRepository tournamentSummaryRepository = mock(TournamentSummaryRepository.class);
    private final TournamentToSummaryTransformer transformer = mock(TournamentToSummaryTransformer.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);

    private final TournamentSettlementProcessor processor = new TournamentSettlementProcessor(
            tournamentHost, tournamentSummaryRepository,
            new TournamentToSummaryTransformer(playerRepository));

    @Before
    public void setUp() {
        when(tournamentHost.getTournamentRepository()).thenReturn(tournamentRepository);
        processor.setTournamentToSummaryTransformer(transformer);
    }

    @Test
    public void shouldSettleAndSendNewsEvents() {
        final TournamentSummary summary = new TournamentSummary();
        summary.setGameType(GAME_TYPE);

        when(transformer.apply(tournament)).thenReturn(summary);

        when(tournament.getTournamentId()).thenReturn(TOURNAMENT_ID);

        final Object[] result = processor.process(tournament);
        verify(tournament).settle(tournamentHost);
        verify(tournamentRepository).save(tournament, true);
        verify(tournamentSummaryRepository).save(summary);
        assertThat(asList(result), hasItem(tournament));
    }
}
