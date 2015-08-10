package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.model.chat.GigaspaceChatRequest;
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
public class ChatMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageProcessor.class);

    private final ChatMessageWorker chatMessageWorker;

    ChatMessageProcessor() {
        // CGLib constructor

        this.chatMessageWorker = null;
    }

    @Autowired
    public ChatMessageProcessor(final ChatMessageWorker chatMessageWorker) {
        notNull(chatMessageWorker, "chatMessageWorker may not be null");

        this.chatMessageWorker = chatMessageWorker;
    }

    @EventTemplate
    public GigaspaceChatRequest getTemplate() {
        return new GigaspaceChatRequest(ChatRequestType.SEND_MESSAGE);
    }

    @SpaceDataEvent
    @Transactional
    public void processRequest(final GigaspaceChatRequest request) {
        LOG.debug("processRequest {}", request);

        chatMessageWorker.sendChatMessage(request);
    }
}
