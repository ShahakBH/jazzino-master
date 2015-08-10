package com.yazino.platform.processor.chat;

import com.yazino.platform.chat.ChatRequestArgument;
import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.chat.*;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.util.ArrayHelper;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class ChatMessageWorker {
    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageWorker.class);

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final PlayerRepository playerRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final ChatRepository chatRepository;
    private final ProfanityFilter profanityFilter;
    private final DocumentDispatcher documentDispatcher;
    private final ChatChannelWorker chatChannelWorker;

    @Autowired
    public ChatMessageWorker(final PlayerRepository playerRepository,
                             final PlayerSessionRepository playerSessionRepository,
                             final ChatRepository chatRepository,
                             final ProfanityFilter profanityFilter,
                             @Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher,
                             final ChatChannelWorker chatChannelWorker) {
        notNull(playerRepository, "playerRepository may not be null");
        notNull(playerSessionRepository, "playerSessionRepository may not be null");
        notNull(chatRepository, "chatRepository may not be null");
        notNull(profanityFilter, "profanityFilter may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");
        notNull(chatChannelWorker, "chatChannelWorker may not be null");

        this.playerRepository = playerRepository;
        this.playerSessionRepository = playerSessionRepository;
        this.chatRepository = chatRepository;
        this.profanityFilter = profanityFilter;
        this.documentDispatcher = documentDispatcher;
        this.chatChannelWorker = chatChannelWorker;
    }

    private Document buildChatDocument(final ChatMessage chatMessage) {
        return new Document(DocumentType.CHAT_MESSAGE.getName(),
                JSON_HELPER.serialize(chatMessage), buildHeaders(chatMessage));
    }

    private Map<String, String> buildHeaders(final ChatMessage chatMessage) {
        final Map<String, String> ret = new HashMap<String, String>();
        ret.put(ChatDocumentHeader.channelId.name(), chatMessage.getChannelId());
        if (chatMessage.getLocationId() != null) {
            ret.put(ChatDocumentHeader.locationId.name(), chatMessage.getLocationId());
        }
        return ret;
    }

    private void dispatchToAllParticipants(final ChatMessage message,
                                           final ChatParticipant[] participants,
                                           final BigDecimal fromId) {
        final Document document = buildChatDocument(message);
        if (fromId == null) {
            dispatch(documentDispatcher, document, participants);

        } else {
            final BigDecimal[] toPlayerIds = ArrayHelper.convert(participants, new BigDecimal[participants.length],
                    ChatParticipantWorker.CONVERT_TO_PLAYER_ID);
            for (BigDecimal toPlayerId : toPlayerIds) {
                sendDocumentToPlayer(fromId, document, toPlayerId);
            }
        }
    }

    private void dispatch(final DocumentDispatcher dispatcher,
                          final Document document,
                          final ChatParticipant[] players) {
        final BigDecimal[] playerIds = ArrayHelper.convert(players, new BigDecimal[players.length],
                ChatParticipantWorker.CONVERT_TO_PLAYER_ID);
        final Set<BigDecimal> playerIdsSet = new HashSet<BigDecimal>(Arrays.asList(playerIds));
        dispatcher.dispatch(document, playerIdsSet);
    }

    private void sendDocumentToPlayer(final BigDecimal fromId,
                                      final Document document,
                                      final BigDecimal toPlayerId) {
        try {
            if (!playerSessionRepository.isOnline(toPlayerId)) {
                return;
            }

            if (fromId.compareTo(toPlayerId) != 0) {
                final Relationship relationshipToSender = player(toPlayerId).getRelationshipTo(fromId);
                if (relationshipToSender != null) {
                    if (RelationshipType.IGNORED.equals(relationshipToSender.getType())
                            || RelationshipType.IGNORED_FRIEND.equals(relationshipToSender.getType())) {
                        return;
                    }
                }
            }

            documentDispatcher.dispatch(document, toPlayerId);

        } catch (Exception e) {
            LOG.error("Unable to send document to player {} from {}", toPlayerId, fromId, e);
        }
    }

    private Player player(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Couldn't load player " + playerId);
        }
        return player;
    }

    public void sendChatMessage(final GigaspaceChatRequest request) {
        LOG.debug("starting sendChatMessage GigaspaceChatRequest: ", request);
        if (!request.isValid()) {
            LOG.warn("Invalid request - not processing {}", request);
            return;
        }
        if (!chatChannelWorker.canSendMessagesToChannel(chatRepository, request.getChannelId(), request.getPlayerId())) {
            LOG.warn("Sender does not have the right to send messages - not processing {}", request);
            return;
        }
        final ChatChannel channel = chatRepository.dirtyRead(new ChatChannel(request.getChannelId()));
        if (channel == null) {
            return;
        }
        final ChatParticipant template = new ChatParticipant(request.getPlayerId(), request.getChannelId());
        final ChatParticipant chatParticipant = chatRepository.read(template);
        String messageText = request.getArg(ChatRequestArgument.MESSAGE);
        if (messageText == null) {
            messageText = "";
        } else {
            messageText = filterMessage(messageText);
        }
        if (profanityFilter != null) {
            messageText = profanityFilter.filter(messageText);
        }
        final ChatMessage message = new ChatMessage(request.getPlayerId(), chatParticipant.getNickname(),
                messageText, channel.getChannelId(), channel.getLocationId());
        final ChatParticipant[] participants = chatRepository.readParticipantsForChannel(request.getChannelId());
        dispatchToAllParticipants(message, participants, request.getPlayerId());
    }

    String filterMessage(final String messageText) {
        return StringEscapeUtils.escapeHtml4(messageText);
    }
}
