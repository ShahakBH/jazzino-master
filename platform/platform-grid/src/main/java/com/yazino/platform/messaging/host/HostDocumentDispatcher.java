package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This is an abstraction layer to allow for easy testing of classes which
 * which to send a {@link HostDocument} immediately.
 */
@Service
public class HostDocumentDispatcher {

    private final DocumentDispatcher documentDispatcher;

    @Autowired
    public HostDocumentDispatcher(@Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        this.documentDispatcher = documentDispatcher;
    }

    public void send(final HostDocument hostDocument) {
        notNull(hostDocument, "hostDocument may not be null");

        hostDocument.send(documentDispatcher);
    }
}
