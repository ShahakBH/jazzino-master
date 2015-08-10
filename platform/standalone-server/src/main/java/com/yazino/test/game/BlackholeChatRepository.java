package com.yazino.test.game;

import com.yazino.platform.model.chat.GigaspaceChatRequest;
import org.springframework.stereotype.Repository;
import com.yazino.platform.model.chat.ChatChannel;
import com.yazino.platform.model.chat.ChatChannelAggregate;
import com.yazino.platform.model.chat.ChatParticipant;
import com.yazino.platform.repository.chat.ChatRepository;

import java.math.BigDecimal;

@Repository("chatRepository")
public class BlackholeChatRepository implements ChatRepository {

    @Override
    public void save(final ChatParticipant chatParticipant) {
    }

    @Override
    public void save(final ChatChannel chatChannel) {
    }

    @Override
    public ChatChannelAggregate readAggregate(final String s) {
        return null;
    }

    @Override
    public ChatParticipant[] readParticipantsForChannel(final String s) {
        return new ChatParticipant[0];
    }

    @Override
    public ChatParticipant[] readParticipantsForSession(final BigDecimal bigDecimal) {
        return new ChatParticipant[0];
    }

    @Override
    public void remove(final ChatParticipant chatParticipant) {
    }

    @Override
    public void remove(final ChatChannel chatChannel) {
    }

    @Override
    public void request(final GigaspaceChatRequest gigaspaceChatRequest) {
    }

    @Override
    public ChatParticipant read(final ChatParticipant chatParticipant) {
        return null;
    }

    @Override
    public ChatChannel dirtyRead(final ChatChannel chatChannel) {
        return null;
    }

    @Override
    public ChatChannel getOrCreateForLocation(final String s) {
        return new ChatChannel(s);
    }

    @Override
    public ChatChannel lock(final String s) {
        return null;
    }
}
