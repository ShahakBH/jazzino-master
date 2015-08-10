package com.yazino.platform.messaging.host;


import com.yazino.platform.messaging.DocumentDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

public class ImmediateHostDocumentPublisher implements HostDocumentPublisher {

    private final DocumentDispatcher documentDispatcher;

    @Autowired(required = true)
    public ImmediateHostDocumentPublisher(
            @Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        this.documentDispatcher = documentDispatcher;
    }

    @Override
    public void publish(final Collection<HostDocument> hostDocuments) {
        notNull(hostDocuments, "hostDocuments may not be null");

        if (hostDocuments.isEmpty()) {
            return;
        }

        for (HostDocument hostDocument : hostDocuments) {
            hostDocument.send(documentDispatcher);
        }
    }

}
