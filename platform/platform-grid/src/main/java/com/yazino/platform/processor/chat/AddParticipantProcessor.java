package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.ChatChannel;
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

import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 5, maxConcurrentConsumers = 10)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class AddParticipantProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AddParticipantProcessor.class);

    private final ChatRepository chatRepository;
    private final ChatChannelWorker chatChannelWorker;

    AddParticipantProcessor() {
        // CGLib constructor
        this.chatRepository = null;
        this.chatChannelWorker = null;
    }

    @Autowired
    public AddParticipantProcessor(final ChatRepository chatRepository,
                                   final ChatChannelWorker chatChannelWorker) {
        notNull(chatRepository, "chatRepository may not be null");
        notNull(chatChannelWorker, "chatChannelWorker may not be null");

        this.chatRepository = chatRepository;
        this.chatChannelWorker = chatChannelWorker;
    }

    @EventTemplate
    public GigaspaceChatRequest getTemplate() {
        return new GigaspaceChatRequest(ChatRequestType.ADD_PARTICIPANT);
    }

    @SpaceDataEvent
    @Transactional
    public void processRequest(final GigaspaceChatRequest request) {
        LOG.debug("starting processRequest GigaspaceChatRequest: {}", request);

        if (!request.isValid()) {
            LOG.warn("Invalid request - not processing " + ReflectionToStringBuilder.reflectionToString(request));
            return;
        }
        final String channelId = request.getChannelId();
        ChatChannel channel = chatRepository.dirtyRead(new ChatChannel(channelId));
        if (channel == null) {
            LOG.error("Channel does not exist: " + channelId);
            return;
        }
        channel = chatRepository.lock(channelId);
        try {
            final BigDecimal playerId = request.getPlayerId();
            final String locationId = request.getLocationId();
            if (!chatChannelWorker.canAddParticipantsToChannel(chatRepository, channelId, playerId, locationId)) {
                LOG.warn("Sender does not have the right to add participants - not processing {}", request);
                return;
            }
            final Map<ChatRequestArgument, String> requestArgs = request.getArgs();
            chatRepository.save(new ChatParticipant(
                    new BigDecimal(requestArgs.get(ChatRequestArgument.PLAYER_ID)),
                    channelId, requestArgs.get(ChatRequestArgument.NICKNAME)));
            final GigaspaceChatRequest dispatchRequest = new GigaspaceChatRequest(
                    ChatRequestType.PUBLISH_CHANNEL, playerId, channelId, locationId);
            chatRepository.request(dispatchRequest);

        } catch (Throwable t) {
            LOG.error("Exception adding participant", t);

        } finally {
            chatRepository.save(channel);
        }
    }
}
