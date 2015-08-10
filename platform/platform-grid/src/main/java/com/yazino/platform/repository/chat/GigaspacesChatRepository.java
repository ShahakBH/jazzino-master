package com.yazino.platform.repository.chat;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.chat.*;
import net.jini.core.lease.Lease;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspacesChatRepository implements ChatRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspacesChatRepository.class);
    private static final int TWO_SECONDS = 2000;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final long timeout;

    @Autowired
    public GigaspacesChatRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                    @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                    final Routing routing) {
        this(localGigaSpace, globalGigaSpace, routing, TWO_SECONDS);
    }

    public GigaspacesChatRepository(final GigaSpace localGigaSpace,
                                    final GigaSpace globalGigaSpace,
                                    final Routing routing,
                                    final long timeout) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");

        this.timeout = timeout;
        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
    }

    @Override
    public void save(final ChatParticipant participant) {
        notNull(participant, "participant may not be null");

        LOG.debug("entering save {}", participant);
        spaceFor(participant.getChannelId()).write(participant, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void save(final ChatChannel chatChannel) {
        notNull(chatChannel, "chatChannel may not be null");

        LOG.debug("entering save {}", chatChannel);
        if (chatChannel.getChannelId() == null) {
            chatChannel.setChannelId(UUID.randomUUID().toString());
        }
        spaceFor(chatChannel.getChannelId()).write(chatChannel, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public ChatChannelAggregate readAggregate(final String channelId) {
        notNull(channelId, "channelId may not be null");

        LOG.debug("entering readAggregate {}", channelId);

        final ChatChannel channel = spaceFor(channelId).readIfExists(new ChatChannel(channelId));
        if (channel == null) {
            return null;
        }
        final ChatParticipant[] participants = readParticipantsForChannel(channelId);
        return new ChatChannelAggregate(channel, participants);
    }

    @Override
    public ChatParticipant[] readParticipantsForChannel(final String channelId) {
        notNull(channelId, "channelId may not be null");

        LOG.debug("entering readParticipantsForChannel {}", channelId);
        final ChatParticipant template = new ChatParticipant(channelId);
        return spaceFor(channelId).readMultiple(template, Integer.MAX_VALUE);
    }

    @Override
    public ChatParticipant[] readParticipantsForSession(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");
        LOG.debug("entering readParticipantsForChannel {}", playerId);
        return globalGigaSpace.readMultiple(new ChatParticipant(playerId), Integer.MAX_VALUE);
    }

    @Override
    public void remove(final ChatParticipant participant) {
        notNull(participant, "participant may not be null");
        LOG.debug("entering remove {}", participant);
        spaceFor(participant.getChannelId()).clear(participant);
    }

    @Override
    public void remove(final ChatChannel channel) {
        notNull(channel, "channel may not be null");
        LOG.debug("entering remove {}", channel);
        spaceFor(channel.getChannelId()).clear(channel);
    }

    @Override
    public void request(final GigaspaceChatRequest chatRequest) {
        notNull(chatRequest, "chatRequest may not be null");
        LOG.debug("entering request {}", chatRequest);
        if (!chatRequest.isValid()) {
            LOG.warn("Invalid request - ignoring " + ReflectionToStringBuilder.reflectionToString(chatRequest));
            return;
        }
        spaceFor(chatRequest.getChannelId()).write(chatRequest, Lease.FOREVER, timeout, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public ChatParticipant read(final ChatParticipant participant) {
        notNull(participant, "participant may not be null");
        LOG.debug("entering read {}", participant);
        return spaceFor(participant.getChannelId()).readIfExists(participant);
    }

    @Override
    public ChatChannel lock(final String channelId) {
        notBlank(channelId, "channelId may not be null or blank");
        LOG.debug("Entering lock for channel with ID: {}", channelId);

        if (!routing.isRoutedToCurrentPartition(channelId)) {
            throw new IllegalArgumentException("You cannot lock a channel on another partition: ID = " + channelId);
        }

        final ChatChannel channel = localGigaSpace.readById(ChatChannel.class, channelId, channelId,
                timeout, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (channel == null) {
            throw new ConcurrentModificationException("Cannot obtain lock for channel: " + channelId);
        }
        return channel;
    }

    @Override
    public ChatChannel dirtyRead(final ChatChannel channel) {
        notNull(channel, "channel may not be null");
        LOG.debug("entering read {}", channel);
        return spaceFor(channel.getChannelId()).readIfExists(channel, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public ChatChannel getOrCreateForLocation(final String locationId) {
        notNull(locationId, "locationId may not be null");
        LOG.debug("entering read {}", locationId);
        if (locationId == null || locationId.trim().length() == 0) {
            LOG.warn("Null or empty locationId, ignoring");
            return null;
        }
        final ChatChannel template = new ChatChannel(ChatChannelType.table, locationId);
        final ChatChannel channel = globalGigaSpace.readIfExists(template, 0, ReadModifiers.DIRTY_READ);
        if (channel != null) {
            return channel;
        }
        save(template);
        return template;
    }

    private GigaSpace spaceFor(final String channelId) {
        if (routing.isRoutedToCurrentPartition(channelId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
