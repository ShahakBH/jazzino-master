package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayers;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TournamentCancellationProcessorTest {

    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(17l);

    @Mock
    private TournamentHost tournamentHost;
    @Mock
    private Tournament tournament;
    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentCancellationProcessor underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(tournamentHost.getTournamentRepository()).thenReturn(tournamentRepository);
        when(tournamentHost.getTimeSource()).thenReturn(new SettableTimeSource());

        underTest = new TournamentCancellationProcessor(tournamentHost);
    }

    @Test
    public void processShouldCallCancelSaveAndReturnTournament() throws TournamentException {
        when(tournament.getTournamentId()).thenReturn(TOURNAMENT_ID);
        when(tournament.getTournamentStatus()).thenReturn(TournamentStatus.CANCELLED);
        when(tournament.getPartnerId()).thenReturn("partner id");
        when(tournament.getName()).thenReturn("name");
        when(tournament.getStartTimeStamp()).thenReturn(new DateTime());
        final TournamentPlayers tournamentPlayers = new TournamentPlayers();
        tournamentPlayers.add(new TournamentPlayer(BigDecimal.ONE, "playerName"));

        final Tournament result = underTest.process(tournament);

        verify(tournamentRepository).save(same(tournament), anyBoolean());
        verify(tournament).cancel(same(tournamentHost));

        assertSame(tournament, result);
    }
}
