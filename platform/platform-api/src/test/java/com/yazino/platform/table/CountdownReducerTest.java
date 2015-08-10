package com.yazino.platform.table;

import org.junit.Before;
import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingResult;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CountdownReducerTest {

    private CountdownReducer underTest;

    @Before
    public void setUp() {
        underTest = new CountdownReducer();
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Test
    public void theFirstCountdownResultIsReturnedByTheReducer() throws Exception {
        final Long countdown = underTest.reduce(new SpaceRemotingResult[]{
                resultOf(123L),
                resultOf(124L)
        }, null);

        assertThat(countdown, is(equalTo(123L)));
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Test
    public void ifNoResultsArePresentThenNullIsReturned() throws Exception {
        final Long countdown = underTest.reduce(new SpaceRemotingResult[0], null);

        assertThat(countdown, is(nullValue()));
    }

    @SuppressWarnings({"unchecked"})
    private static SpaceRemotingResult<Long> resultOf(final Long countdown) {
        final SpaceRemotingResult<Long> remotingResult = mock(SpaceRemotingResult.class);
        when(remotingResult.getResult()).thenReturn(countdown);
        return remotingResult;
    }

}
