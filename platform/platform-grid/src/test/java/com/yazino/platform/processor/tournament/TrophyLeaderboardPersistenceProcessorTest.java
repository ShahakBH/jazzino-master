package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest;
import com.yazino.platform.persistence.tournament.TrophyLeaderboardDao;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest.Operation;
import static com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest.STATUS_ERROR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrophyLeaderboardPersistenceProcessorTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(4334);

    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;
    @Mock
    private TrophyLeaderboardDao trophyLeaderboardDao;
    @Mock
    private GigaSpace tournamentGigaSpace;

    private TrophyLeaderboardPersistenceProcessor unit;
    private TrophyLeaderboard trophyLeaderboard;

    @Before
    public void setUp() {
        unit = new TrophyLeaderboardPersistenceProcessor(trophyLeaderboardRepository, trophyLeaderboardDao, tournamentGigaSpace);

        trophyLeaderboard = new TrophyLeaderboard(LEADERBOARD_ID, "UnitTest", "BLACKJACK",
                new Interval(new DateTime(2009, 10, 1, 1, 1, 1, 0), new DateTime(2010, 10, 1, 1, 1, 1, 0)),
                new Period(Days.days(3)).toStandardDuration());
    }

    @Test
    public void persistenceSaveRequestRetrievesModelFromSpaceAndPassesToDao() {
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);

        final TrophyLeaderboardPersistenceRequest response = unit.processRequest(new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID));

        assertThat(response, is(nullValue()));
        verify(trophyLeaderboardDao).save(trophyLeaderboard);
    }

    @Test
    public void persistenceArchiveRequestRetrievesModelFromSpaceAndPassesToDaoAndThenRemovesFromSpace() {
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);

        final TrophyLeaderboardPersistenceRequest response = unit.processRequest(
                new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID, Operation.ARCHIVE));

        assertThat(response, is(nullValue()));
        verify(trophyLeaderboardDao).save(trophyLeaderboard);
        verify(trophyLeaderboardRepository).clear(LEADERBOARD_ID);
    }

    @Test
    public void aSuccessfulPersistenceRequestRemovesMatchingQueriesFromSpace() {
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);

        final TrophyLeaderboardPersistenceRequest response = unit.processRequest(new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID));

        assertThat(response, is(nullValue()));

        verify(tournamentGigaSpace, times(2)).takeMultiple(any(TrophyLeaderboardPersistenceRequest.class), eq(Integer.MAX_VALUE));
    }

    @Test
    public void persistenceRequestExitsQuietlyIfIdIsInvalid() {
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(null);

        final TrophyLeaderboardPersistenceRequest response = unit.processRequest(new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID));

        assertThat(response, is(nullValue()));
        verifyZeroInteractions(trophyLeaderboardDao);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void persistenceRequestCatchesErrorAndUpdatesStatusOnRequestAndReturnsToSpace() {
        when(trophyLeaderboardRepository.findById(LEADERBOARD_ID)).thenReturn(trophyLeaderboard);
        doThrow(new RuntimeException("anException")).when(trophyLeaderboardDao).save(trophyLeaderboard);

        final TrophyLeaderboardPersistenceRequest request = new TrophyLeaderboardPersistenceRequest(LEADERBOARD_ID);
        final TrophyLeaderboardPersistenceRequest response = unit.processRequest(request);

        request.setStatus(STATUS_ERROR);

        assertThat(response, is(equalTo(request)));
        verify(trophyLeaderboardDao).save(trophyLeaderboard);
    }

}
