package com.yazino.platform.processor.tournament;

import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.TrophyLeaderboardPlayerUpdateDocument;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateResult;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceTrophyLeaderboardPlayerUpdateProcessorTest {
    private static final BigDecimal TROPHY_LEADERBOARD_ID = BigDecimal.valueOf(2342334);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(890789);
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(234234);

    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;
    @Mock
    private HostDocumentDispatcher hostDocumentDispatcher;
    @Mock
    private DestinationFactory destinationFactory;
    @Mock
    private Destination destination;
    @Mock
    private TrophyLeaderboard trophyLeaderboard;

    private GigaspaceTrophyLeaderboardPlayerUpdateProcessor underTest;

    @Before
    public void setUp() {
        when(trophyLeaderboardRepository.findById(TROPHY_LEADERBOARD_ID)).thenReturn(trophyLeaderboard);
        when(trophyLeaderboardRepository.lock(TROPHY_LEADERBOARD_ID)).thenReturn(trophyLeaderboard);

        when(trophyLeaderboard.update(aRequest())).thenReturn(anUpdateResult());

        when(destinationFactory.player(PLAYER_ID)).thenReturn(destination);

        underTest = new GigaspaceTrophyLeaderboardPlayerUpdateProcessor(trophyLeaderboardRepository,
                hostDocumentDispatcher, destinationFactory);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullTrophyLeaderboardRepository() {
        new GigaspaceTrophyLeaderboardPlayerUpdateProcessor(null,
                hostDocumentDispatcher, destinationFactory);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullHostDocumentDispatcher() {
        new GigaspaceTrophyLeaderboardPlayerUpdateProcessor(trophyLeaderboardRepository,
                null, destinationFactory);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullDestinationFactory() {
        new GigaspaceTrophyLeaderboardPlayerUpdateProcessor(trophyLeaderboardRepository,
                hostDocumentDispatcher, null);
    }

    @Test
    public void anUninitialisedClassIsIgnored() {
        new GigaspaceTrophyLeaderboardPlayerUpdateProcessor().process(aRequest());

        verifyZeroInteractions(trophyLeaderboardRepository, trophyLeaderboard);
    }

    @Test
    public void aNullRequestIsIgnored() {
        final TrophyLeaderboardPlayerUpdateRequest result = underTest.process(null);

        verifyZeroInteractions(trophyLeaderboardRepository, trophyLeaderboard);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void anInvalidTrophyLeaderboardIdIsIgnored() {
        reset(trophyLeaderboardRepository);

        final TrophyLeaderboardPlayerUpdateRequest result = underTest.process(aRequest());

        verifyZeroInteractions(trophyLeaderboard);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void theRequestIsReturnedToTheSpaceIfTheLockCannotBeObtained() {
        when(trophyLeaderboardRepository.lock(TROPHY_LEADERBOARD_ID)).thenThrow(new ConcurrentModificationException());

        final TrophyLeaderboardPlayerUpdateRequest result = underTest.process(aRequest());

        verifyZeroInteractions(trophyLeaderboard);
        assertThat(result, is(equalTo(aRequest())));
    }

    @Test
    public void anUpdateRequestInvokesUpdateOnTheLeaderboard() {
        final TrophyLeaderboardPlayerUpdateRequest result = underTest.process(aRequest());

        verify(trophyLeaderboard).update(aRequest());
        assertThat(result, is(nullValue()));
    }

    @Test
    public void anUpdateRequestLocksTheLeaderboard() {
        underTest.process(aRequest());

        verify(trophyLeaderboardRepository).lock(TROPHY_LEADERBOARD_ID);
    }

    @Test
    public void anUpdateSendsAnUpdateDocument() {
        underTest.process(aRequest());

        verify(hostDocumentDispatcher).send(new TrophyLeaderboardPlayerUpdateDocument(anUpdateResult(), destination));
    }

    private TrophyLeaderboardPlayerUpdateRequest aRequest() {
        return new TrophyLeaderboardPlayerUpdateRequest(TROPHY_LEADERBOARD_ID, TOURNAMENT_ID, PLAYER_ID,
                "aName", "aPictureUrl", 13, 233);
    }

    private TrophyLeaderboardPlayerUpdateResult anUpdateResult() {
        return new TrophyLeaderboardPlayerUpdateResult(TROPHY_LEADERBOARD_ID, TOURNAMENT_ID, 10, 1000, 1234, 50, 100);
    }
}
