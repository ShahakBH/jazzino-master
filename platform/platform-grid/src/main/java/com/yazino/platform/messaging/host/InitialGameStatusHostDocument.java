package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.Destination;

public class InitialGameStatusHostDocument extends ObservableHostDocument {
    private static final long serialVersionUID = -1259063285258379149L;

    public InitialGameStatusHostDocument(final ObservableDocumentContext context,
                                         final Destination destination) {
        super(DocumentType.INITIAL_GAME_STATUS, context, destination, true);
    }
}
