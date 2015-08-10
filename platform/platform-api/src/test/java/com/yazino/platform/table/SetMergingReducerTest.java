package com.yazino.platform.table;

import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SetMergingReducerTest {

    private SetMergingReducer<String> underTest = new SetMergingReducer<String>();

    @SuppressWarnings("unchecked")
    @Test
    public void allRemoteSetsAreMergedIntoASingleResult() throws Exception {
        final Set<String> reducedResult = underTest.reduce(resultsWith(
                newHashSet("res1", "res2"),
                newHashSet("res3"),
                newHashSet("res4", "res5")),
                mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult, is(equalTo((Set) newHashSet("res1", "res2", "res3", "res4", "res5"))));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void nullResultsAreNotMergedIntoTheReducedResult() throws Exception {
        final Set<String> reducedResult = underTest.reduce(resultsWith(
                newHashSet("res1", "res2"),
                null,
                newHashSet("res4", "res5")),
                mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult, is(equalTo((Set) newHashSet("res1", "res2", "res4", "res5"))));
    }

    @Test
    public void aNullRemotingResultIsIgnored() throws Exception {
        final Set<String> reducedResult = underTest.reduce(null, mock(SpaceRemotingInvocation.class));

        assertThat(reducedResult.isEmpty(), is(true));
    }

    @SuppressWarnings("unchecked")
    private SpaceRemotingResult<Set<String>>[] resultsWith(final Set<String>... results) {
        final List<SpaceRemotingResult> remotingResults = new ArrayList<SpaceRemotingResult>();
        for (Set<String> result : results) {
            final SpaceRemotingResult remotingResult = mock(SpaceRemotingResult.class);
            when(remotingResult.getResult()).thenReturn(result);
            remotingResults.add(remotingResult);
        }
        return remotingResults.toArray(new SpaceRemotingResult[remotingResults.size()]);
    }

}
