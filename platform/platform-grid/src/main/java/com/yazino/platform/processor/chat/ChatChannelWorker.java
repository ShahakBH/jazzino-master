package com.yazino.platform.processor.chat;

import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.repository.chat.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ChatChannelWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ChatChannelWorker.class);

    public boolean canSendMessagesToChannel(final ChatRepository chatRepository,
                                            final String channelId,
                                            final BigDecimal playerId) {
        final ChatParticipant participation = chatRepository.read(new ChatParticipant(playerId, channelId));
        return (participation != null);
    }

    public boolean canAddParticipantsToChannel(final ChatRepository chatRepository,
                                               final String channelId,
                                               final BigDecimal playerId,
                                               final String locationId) {
        final ChatChannel chatChannel = chatRepository.dirtyRead(new ChatChannel(channelId));
        if (chatChannel == null) {
            LOG.warn("no chat channel found for id " + channelId);
            return false;
        }
        if (locationId != null && locationId.equals(chatChannel.getLocationId())) {
            return true;
        }
        if (!canSendMessagesToChannel(chatRepository, channelId, playerId)) {
            return false;
        }
        if (chatChannel.getChannelType().canAddParticipants()) {
            return true;
        }
        return false;
    }
}
