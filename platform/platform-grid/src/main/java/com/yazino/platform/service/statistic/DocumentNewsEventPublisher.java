package com.yazino.platform.service.statistic;

import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yazino.game.api.NewsEvent;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class DocumentNewsEventPublisher implements NewsEventPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentNewsEventPublisher.class);

    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final String partnerId;
    private final DestinationFactory destinationFactory;

    @Autowired
    public DocumentNewsEventPublisher(final HostDocumentDispatcher hostDocumentDispatcher,
                                      @Value("${strata.partner.id}") final String partnerId,
                                      @Qualifier("destinationFactory") final DestinationFactory destinationFactory) {
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher is null");
        notNull(partnerId, "partnerId is null");
        notNull(destinationFactory, "destinationFactory is null");

        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.partnerId = partnerId;
        this.destinationFactory = destinationFactory;
    }

    public void send(final NewsEvent... events) {
        if (events == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No events to send");
            }
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending news events: " + ArrayUtils.toString(events));
        }

        for (NewsEvent event : events) {
            hostDocumentDispatcher.send(new NewsEventHostDocument(partnerId, event,
                    destinationFactory.player(event.getPlayerId())));
        }
    }

}
