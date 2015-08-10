package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.Destination;

public class ErrorHostDocument extends ObservableHostDocument {
    private static final long serialVersionUID = 7700495537925987204L;

    public ErrorHostDocument(final ObservableDocumentContext context,
                             final Destination destination) {
        super(DocumentType.ERROR, context, destination, true);
    }
}
