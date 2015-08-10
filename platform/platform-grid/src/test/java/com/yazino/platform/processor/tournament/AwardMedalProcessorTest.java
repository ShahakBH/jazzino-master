package com.yazino.platform.processor.tournament;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.AwardMedalHostDocument;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.model.tournament.AwardMedalsRequest;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import com.yazino.platform.service.tournament.AwardTrophyService;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AwardMedalProcessorTest {
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.TEN;

    @Mock
    private TournamentSummaryRepository tournamentSummaryRepository;
    @Mock
    private TrophyRepository trophyRepository;
    @Mock
    private AwardTrophyService awardTrophyService;
    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;

    private AwardMedalProcessor underTest;

    @Before
    public void setUp() {
        when(destinationFactory.player(any(BigDecimal.class))).thenReturn(destination);

        underTest = new AwardMedalProcessor(tournamentSummaryRepository, trophyRepository,
                awardTrophyService, hostDocumentDispatcher, destinationFactory);
    }

    @Test
    public void shouldGiveTrophyToTopPlayers() {
        final TournamentSummary tournamentSummary = createSummary(4);
        when(tournamentSummaryRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(tournamentSummary);
        when(trophyRepository.findByNameAndGameType("medal_1", GAME_TYPE)).thenReturn(trophy(1));
        when(trophyRepository.findByNameAndGameType("medal_2", GAME_TYPE)).thenReturn(trophy(2));
        when(trophyRepository.findByNameAndGameType("medal_3", GAME_TYPE)).thenReturn(trophy(3));
        when(trophyRepository.findByNameAndGameType("medal_4", GAME_TYPE)).thenReturn(trophy(4));
        underTest.process(new AwardMedalsRequest(TOURNAMENT_ID, GAME_TYPE));
        verify(awardTrophyService).awardTrophy(bd(1), bd(1));
        verify(awardTrophyService).awardTrophy(bd(2), bd(2));
        verify(awardTrophyService).awardTrophy(bd(3), bd(3));
        verify(hostDocumentDispatcher).send(new AwardMedalHostDocument(bd(1), GAME_TYPE, destination));
        verify(hostDocumentDispatcher).send(new AwardMedalHostDocument(bd(2), GAME_TYPE, destination));
        verify(hostDocumentDispatcher).send(new AwardMedalHostDocument(bd(3), GAME_TYPE, destination));
        verifyNoMoreInteractions(awardTrophyService);
        verifyNoMoreInteractions(hostDocumentDispatcher);
    }

    private BigDecimal bd(final int val) {
        return BigDecimal.valueOf(val);
    }

    @Test
    public void shouldIgnoreIfTrophyNotFound() {
        final TournamentSummary tournamentSummary = createSummary(2);
        when(tournamentSummaryRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(tournamentSummary);
        when(trophyRepository.findByNameAndGameType("medal_1", GAME_TYPE)).thenReturn(null);
        when(trophyRepository.findByNameAndGameType("medal_2", GAME_TYPE)).thenReturn(trophy(2));
        underTest.process(new AwardMedalsRequest(TOURNAMENT_ID, GAME_TYPE));
        verify(awardTrophyService).awardTrophy(bd(2), bd(2));
//		verify(communityService).publishPlayerMedals(BigDecimal.valueOf(2), GAME_TYPE);
        verifyNoMoreInteractions(awardTrophyService);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void shouldIgnoreIfExceptionsHappen() {
        when(tournamentSummaryRepository.findByTournamentId(TOURNAMENT_ID)).thenThrow(new RuntimeException("some error"));
        underTest.process(new AwardMedalsRequest(TOURNAMENT_ID, GAME_TYPE));
        verifyNoMoreInteractions(awardTrophyService);
    }

    private Trophy trophy(int id) {
        final Trophy trophy = new Trophy();
        trophy.setId(BigDecimal.valueOf(id));
        return trophy;
    }

    private TournamentSummary createSummary(final int numberOfPlayers) {
        TournamentSummary tournamentSummary = new TournamentSummary();
        tournamentSummary.setGameType(GAME_TYPE);
        for (int i = 1; i <= numberOfPlayers; i++) {
            tournamentSummary.addPlayer(new TournamentPlayerSummary(
                    BigDecimal.valueOf(i), i, "player " + i, BigDecimal.valueOf(i), "picture" + i));
        }
        return tournamentSummary;
    }
}

