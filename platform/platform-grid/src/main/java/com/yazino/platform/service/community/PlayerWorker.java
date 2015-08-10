package com.yazino.platform.service.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.community.*;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.processor.community.RelationshipActionProcessor;
import com.yazino.platform.processor.community.RelationshipActionProcessorFactory;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerWorker {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerWorker.class);

    @SuppressWarnings("unchecked")
    private static final Map<String, String> EMPTY_HEADERS = Collections.unmodifiableMap(new HashMap<String, String>());
    private JsonHelper jsonHelper = new JsonHelper();

    public RelationshipType getCurrentRelationshipType(final Player p,
                                                       final BigDecimal relatedPlayerId) {
        final Relationship current = p.getRelationshipTo(relatedPlayerId);
        if (current != null) {
            return current.getType();
        }
        return RelationshipType.NO_RELATIONSHIP;
    }

    public RelationshipType calculateNewRelationshipType(
            final RelationshipType currentRelationship,
            final RelationshipAction requestedAction,
            final boolean processingInverseSide) {
        final RelationshipActionProcessor ap = RelationshipActionProcessorFactory.create(requestedAction);
        return ap.process(currentRelationship, processingInverseSide);
    }

    public PlayerSessionDocumentProperties getLocationDocumentProperties(
            final BigDecimal playerId,
            final PlayerSessionsSummary sessionsSummary,
            final TableInviteRepository tableInviteRepository) {
        if (sessionsSummary == null) {
            return PlayerSessionDocumentProperties.OFFLINE;
        }
        return new PlayerSessionDocumentProperties(playerId, sessionsSummary, tableInviteRepository);
    }

    private RelationshipDocumentProperties getRelationshipDocumentProperties(
            final BigDecimal playerId,
            final BigDecimal relatedPlayerId,
            final Relationship relationship,
            final String friendPictureUrl,
            final PlayerSessionsSummary sessionsSummary,
            final TableInviteRepository tableInviteRepository) {
        LOG.debug("getRelationshipDocumentProperties {} {}", relatedPlayerId, relationship);
        return new RelationshipDocumentProperties(relationship.getNickname(),
                friendPictureUrl, relationship.getType(),
                getLocationDocumentProperties(playerId, sessionsSummary, tableInviteRepository), sessionsSummary != null
        );
    }

    private Document buildRelationshipDocument(final HashMap<BigDecimal, RelationshipDocumentProperties> body) {
        return new Document(DocumentType.COMMUNITY_PLAYER_RELATIONSHIP.getName(),
                jsonHelper.serialize(body), EMPTY_HEADERS);
    }

    public Document buildLocationDocument(final BigDecimal playerId,
                                          final PlayerSessionDocumentProperties playerSessionDocumentProperties) {
        final HashMap<BigDecimal, PlayerSessionDocumentProperties> body = new HashMap<>();
        body.put(playerId, playerSessionDocumentProperties);
        return new Document(DocumentType.COMMUNITY_STATUS.getName(), jsonHelper.serialize(body), EMPTY_HEADERS);
    }

    public Document buildOwnLocationDocument(final LocationChangeNotification changeNotification) {
        notNull(changeNotification, "changeNotification is null");
        notNull(changeNotification.getLocation(), "location is null");
        notNull(changeNotification.getNotificationType(), "notification type is null");
        final HashMap<String, String> body = new HashMap<>();
        body.put("location", changeNotification.getLocation().getLocationId());
        body.put("removed", String.valueOf(LocationChangeType.REMOVE.equals(
                changeNotification.getNotificationType())));
        return new Document(DocumentType.OWN_LOCATION_CHANGE.getName(),
                jsonHelper.serialize(body), EMPTY_HEADERS);
    }

    public Document buildRelationshipDocument(final Player player,
                                              final PlayerSessionRepository playerSessionGlobalRepository,
                                              final TableInviteRepository tableInviteRepository) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("buildRelationshipDocument " + player.getPlayerId());
        }
        final HashMap<BigDecimal, RelationshipDocumentProperties> body = new HashMap<>();
        final Map<BigDecimal, Relationship> relationships = player.getRelationships();
        if (relationships != null) {
            final Map<BigDecimal, PlayerSessionsSummary> playerIdsToSessionMap = playerSessionGlobalRepository.findOnlinePlayerSessions(
                    new HashSet<>(relationships.keySet()));
            LOG.debug("relationships [{}]={}", player.getPlayerId(), relationships);
            for (Entry<BigDecimal, Relationship> relatedPlayer : relationships.entrySet()) {
                final Relationship relationship = relatedPlayer.getValue();
                final BigDecimal relatedPlayerId = relatedPlayer.getKey();
                final PlayerSessionsSummary sessionsSummary = playerIdsToSessionMap.get(relatedPlayerId);
                if (relationship.getType().equals(RelationshipType.INVITATION_RECEIVED)
                        || (relationship.getType().equals(RelationshipType.FRIEND) && sessionsSummary != null)) {
                    final String pictureUrl;
                    if (sessionsSummary != null) {
                        pictureUrl = sessionsSummary.getPictureUrl();
                    } else {
                        pictureUrl = null;
                    }
                    body.put(relatedPlayerId, getRelationshipDocumentProperties(
                            player.getPlayerId(), relatedPlayerId, relationship,
                            pictureUrl, sessionsSummary, tableInviteRepository));
                }
            }
        }
        return buildRelationshipDocument(body);
    }


    public Document buildPlayerBalanceDocument(final Player p,
                                               final InternalWalletService walletService) {
        final HashMap<String, Object> body = new HashMap<>();
        try {
            body.put("balance", walletService.getBalance(p.getAccountId()).toString());
        } catch (WalletServiceException e) {
            throw new RuntimeException(e);
        }
        return new Document(DocumentType.PLAYER_BALANCE.getName(),
                jsonHelper.serialize(body), EMPTY_HEADERS);
    }

    public Document buildGiftReceivedDocument() {
        final HashMap<String, Object> body = new HashMap<>();
        return new Document(DocumentType.GIFT_RECEIVED.getName(), jsonHelper.serialize(body), EMPTY_HEADERS);
    }

    public Document buildGiftingPlayerCollectionStatusDocument(final Map<String, Object> arguments) {
        return new Document(DocumentType.GIFTING_PLAYER_COLLECTION_STATUS.getName(), jsonHelper.serialize(arguments), EMPTY_HEADERS);
    }
}
