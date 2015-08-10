package com.yazino.host.chat;

import com.yazino.platform.model.chat.*;
import com.yazino.platform.repository.chat.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
public class StandaloneChatRepository implements ChatRepository, ChatRequestSource {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneChatRepository.class);

    private final BlockingQueue<GigaspaceChatRequest> queue = new LinkedBlockingQueue<GigaspaceChatRequest>();

    private Map<String, List<ChatParticipant>> chatParticipants = new HashMap<String, List<ChatParticipant>>();
    private Map<BigDecimal, List<ChatParticipant>> chatParticipantsByPlayerId =
            new HashMap<BigDecimal, List<ChatParticipant>>();
    private Map<String, ChatChannel> chatChannels = new HashMap<String, ChatChannel>();
    private Map<String, ChatChannel> chatChannelsByLocation = new HashMap<String, ChatChannel>();

    private final ReentrantReadWriteLock channelLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock participantLock = new ReentrantReadWriteLock();

    @Override
    public void save(final ChatParticipant chatParticipant) {
        channelLock.writeLock().lock();
        try {
            final String channelId = chatParticipant.getChannelId();
            if (!chatParticipants.containsKey(channelId)) {
                chatParticipants.put(channelId, new ArrayList<ChatParticipant>());
            }
            chatParticipants.get(channelId).add(chatParticipant);

            final BigDecimal playerId = chatParticipant.getPlayerId();
            if (!chatParticipantsByPlayerId.containsKey(playerId)) {
                chatParticipantsByPlayerId.put(playerId, new ArrayList<ChatParticipant>());
            }
            chatParticipantsByPlayerId.get(playerId).add(chatParticipant);
        } finally {
            channelLock.writeLock().unlock();
        }
    }

    @Override
    public void save(final ChatChannel chatChannel) {
        participantLock.writeLock().lock();
        try {
            if (chatChannel.getLocationId() != null) {
                chatChannelsByLocation.put(chatChannel.getLocationId(), chatChannel);
            }
            if (chatChannel.getChannelId() == null) {
                chatChannel.setChannelId(UUID.randomUUID().toString());
            }
            chatChannels.put(chatChannel.getChannelId(), chatChannel);
        } finally {
            participantLock.writeLock().unlock();
        }
    }

    @Override
    public ChatChannelAggregate readAggregate(final String channelId) {
        channelLock.readLock().lock();
        participantLock.readLock().lock();
        try {
            final ChatChannel chatChannel = chatChannels.get(channelId);
            if (chatChannel == null) {
                return null;
            }
            return new ChatChannelAggregate(chatChannel, readParticipantsForChannel(channelId));
        } finally {
            channelLock.readLock().unlock();
            participantLock.readLock().unlock();
        }
    }

    @Override
    public ChatParticipant[] readParticipantsForChannel(final String channelId) {
        participantLock.readLock().lock();
        try {
            final List<ChatParticipant> participants = chatParticipants.get(channelId);
            if (participants == null) {
                return new ChatParticipant[0];
            }
            return participants.toArray(new ChatParticipant[participants.size()]);
        } finally {
            participantLock.readLock().unlock();
        }
    }

    @Override
    public ChatParticipant[] readParticipantsForSession(final BigDecimal playerId) {
        participantLock.readLock().lock();
        try {
            final List<ChatParticipant> participants = chatParticipantsByPlayerId.get(playerId);
            if (participants == null) {
                return new ChatParticipant[0];
            }
            return participants.toArray(new ChatParticipant[participants.size()]);
        } finally {
            participantLock.readLock().unlock();
        }
    }

    @Override
    public void remove(final ChatParticipant chatParticipant) {
        participantLock.writeLock().lock();
        try {
            chatParticipants.get(chatParticipant.getChannelId()).remove(chatParticipant);
            chatParticipantsByPlayerId.get(chatParticipant.getPlayerId()).remove(chatParticipant);
        } finally {
            participantLock.writeLock().unlock();
        }
    }

    @Override
    public void remove(final ChatChannel chatChannel) {
        channelLock.writeLock().lock();
        try {
            chatChannels.remove(chatChannel.getChannelId());
            chatChannelsByLocation.remove(chatChannel.getLocationId());
        } finally {
            channelLock.writeLock().unlock();
        }
    }

    @Override
    public void request(final GigaspaceChatRequest chatRequest) {
        if (!chatRequest.isValid()) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("Invalid request - ignoring " + chatRequest);
            }
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Enqueueing request: " + chatRequest);
        }
        queue.add(chatRequest);
    }

    @Override
    public ChatParticipant read(final ChatParticipant chatParticipant) {
        participantLock.readLock().lock();
        try {
            final List<ChatParticipant> participants = chatParticipants.get(chatParticipant.getChannelId());
            if (participants != null) {
                for (ChatParticipant potentialMatch : participants) {
                    if (potentialMatch.getPlayerId().equals(chatParticipant.getPlayerId())) {
                        return potentialMatch;
                    }
                }
            }
            return null;
        } finally {
            participantLock.readLock().unlock();
        }
    }

    @Override
    public ChatChannel dirtyRead(final ChatChannel chatChannel) {
        channelLock.readLock().lock();
        try {
            final ChatChannel channel = chatChannels.get(chatChannel.getChannelId());
            if (channel == null) {
                return null;
            }
            return channel;
        } finally {
            channelLock.readLock().unlock();
        }
    }

    @Override
    public ChatChannel getOrCreateForLocation(final String locationId) {
        channelLock.readLock().lock();
        try {
            if (!chatChannelsByLocation.containsKey(locationId)) {
                createChannel(locationId);
            }
            return chatChannelsByLocation.get(locationId);
        } finally {
            channelLock.readLock().unlock();
        }
    }

    private void createChannel(final String locationId) {
        channelLock.readLock().unlock();
        try {
            save(new ChatChannel(ChatChannelType.table, locationId));
        } finally {
            channelLock.readLock().lock();
        }
    }

    @Override
    public ChatChannel lock(final String channelId) {
        return chatChannels.get(channelId);
    }

    @Override
    public GigaspaceChatRequest getNextRequest() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            LOG.error("Error retrieving chatRequest", e);
            throw new RuntimeException(e);
        }
    }
}
