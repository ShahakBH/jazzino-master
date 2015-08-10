package com.yazino.platform.table;

import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegerAdditionReducerTest {

    private IntegerAdditionReducer underTest = new IntegerAdditionReducer();

    @SuppressWarnings("unchecked")
    @Test
    public void allRemoteSetsAreMergedIntoASingleResult() throws Exception {
        final Integer reducedResult = underTest.reduce(resultsWith(4, 5, 6, 7),
                mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult, is(equalTo(22)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullResultsAreNotMergedIntoTheReducedResult() throws Exception {
        final Integer reducedResult = underTest.reduce(resultsWith(4, 5, null, 7),
                mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult, is(equalTo(16)));
    }

    @Test
    public void aNullRemotingResultIsIgnored() throws Exception {
        final Integer reducedResult = underTest.reduce(null, mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult, is(equalTo(0)));
    }

    @SuppressWarnings("unchecked")
    private SpaceRemotingResult<Integer>[] resultsWith(final Integer... results) {
        final List<SpaceRemotingResult> remotingResults = new ArrayList<SpaceRemotingResult>();
        for (Integer result : results) {
            final SpaceRemotingResult remotingResult = mock(SpaceRemotingResult.class);
            when(remotingResult.getResult()).thenReturn(result);
            remotingResults.add(remotingResult);
        }
        return remotingResults.toArray(new SpaceRemotingResult[remotingResults.size()]);
    }
}
