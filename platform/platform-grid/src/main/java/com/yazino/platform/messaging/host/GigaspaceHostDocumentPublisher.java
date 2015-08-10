package com.yazino.platform.messaging.host;


import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspaceHostDocumentPublisher implements HostDocumentPublisher {

    private final GigaSpace gigaSpace;

    @Autowired(required = true)
    public GigaspaceHostDocumentPublisher(@Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "gigaSpace may not be null");

        this.gigaSpace = gigaSpace;
    }

    @Override
    public void publish(final Collection<HostDocument> hostDocuments) {
        notNull(hostDocuments, "hostDocuments may not be null");

        if (hostDocuments.isEmpty()) {
            return;
        }

        gigaSpace.write(new HostDocumentWrapper(hostDocuments));
    }

}
