package com.yazino.platform.processor.chat;


import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentPublisher;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatChannelStatusHostDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class ChatChannelAggregateWorker {
    private final HostDocumentPublisher hostDocumentPublisher;
    private final DestinationFactory destinationFactory;

    @Autowired(required = true)
    public ChatChannelAggregateWorker(final HostDocumentPublisher hostDocumentPublisher,
                                      final DestinationFactory destinationFactory) {
        notNull(hostDocumentPublisher, "hostDocumentPublisher may not be null");
        notNull(destinationFactory, "destinationFactory may not be null");

        this.hostDocumentPublisher = hostDocumentPublisher;
        this.destinationFactory = destinationFactory;
    }

    public void dispatchToAllParticipants(final ChatChannelAggregate aggregate,
                                          final BigDecimal sender) {
        final Set<BigDecimal> playerIds = new HashSet<BigDecimal>();
        for (ChatChannelAggregate.AggregateParticipant aggregateParticipant : aggregate.getChatParticipants()) {
            playerIds.add(aggregateParticipant.getPlayerId());
        }

        if (sender != null && !aggregate.hasParticipant(sender)) {
            playerIds.add(sender);
        }

        final HostDocument hostDocument = new ChatChannelStatusHostDocument(
                aggregate, destinationFactory.players(playerIds));
        hostDocumentPublisher.publish(asList(hostDocument));
    }
}
