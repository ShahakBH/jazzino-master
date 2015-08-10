package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboardPersistenceRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultPersistenceRequest;
import com.yazino.platform.persistence.tournament.TrophyLeaderboardResultDao;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrophyLeaderboardResultPersistenceProcessorTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(4334);
    private static final DateTime RESULT_DATE = new DateTime(2010, 10, 1, 1, 1, 1, 0);

    @Mock
    private TrophyLeaderboardResultRepository trophyLeaderboardResultRepository;
    @Mock
    private TrophyLeaderboardResultDao trophyLeaderboardResultDao;
    @Mock
    private GigaSpace tournamentGigaSpace;

    private TrophyLeaderboardResultPersistenceProcessor unit;
    private TrophyLeaderboardResult trophyLeaderboardResult;
    private static final TrophyLeaderboardResultPersistenceRequest REQUEST = new TrophyLeaderboardResultPersistenceRequest(LEADERBOARD_ID, RESULT_DATE);

    @Before
    public void setUp() {
        unit = new TrophyLeaderboardResultPersistenceProcessor(trophyLeaderboardResultRepository, trophyLeaderboardResultDao, tournamentGigaSpace);

        trophyLeaderboardResult = new TrophyLeaderboardResult(LEADERBOARD_ID, RESULT_DATE, RESULT_DATE.plus(Weeks.weeks(1)),
                new ArrayList<TrophyLeaderboardPlayerResult>());
    }

    @Test
    public void persistenceRequestRetrievesModelFromSpaceAndPassesToDao() {
        when(trophyLeaderboardResultRepository.findByIdAndTime(LEADERBOARD_ID, RESULT_DATE)).thenReturn(trophyLeaderboardResult);

        final TrophyLeaderboardResultPersistenceRequest response = unit.processRequest(REQUEST);

        assertThat(response, is(nullValue()));
        verify(trophyLeaderboardResultDao).save(trophyLeaderboardResult);
    }

    @Test
    public void aSuccessfulPersistenceRequestRemovesMatchingQueriesFromSpace() {
        when(trophyLeaderboardResultRepository.findByIdAndTime(LEADERBOARD_ID, RESULT_DATE)).thenReturn(trophyLeaderboardResult);

        final TrophyLeaderboardResultPersistenceRequest response = unit.processRequest(REQUEST);

        assertThat(response, is(nullValue()));

        verify(tournamentGigaSpace, times(2)).takeMultiple(any(TrophyLeaderboardPersistenceRequest.class), eq(Integer.MAX_VALUE));
    }

    @Test
    public void persistenceRequestExitsQuietlyIfIdIsInvalid() {
        when(trophyLeaderboardResultRepository.findByIdAndTime(LEADERBOARD_ID, RESULT_DATE)).thenReturn(null);

        final TrophyLeaderboardResultPersistenceRequest response = unit.processRequest(REQUEST);

        assertThat(response, is(nullValue()));
        verifyZeroInteractions(trophyLeaderboardResultDao);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void persistenceRequestCatchesErrorAndUpdatesStatusOnRequestAndReturnsToSpace() {
        when(trophyLeaderboardResultRepository.findByIdAndTime(LEADERBOARD_ID, RESULT_DATE)).thenReturn(trophyLeaderboardResult);
        doThrow(new RuntimeException("anException")).when(trophyLeaderboardResultDao).save(trophyLeaderboardResult);

        final TrophyLeaderboardResultPersistenceRequest request = REQUEST;
        final TrophyLeaderboardResultPersistenceRequest response = unit.processRequest(request);

        request.setStatus(TrophyLeaderboardPersistenceRequest.STATUS_ERROR);

        assertThat(response, is(equalTo(request)));
        verify(trophyLeaderboardResultDao).save(trophyLeaderboardResult);
    }

}
