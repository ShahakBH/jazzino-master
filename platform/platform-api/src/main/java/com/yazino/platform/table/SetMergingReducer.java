package com.yazino.platform.table;

import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SetMergingReducer<T> implements RemoteResultReducer<Set<T>, Set<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(SetMergingReducer.class);

    @SuppressWarnings("unchecked")
    @Override
    public Set<T> reduce(final SpaceRemotingResult[] spaceRemotingResults,
                         final SpaceRemotingInvocation spaceRemotingInvocation) throws Exception {
        final Set<T> mergedResults = new HashSet<T>();

        if (spaceRemotingResults != null) {
            for (SpaceRemotingResult remotingResult : spaceRemotingResults) {
                final Object result = remotingResult.getResult();
                if (result != null && result instanceof Collection) {
                    mergedResults.addAll((Collection<T>) result);
                } else {
                    LOG.warn("Received invalid remote result: {}", result);
                }
            }
        }

        return mergedResults;
    }
}
