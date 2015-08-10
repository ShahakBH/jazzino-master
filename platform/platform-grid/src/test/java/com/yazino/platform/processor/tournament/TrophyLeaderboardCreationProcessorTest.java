package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationResponse;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static com.yazino.platform.model.tournament.TrophyLeaderboardCreationResponse.Status;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TrophyLeaderboardCreationProcessorTest {
    private static final BigDecimal NEW_ID = BigDecimal.valueOf(435);
    private static final String REQUEST_SPACE_ID = "aSpaceId";

    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private TrophyLeaderboardRepository trophyLeaderboardRepository;

    private TrophyLeaderboardCreationProcessor unit;
    private TrophyLeaderboard leaderboard;
    private TrophyLeaderboardCreationRequest request;

    @Before
    public void setUp() throws Exception {
        leaderboard = new TrophyLeaderboard();
        request = new TrophyLeaderboardCreationRequest(leaderboard);
        request.setSpaceId(REQUEST_SPACE_ID);

        when(sequenceGenerator.next()).thenReturn(NEW_ID);

        unit = new TrophyLeaderboardCreationProcessor(trophyLeaderboardRepository, sequenceGenerator);
    }

    @Test
    public void templateIsEmptyRequest() {
        final TrophyLeaderboardCreationRequest template = unit.eventTemplate();

        assertThat(template, is(equalTo(new TrophyLeaderboardCreationRequest())));
    }

    @Test
    public void processReturnsNullOnNullRequest() {
        final TrophyLeaderboardCreationResponse response = unit.process(null);

        assertThat(response, is(nullValue()));
    }

    @Test
    public void createRequestGetsNewIdAndSavesToRepository() {
        final TrophyLeaderboardCreationResponse response = unit.process(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getStatus(), is(Status.SUCCESS));
        assertThat(leaderboard.getId(), is(equalTo(NEW_ID)));

        verify(trophyLeaderboardRepository).save(leaderboard);
    }

    @Test
    public void createRequestSetsRequestIdInResponse() {
        final TrophyLeaderboardCreationResponse response = unit.process(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getRequestSpaceId(), is(equalTo(REQUEST_SPACE_ID)));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void createRequestCatchesExceptionAndReturnsError() {
        doThrow(new RuntimeException("TestEx")).when(trophyLeaderboardRepository).save(any(TrophyLeaderboard.class));

        final TrophyLeaderboardCreationResponse response = unit.process(request);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getTrophyLeaderboardId(), is(nullValue()));
        assertThat(response.getStatus(), is(Status.FAILURE));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void createRequestWithErrorSetsRequestIdInResponse() {
        final TrophyLeaderboardCreationResponse response = unit.process(request);

        doThrow(new RuntimeException("TestEx")).when(trophyLeaderboardRepository).save(leaderboard);

        assertThat(response, is(not(nullValue())));
        assertThat(response.getRequestSpaceId(), is(equalTo(REQUEST_SPACE_ID)));
    }
}
