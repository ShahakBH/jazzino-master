package com.yazino.platform.messaging.host;

import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.Destination;

public class GameStatusHostDocument extends ObservableHostDocument {
    private static final long serialVersionUID = 8831603238467170621L;

    public GameStatusHostDocument(final ObservableDocumentContext context,
                                  final Destination destination) {
        super(DocumentType.GAME_STATUS, context, destination, false);
    }
}
