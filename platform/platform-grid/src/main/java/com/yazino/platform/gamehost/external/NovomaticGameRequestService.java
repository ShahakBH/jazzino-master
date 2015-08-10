package com.yazino.platform.gamehost.external;

import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;

public class NovomaticGameRequestService implements ExternalGameRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(NovomaticGameRequestService.class);
    private final GigaSpace gigaSpace;

    @Autowired
    public NovomaticGameRequestService(@Qualifier("gigaSpace") GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }

    @Override
    public void postRequests(Collection<NovomaticRequest> requests) {
        LOG.debug("Writing requests into the space: " + requests);
        for (NovomaticRequest request : requests) {
            gigaSpace.write(request);
        }
    }
}
