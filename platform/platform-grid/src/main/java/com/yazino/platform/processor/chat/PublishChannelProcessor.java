package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
import com.yazino.platform.repository.chat.ChatRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 10)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class PublishChannelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PublishChannelProcessor.class);

    private final ChatRepository chatRepository;
    private final ChatChannelAggregateWorker chatChannelAggregateWorker;

    PublishChannelProcessor() {
        // CGLib constructor
        this.chatRepository = null;
        this.chatChannelAggregateWorker = null;
    }

    @Autowired
    public PublishChannelProcessor(final ChatRepository chatRepository,
                                   final ChatChannelAggregateWorker chatChannelAggregateWorker) {
        notNull(chatRepository, "chatRepository may not be null");
        notNull(chatChannelAggregateWorker, "chatChannelAggregateWorker may not be null");

        this.chatRepository = chatRepository;
        this.chatChannelAggregateWorker = chatChannelAggregateWorker;
    }

    @EventTemplate
    public GigaspaceChatRequest getTemplate() {
        return new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL);
    }

    @SpaceDataEvent
    @Transactional
    public void processRequest(final GigaspaceChatRequest request) {
        if (request == null || !request.isValid()) {
            LOG.warn("Invalid request - not processing " + ReflectionToStringBuilder.reflectionToString(request));
            return;
        }

        ChatChannel channel = chatRepository.dirtyRead(new ChatChannel(request.getChannelId()));
        if (channel == null) {
            LOG.error("Channel does not exist: " + request.getChannelId());
            return;
        }
        channel = chatRepository.lock(request.getChannelId());
        try {
            LOG.debug("starting processRequest GigaspaceChatRequest: {}", request);
            final ChatChannelAggregate aggregate = chatRepository.readAggregate(request.getChannelId());
            if (aggregate == null) {
                LOG.warn(String.format("no channel found for channel id [%s], discarding request [%s]",
                        request.getChannelId(), ReflectionToStringBuilder.reflectionToString(request)));
                return;
            }
            chatChannelAggregateWorker.dispatchToAllParticipants(aggregate, request.getPlayerId());
            if (ChatChannelType.personal.equals(aggregate.getType()) && aggregate.getChatParticipants().length == 1) {
                final GigaspaceChatRequest leaverequest = new GigaspaceChatRequest(ChatRequestType.LEAVE_CHANNEL,
                        aggregate.getChatParticipants()[0].getPlayerId(), aggregate.getId(), aggregate.getLocationId());
                chatRepository.request(leaverequest);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("start closing personal channel with only one participant "
                            + ReflectionToStringBuilder.reflectionToString(leaverequest));
                }
            }

        } catch (Exception e) {
            LOG.error("Request processing failed: {}", request, e);

        } finally {
            chatRepository.save(channel);
        }
    }
}
