package com.yazino.platform.table;

import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

public class CountdownReducer implements RemoteResultReducer<Long, Long> {

    @Override
    public Long reduce(final SpaceRemotingResult<Long>[] spaceRemotingResults,
                       final SpaceRemotingInvocation spaceRemotingInvocation) throws Exception {
        if (spaceRemotingResults != null && spaceRemotingResults.length > 0) {
            return spaceRemotingResults[0].getResult();
        }
        return null;
    }

}
