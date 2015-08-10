package com.yazino.platform.processor.chat;

import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.repository.chat.ChatRepository;

public class ChatProcessorContext {
    private final ChatRepository chatRepository;

    public ChatProcessorContext(final ChatRepository chatRepository,
                                final DocumentDispatcher documentDispatcher,
                                final ChatMessageWorker chatMessageWorker,
                                final ChatChannelAggregateWorker aggregateWorker,
                                final ChatChannelWorker chatChannelWorker) {
        this.chatRepository = chatRepository;
    }

}
