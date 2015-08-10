package com.yazino.host.table.document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentPublisher;

import java.util.Collection;

@Component
public class StandaloneHostDocumentPublisher implements HostDocumentPublisher {

    private final DocumentDispatcher documentDispatcher;

    @Autowired
    public StandaloneHostDocumentPublisher(@Qualifier("standaloneDocumentDispatcher")
                                           final DocumentDispatcher documentDispatcher) {
        this.documentDispatcher = documentDispatcher;
    }

    @Override
    public void publish(final Collection<HostDocument> hostDocuments) {
        if (hostDocuments.isEmpty()) {
            return;
        }

        for (HostDocument hostDocument : hostDocuments) {
            hostDocument.send(documentDispatcher);
        }
    }

}
