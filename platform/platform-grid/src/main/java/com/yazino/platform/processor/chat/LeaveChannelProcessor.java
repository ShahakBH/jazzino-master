package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelType;
import com.yazino.platform.model.chat.ChatParticipant;
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
public class LeaveChannelProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(LeaveChannelProcessor.class);

    private final ChatRepository chatRepository;

    LeaveChannelProcessor() {
        // CGLib constructor
        this.chatRepository = null;
    }

    @Autowired
    public LeaveChannelProcessor(final ChatRepository chatRepository) {
        notNull(chatRepository, "chatRepository may not be null");

        this.chatRepository = chatRepository;
    }

    @EventTemplate
    public GigaspaceChatRequest getTemplate() {
        return new GigaspaceChatRequest(ChatRequestType.LEAVE_CHANNEL);
    }

    @SpaceDataEvent
    @Transactional
    public void processRequest(final GigaspaceChatRequest request) {
        LOG.debug("starting processRequest GigaspaceChatRequest: {}", request);

        if (!ChatRequestType.LEAVE_CHANNEL.equals(request.getRequestType())) {
            LOG.warn("Invalid request type - not processing " + ReflectionToStringBuilder.reflectionToString(request));
            return;
        }
        if (!request.isValid()) {
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
            chatRepository.remove(new ChatParticipant(request.getPlayerId(), request.getChannelId()));
            if (ChatChannelType.personal.equals(channel.getChannelType())) {
                final ChatParticipant[] participants = chatRepository.readParticipantsForChannel(
                        request.getChannelId());
                if (participants == null || participants.length == 0) {
                    LOG.info("Removing chat channel, there are no participants left {}", channel);
                    chatRepository.remove(channel);
                    return;
                }
            }
            chatRepository.request(new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL,
                    request.getPlayerId(), request.getChannelId(), request.getLocationId()));

        } catch (Throwable t) {
            LOG.error("Exception removing participant", t);
        }

        chatRepository.save(channel);

    }
}
