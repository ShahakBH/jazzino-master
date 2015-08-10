package com.yazino.platform.processor.session;

import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessageReceived;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class InboxMessageReceivedProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(InboxMessageReceivedProcessor.class);

    private static final InboxMessageReceived TEMPLATE = new InboxMessageReceived();

    private final PlayerSessionRepository playerSessionRepository;
    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final InboxMessageRepository inboxMessageRepository;
    private final DestinationFactory destinationFactory;

    InboxMessageReceivedProcessor() {
        // CGLib constructor

        this.playerSessionRepository = null;
        this.hostDocumentDispatcher = null;
        this.inboxMessageRepository = null;
        this.destinationFactory = null;
    }

    @Autowired
    public InboxMessageReceivedProcessor(final PlayerSessionRepository playerSessionRepository,
                                         final InboxMessageRepository inboxMessageRepository,
                                         final HostDocumentDispatcher hostDocumentDispatcher,
                                         final DestinationFactory destinationFactory) {
        notNull(playerSessionRepository, "playerSessionRepository is null");
        notNull(inboxMessageRepository, "inboxMessageRepository is null");
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher is null");
        notNull(destinationFactory, "destinationFactory is null");

        this.playerSessionRepository = playerSessionRepository;
        this.inboxMessageRepository = inboxMessageRepository;
        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.destinationFactory = destinationFactory;
    }

    private boolean verifyInitialisation() {
        return hostDocumentDispatcher != null
                && inboxMessageRepository != null
                && playerSessionRepository != null
                && destinationFactory != null;
    }

    @EventTemplate
    public InboxMessageReceived template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processRequest(final InboxMessageReceived event) {
        if (!verifyInitialisation()) {
            LOG.error("Class was created with the CGLib constructor and is invalid for direct use");
            return;
        }

        try {
            LOG.debug("Received event {}", event);

            final InboxMessage message = event.getMessage();
            if (message == null) {
                LOG.warn("Empty event! Ignoring");
                return;
            }

            if (playerSessionRepository.isOnline(message.getPlayerId())) {
                LOG.debug("Player {} is online. Dispatching news event document", message.getPlayerId());

                hostDocumentDispatcher.send(new NewsEventHostDocument(message.getNewsEvent(),
                        destinationFactory.player(message.getPlayerId())));
                message.setRead(true);
                inboxMessageRepository.save(message);
            }

        } catch (Exception e) {
            LOG.error("Processing of event failed: {}", event, e);
        }
    }
}
