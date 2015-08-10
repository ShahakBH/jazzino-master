package com.yazino.platform.messaging.dispatcher;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentWrapper;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Service("spaceDocumentDispatcher")
public class GigaspaceDocumentDispatcher implements DocumentDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceDocumentDispatcher.class);

    private final GigaSpace gigaSpace;
    private final Routing routing;

    @Autowired
    public GigaspaceDocumentDispatcher(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                       final Routing routing) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(routing, "routing may not be null");

        this.gigaSpace = gigaSpace;
        this.routing = routing;
    }

    public void dispatch(final Document document) {
        LOG.debug("Writing document to space: {}", document);
        writeDocument(new DocumentWrapper(document));
    }

    public void dispatch(final Document document,
                         final BigDecimal playerId) {
        dispatch(document, newHashSet(playerId));
    }

    public void dispatch(final Document document,
                         final Set<BigDecimal> playerIds) {
        LOG.debug("Writing document to space: {} for Players: {}", document, playerIds);
        writeDocument(new DocumentWrapper(document, playerIds));
    }

    private void writeDocument(final DocumentWrapper documentWrapper) {
        if (documentWrapper.getPlayerId() == null) {
            documentWrapper.setPlayerId(BigDecimal.valueOf(routing.partitionId()));
        }
        gigaSpace.write(documentWrapper);
    }

}
