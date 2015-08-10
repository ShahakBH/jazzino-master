package com.yazino.platform.table;

import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegerAdditionReducer implements RemoteResultReducer<Integer, Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(IntegerAdditionReducer.class);

    @Override
    public Integer reduce(final SpaceRemotingResult<Integer>[] spaceRemotingResults,
                          final SpaceRemotingInvocation spaceRemotingInvocation) throws Exception {
        int reducedResult = 0;

        if (spaceRemotingResults != null) {
            for (SpaceRemotingResult remotingResult : spaceRemotingResults) {
                final Object result = remotingResult.getResult();
                if (result != null && result instanceof Number) {
                    reducedResult += ((Number) result).intValue();
                } else {
                    LOG.warn("Received invalid remote result: {}", result);
                }
            }
        }

        return reducedResult;
    }
}
