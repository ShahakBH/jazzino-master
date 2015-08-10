package com.yazino.platform.service.chat;

import com.google.common.collect.Sets;
import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.chat.ChatRequestType;
import com.yazino.platform.chat.ChatService;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.model.chat.*;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import org.openspaces.remoting.RemotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingChatService implements ChatService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingChatService.class);

    private final ChatRepository chatRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public GigaspaceRemotingChatService(final ChatRepository chatRepository,
                                        final PlayerRepository playerRepository) {
        notNull(chatRepository, "chatRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");

        this.chatRepository = chatRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public void processCommand(final BigDecimal playerId,
                               final String... chatCommand) {
        notNull(playerId, "playerId may not be null");
        notNull(chatCommand, "incomingRequest may not be null");

        // As this is parsed here, we are NOT routed on the channelId at this point

        if (LOG.isDebugEnabled()) {
            LOG.debug("processCommand {} {}", playerId, Arrays.asList(chatCommand).toString());
        }

        final GigaspaceChatRequest request = GigaspaceChatRequest.parse(chatCommand);
        request.setPlayerId(playerId);

        switch (request.getRequestType()) {
            case ADD_PARTICIPANT:
                processAddParticipant(request);
                break;

            case LEAVE_ALL:
                processLeaveAll(request);
                break;

            case PUBLISH_CHANNELS:
                processPublishChannels(request);
                break;

            case LEAVE_CHANNEL:
            case PUBLISH_CHANNEL:
            case SEND_MESSAGE:
                processOtherRequest(request);
                break;

            default:
                LOG.error("Unknown chat command: {}", request);
                break;
        }
    }

    @Override
    public void asyncProcessCommand(final BigDecimal playerId, final String... chatCommand) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #processCommand will be invoked
    }

    private void processOtherRequest(final GigaspaceChatRequest request) {
        if (!request.isValid()) {
            LOG.info("Invalid other request received: {}", request);
            throw new IllegalArgumentException("Invalid request [invalid]");
        }
        chatRepository.request(request);
    }

    private void processPublishChannels(final GigaspaceChatRequest request) {
        if (request.getPlayerId() == null) {
            LOG.info("Invalid publish request received: {}", request);
            throw new IllegalArgumentException("Invalid request [publishChannels without playerId]");
        }
        publishMultiple(request, ChatRequestType.PUBLISH_CHANNEL);
    }

    private void processLeaveAll(final GigaspaceChatRequest request) {
        if (request.getPlayerId() == null) {
            LOG.info("Invalid leave all request received: {}", request);
            throw new IllegalArgumentException("Invalid request [leaveAll without playerId]");
        }
        publishMultiple(request, ChatRequestType.LEAVE_CHANNEL);
    }

    private void processAddParticipant(final GigaspaceChatRequest request) {
        LOG.debug("Adding participant");

        final Player player = playerRepository.findById(request.getPlayerId());
        GigaspaceChatRequest requestToProcess = request;

        final Relationship relationship = player.getRelationshipTo(requestToProcess.getAddedPlayerId());
        if (relationship == null) {
            LOG.debug("Invalid request [You have no relationship with the new participant] {}", requestToProcess);
            throw new RuntimeException("You have no relationship with the new participant");
        }
        if (!RelationshipType.FRIEND.equals(relationship.getType())) {
            LOG.info("Invalid request [New participant is not your friend]: {}", requestToProcess);
            throw new RuntimeException("New participant is not your friend");
        }

        requestToProcess.getArgs().put(ChatRequestArgument.NICKNAME, relationship.getNickname());
        if (requestToProcess.getChannelId() == null) {
            LOG.debug("No Channel id supplied, looking for existing chanel for same participants");

            final ChatChannelAggregate matchingChannel = findChannelWithParticipants(requestToProcess);
            if (matchingChannel == null) {
                LOG.debug("Creating a new Channel");
                final ChatChannel channel = createChannel(request, player);
                requestToProcess.setChannelId(channel.getChannelId());

            } else {
                LOG.debug("Creating a request to publish {}", matchingChannel);
                requestToProcess = new GigaspaceChatRequest(ChatRequestType.PUBLISH_CHANNEL,
                        request.getPlayerId(), matchingChannel.getId(), matchingChannel.getLocationId());
            }
        }

        LOG.debug("Sending chat request: {}", requestToProcess);
        chatRepository.request(requestToProcess);
    }

    private ChatChannel createChannel(final GigaspaceChatRequest request,
                                      final Player player) {
        final ChatChannel channel = new ChatChannel(ChatChannelType.personal);
        chatRepository.save(channel);
        final ChatParticipant playerP = new ChatParticipant(request.getPlayerId(),
                channel.getChannelId(), player.getName());
        chatRepository.save(playerP);
        return channel;
    }

    private ChatChannelAggregate findChannelWithParticipants(final GigaspaceChatRequest request) {
        final ChatParticipant[] participants = chatRepository.readParticipantsForSession(request.getPlayerId());
        LOG.debug("Player is participating in {} channels", participants.length);
        final Set<BigDecimal> participantsToMatch = Sets.newHashSet(request.getPlayerId(), request.getAddedPlayerId());

        LOG.debug("Looking for channel with participants: {}", participantsToMatch);

        final Set<String> checkedChannels = new HashSet<String>();
        ChatChannelAggregate matchingChannel = null;
        for (ChatParticipant participant : participants) {
            if (!checkedChannels.contains(participant.getChannelId())) {
                checkedChannels.add(participant.getChannelId());
                final ChatChannelAggregate channel = chatRepository.readAggregate(participant.getChannelId());
                LOG.debug("Checking  channel: {}", channel);
                if (ChatChannelType.personal.equals(channel.getType()) && channel.getChatParticipants().length == 2) {
                    final Set<BigDecimal> participantsInChannel = new HashSet<BigDecimal>();
                    for (ChatChannelAggregate.AggregateParticipant aggregateParticipant : channel.getChatParticipants()) {
                        participantsInChannel.add(aggregateParticipant.getPlayerId());
                    }
                    if (participantsInChannel.equals(participantsToMatch)) {
                        LOG.debug("Matching channel found: {}", channel);
                        matchingChannel = channel;
                    }
                }
            }
            if (matchingChannel != null) {
                break;
            }
        }
        return matchingChannel;
    }

    private void publishMultiple(final GigaspaceChatRequest request,
                                 final ChatRequestType chatRequestType) {
        final ChatParticipant[] participants = chatRepository.readParticipantsForSession(request.getPlayerId());
        for (ChatParticipant participant : participants) {
            final GigaspaceChatRequest req = new GigaspaceChatRequest(chatRequestType, participant.getPlayerId(),
                    participant.getChannelId(), null, request.getArgs());
            chatRepository.request(req);
        }
    }
}
