package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultPersistenceRequest;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import net.jini.core.lease.Lease;
import org.joda.time.DateTime;
import org.joda.time.Weeks;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.Mockito.verify;

public class GigaspaceTrophyLeaderboardResultRepositoryTest {
    private static final BigDecimal LEADERBOARD_ID = BigDecimal.valueOf(4534);
    private static final DateTime RESULT_TIME = new DateTime(2010, 10, 1, 1, 1, 1, 0);

    @Mock
    private GigaSpace gigaSpace;

    private GigaspaceTrophyLeaderboardResultRepository underTest;
    private TrophyLeaderboardResult trophyLeaderboardResult;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        trophyLeaderboardResult = new TrophyLeaderboardResult(
                LEADERBOARD_ID, RESULT_TIME, RESULT_TIME.plus(Weeks.weeks(1)), new ArrayList<TrophyLeaderboardPlayerResult>());

        underTest = new GigaspaceTrophyLeaderboardResultRepository(gigaSpace);
    }

    @Test
    public void saveWritesModelToSpaceAndGeneratesPersistenceRequest() {
        underTest.save(trophyLeaderboardResult);

        verify(gigaSpace).write(trophyLeaderboardResult, Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
        verify(gigaSpace).write(new TrophyLeaderboardResultPersistenceRequest(LEADERBOARD_ID, RESULT_TIME),
                Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }
}
