package com.yazino.platform.repository.chat;

import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.model.chat.GigaspaceChatRequest;

import java.math.BigDecimal;

public interface ChatRepository {

    void save(ChatParticipant participant);

    void save(ChatChannel chatChannel);

    ChatChannelAggregate readAggregate(String channelId);

    ChatParticipant[] readParticipantsForChannel(String channelId);

    ChatParticipant[] readParticipantsForSession(BigDecimal playerId);

    void remove(ChatParticipant participant);

    void remove(ChatChannel channel);

    void request(GigaspaceChatRequest chatRequest);

    ChatParticipant read(ChatParticipant participant);

    ChatChannel dirtyRead(ChatChannel channel);

    ChatChannel getOrCreateForLocation(String locationId);

    ChatChannel lock(String channelId);
}
