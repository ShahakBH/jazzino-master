package com.yazino.host.table.document;

import com.yazino.model.log.IncrementalLog;
import com.yazino.model.log.LogStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;

import java.math.BigDecimal;
import java.util.Set;

@Component("standaloneDocumentDispatcher")
@Qualifier("documentLog")
public class StandaloneDocumentDispatcher implements DocumentDispatcher, IncrementalLog {
    private final DocumentDispatcher delegate;

    private final LogStorage logStorage = new LogStorage();

    @Autowired
    public StandaloneDocumentDispatcher(@Qualifier("delegateDocumentDispatcher") final DocumentDispatcher delegate) {
        this.delegate = delegate;
    }

    @Override
    public void dispatch(final Document document) {
        logStorage.log(document.getType() + " to unknown destination", document.getBody());
        delegate.dispatch(document);
    }

    @Override
    public void dispatch(final Document document, final BigDecimal playerId) {
        logStorage.log(document.getType() + " to playerId " + String.valueOf(playerId), document.getBody());
        delegate.dispatch(document, playerId);
    }

    @Override
    public void dispatch(final Document document, final Set<BigDecimal> playerIds) {
        logStorage.log(document.getType() + " to playerIds " + String.valueOf(playerIds), document.getBody());
        delegate.dispatch(document, playerIds);
    }

    @Override
    public String nextIncrement() {
        return logStorage.popJSON();
    }
}
