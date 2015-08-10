package com.yazino.platform.processor.tournament;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.event.message.LeaderboardEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultingRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.AwardTrophyService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class TrophyLeaderboardResultingProcessorTest {
    private static final int START_TIME = 1000;
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(34234);
    private static final BigDecimal ARCHIVAL_LEADERBOARD_ID = BigDecimal.valueOf(34236);

    private final TrophyLeaderboardRepository trophyLeaderboardRepository = mock(TrophyLeaderboardRepository.class);
    private final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository = mock(TrophyLeaderboardResultRepository.class);
    private final InternalWalletService internalWalletService = mock(InternalWalletService.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final AwardTrophyService awardTrophyService = mock(AwardTrophyService.class);
    private final InboxMessageRepository inboxMessageRepository = mock(InboxMessageRepository.class);
    private final TrophyRepository trophyRepository = mock(TrophyRepository.class);
    private final QueuePublishingService<LeaderboardEvent> eventService = mock(QueuePublishingService.class);
    private final AuditLabelFactory auditor = mock(AuditLabelFactory.class);
    private final TrophyLeaderboard trophyLeaderboard = mock(TrophyLeaderboard.class);
    private final TrophyLeaderboard archivalTrophyLeaderboard = mock(TrophyLeaderboard.class);

    private SettableTimeSource timeSource;
    private TrophyLeaderboardResultingRequest request;
    private TrophyLeaderboardResultingProcessor unit;

    @Before
    public void setUp() throws Exception {
        timeSource = new SettableTimeSource(START_TIME);

        request = new TrophyLeaderboardResultingRequest(LEADERBOARD_ID);
        request.setSpaceId("aSpaceId");

        when(trophyLeaderboard.getActive()).thenReturn(Boolean.TRUE);
        when(trophyLeaderboard.getCurrentCycleEnd()).thenReturn(new DateTime(START_TIME - 10));
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);
        when(trophyLeaderboardRepository.lock(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);

        when(archivalTrophyLeaderboard.getActive()).thenReturn(Boolean.TRUE);
        when(archivalTrophyLeaderboard.getCurrentCycleEnd()).thenReturn(new DateTime(START_TIME - 10)).thenReturn(new DateTime(START_TIME - 10)).thenReturn(null);
        when(trophyLeaderboardRepository.findById(ARCHIVAL_LEADERBOARD_ID)).thenReturn(archivalTrophyLeaderboard);
        when(trophyLeaderboardRepository.lock(ARCHIVAL_LEADERBOARD_ID)).thenReturn(archivalTrophyLeaderboard);

        unit = new TrophyLeaderboardResultingProcessor(trophyLeaderboardRepository, trophyLeaderboardResultRepository,
                internalWalletService, playerRepository, awardTrophyService, inboxMessageRepository, trophyRepository, eventService, auditor, timeSource);
    }

    @Test
    public void templateIsEmptyObject() {
        final TrophyLeaderboardResultingRequest template = unit.eventTemplate();

        assertThat(template, is(equalTo(new TrophyLeaderboardResultingRequest())));
    }

    @Test
    public void processReadsThenLocksAndSavesLeaderboardIfEndPeriodRemainsValid() {
        unit.process(request);

        verify(trophyLeaderboardRepository).findById(LEADERBOARD_ID);
        verify(trophyLeaderboardRepository).lock(LEADERBOARD_ID);
        verify(trophyLeaderboardRepository).save(trophyLeaderboard);
    }

    @Test
    public void processReadsThenLocksAndArchivesLeaderboardIfEndPeriodIsUnset() {
        request.setTrophyLeaderboardId(ARCHIVAL_LEADERBOARD_ID);

        unit.process(request);

        verify(trophyLeaderboardRepository).findById(ARCHIVAL_LEADERBOARD_ID);
        verify(trophyLeaderboardRepository).lock(ARCHIVAL_LEADERBOARD_ID);
        verify(trophyLeaderboardRepository).archive(archivalTrophyLeaderboard);
    }

    @Test
    public void processCallsResultOnLeaderboard() throws WalletServiceException {
        unit.process(request);

        verify(trophyLeaderboard).result(new TrophyLeaderboardResultContext(
                trophyLeaderboardResultRepository, internalWalletService, playerRepository, awardTrophyService, inboxMessageRepository, trophyRepository, auditor, timeSource));
    }

    @Test
    public void processReturnsWithoutErrorIfLeaderboardIsInvalid() {
        reset(trophyLeaderboardRepository);

        unit.process(request);
    }

    @Test
    public void inactiveLeaderboardsAreIgnored() {
        when(trophyLeaderboard.getActive()).thenReturn(Boolean.FALSE);

        verifyNoMoreInteractions(trophyLeaderboardRepository);

        unit.process(request);
    }

    @Test
    public void resultedLeaderboardsAreIgnored() {
        when(trophyLeaderboard.getCurrentCycleEnd()).thenReturn(new DateTime(START_TIME + 10));

        verifyNoMoreInteractions(trophyLeaderboardRepository);

        unit.process(request);
    }

    @Test
    public void processReturnsWithoutErrorIfRequestIsNull() {
        verifyNoMoreInteractions(trophyLeaderboardRepository);

        unit.process(null);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test(expected = ConcurrentModificationException.class)
    public void lockFailureIsPropagatedToSpace() {
        reset(trophyLeaderboardRepository);
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);
        when(trophyLeaderboardRepository.lock(LEADERBOARD_ID)).thenThrow(new ConcurrentModificationException("anException"));

        unit.process(request);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void exceptionsInResultingAreNotPropagated() throws WalletServiceException {
        doThrow(new RuntimeException("anException")).when(trophyLeaderboard).result(
                new TrophyLeaderboardResultContext(trophyLeaderboardResultRepository, internalWalletService, playerRepository, awardTrophyService, inboxMessageRepository, trophyRepository, auditor, timeSource));

        unit.process(request);
    }

}
