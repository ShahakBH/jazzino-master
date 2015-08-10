package com.yazino.platform.service.community;

import com.gigaspaces.async.AsyncResult;
import com.yazino.platform.community.GiftService;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.gifting.*;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import org.joda.time.DateTime;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.remoting.RemotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.gifting.CollectionResult.*;
import static com.yazino.platform.gifting.Giftable.GIFTABLE;
import static com.yazino.platform.gifting.Giftable.GIFTED;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingGiftService implements GiftService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingGiftService.class);

    private final GiftRepository giftRepository;
    private final GiftGamblingService giftGamblingService;
    private final GiftProperties giftProperties;
    private final PlayerRepository playerRepository;
    private final SequenceGenerator sequenceGenerator;
    private final Executor executor;

    @Autowired
    public GigaspaceRemotingGiftService(final GiftRepository giftRepository,
                                        final PlayerRepository playerRepository,
                                        final GiftGamblingService giftGamblingService,
                                        final GiftProperties giftProperties,
                                        final SequenceGenerator sequenceGenerator,
                                        final Executor executor) {
        notNull(giftRepository, "giftRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(giftGamblingService, "giftGamblingService may not be null");
        notNull(giftProperties, "giftingProperties may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(executor, "executor may not be null");

        this.giftRepository = giftRepository;
        this.playerRepository = playerRepository;
        this.giftGamblingService = giftGamblingService;
        this.giftProperties = giftProperties;
        this.sequenceGenerator = sequenceGenerator;
        this.executor = executor;
    }

    public Set<BigDecimal> giveGiftsToAllFriends(@org.openspaces.remoting.Routing final BigDecimal sendingPlayer,
                                                 final BigDecimal sessionId) {
        notNull(sendingPlayer, "sendingPlayer may not be null");
        notNull(sessionId, "sessionId may not be null");

        final Player sender = playerRepository.findById(sendingPlayer);
        if (sender == null) {
            LOG.debug("Player {} does not exist", sendingPlayer);
            return Collections.emptySet();
        }

        final Map<BigDecimal, Relationship> friends = sender.listRelationships(RelationshipType.FRIEND);
        if (friends == null || friends.isEmpty()) {
            LOG.debug("Player {} has no friends", sendingPlayer);
            return Collections.emptySet();
        }

        return giveGifts(sendingPlayer, friends.keySet(), sessionId);
    }

    @Override
    public Set<BigDecimal> giveGifts(@org.openspaces.remoting.Routing final BigDecimal sendingPlayer,
                                     final Set<BigDecimal> recipientPlayers,
                                     final BigDecimal sessionId) {
        notNull(sendingPlayer, "sendingPlayer may not be null");
        notNull(recipientPlayers, "recipientPlayers may not be null");
        notNull(sessionId, "sessionId may not be null");

        LOG.debug("{} attempting to send gifts to {}", sendingPlayer, recipientPlayers);

        final Set<BigDecimal> recipientsToGift = recipientsAvailableToGift(sendingPlayer, recipientPlayers);
        if (recipientsToGift.isEmpty()) {
            LOG.debug("{} has no valid recipients to gift", sendingPlayer);
            return Collections.emptySet();
        }

        final Set<BigDecimal> giftIds = sequenceGenerator.next(recipientsToGift.size());

        final Iterator<BigDecimal> giftIdIterator = giftIds.iterator();
        final Map<BigDecimal, BigDecimal> recipientsToGiftIds = new HashMap<>(recipientsToGift.size());
        for (BigDecimal recipientPlayerId : recipientsToGift) {
            if (!giftIdIterator.hasNext()) {
                throw new IllegalStateException("Gift IDs exhausted unexpectedly");
            }
            recipientsToGiftIds.put(recipientPlayerId, giftIdIterator.next());
        }

        LOG.debug("Requesting gift from {} to players {}", sendingPlayer, recipientsToGiftIds);
        giftRepository.requestSendGifts(sendingPlayer, sessionId, recipientsToGiftIds);

        LOG.debug("{} sent gifts to valid recipients {}", sendingPlayer, giftIds);
        return giftIds;
    }

    @Override
    public Set<com.yazino.platform.gifting.Gift> getAvailableGifts(@org.openspaces.remoting.Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final Set<com.yazino.platform.gifting.Gift> gifts = new HashSet<>();
        for (Gift gift : giftRepository.findAvailableByRecipient(playerId)) {
            gifts.add(asView(gift));
        }

        LOG.debug("Player {} has gifts {}", playerId, gifts);

        return gifts;
    }

    private com.yazino.platform.gifting.Gift asView(final Gift gift) {
        return new com.yazino.platform.gifting.Gift(gift.getId(),
                gift.getSendingPlayer(),
                gift.getRecipientPlayer(),
                gift.getExpiry(),
                gift.getAcknowledged());
    }

    @Override
    public void acknowledgeViewedGifts(@org.openspaces.remoting.Routing final BigDecimal playerId,
                                       final Set<BigDecimal> giftIds) {
        notNull(playerId, "playerId may not be null");
        notNull(giftIds, "giftIds may not be null");

        LOG.debug("Requesting acknowledgement for recipient {} of gifts {}", playerId, giftIds);
        giftRepository.requestAcknowledgement(playerId, giftIds);
    }

    @Override
    public BigDecimal collectGift(@org.openspaces.remoting.Routing final BigDecimal playerId,
                                  final BigDecimal giftId,
                                  final CollectChoice choice,
                                  final BigDecimal sessionId) throws GiftCollectionFailure {
        LOG.debug("Player {} collecting gift {} with choice {}", playerId, giftId, choice);

        final DateTime now = new DateTime();
        final int remainingGiftCollectionsForPlayer = giftProperties.remainingGiftCollections(giftRepository.countCollectedOn(playerId, now));
        if (remainingGiftCollectionsForPlayer <= 0) {
            LOG.debug("Player {} has no gift collections left for day beginning {}", playerId, now);
            throw new GiftCollectionFailure(MAX_COLLECTION_LIMIT_REACHED);
        }

        final Gift gift = giftRepository.findByRecipientAndId(playerId, giftId);
        if (gift == null) {
            LOG.debug("Player {} is attempting to collect an invalid gift: {}", playerId, giftId);
            throw new GiftCollectionFailure(GIFT_NOT_FOUND);
        }

        final CollectionResult collectionResult = collectionResultFor(now, gift);
        if (collectionResult == COLLECTED) {
            final BigDecimal giftWinnings = defaultIfNull(giftGamblingService.collectGift(choice), BigDecimal.ZERO);

            LOG.debug("Player {} has request collections of gift {} with winnings {}", playerId, giftId, giftWinnings);
            giftRepository.requestCollection(playerId, giftId, sessionId, giftWinnings, choice);

            return giftWinnings;

        } else {
            LOG.debug("Player {} collection of gift {} failed with result {}", playerId, giftId, collectionResult);
            throw new GiftCollectionFailure(collectionResult);
        }
    }

    private CollectionResult collectionResultFor(final DateTime now, final Gift gift) {
        final CollectionResult collectionResult;
        if (gift.getCollected() != null) {
            collectionResult = ALREADY_COLLECTED;
        } else if (gift.getExpiry().isBefore(now)) {
            collectionResult = GIFT_EXPIRED;
        } else {
            collectionResult = COLLECTED;
        }
        return collectionResult;
    }

    @Override
    public DateTime getEndOfGiftPeriod() {
        return giftProperties.endOfGiftPeriod();
    }

    @Override
    public PlayerCollectionStatus pushPlayerCollectionStatus(@org.openspaces.remoting.Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        LOG.debug("Pushing collection status for player {}", playerId);

        final int collectedToday = giftRepository.countCollectedOn(playerId, giftProperties.startOfGiftPeriod());
        final PlayerCollectionStatus playerCollectionStatus = new PlayerCollectionStatus(
                giftProperties.remainingGiftCollections(collectedToday),
                giftRepository.countAvailableForCollection(playerId));
        giftRepository.publishCollectionStatus(playerId, playerCollectionStatus);
        return playerCollectionStatus;
    }

    @Override
    public Set<GiftableStatus> getGiftableStatusForPlayers(final BigDecimal sendingPlayerId,
                                                           final Set<BigDecimal> friendIds) {
        notNull(sendingPlayerId, "sendingPlayerId may not be null");
        notNull(friendIds, "friendIds may not be null");

        LOG.debug("Getting giftable status for player {} with friends {}", sendingPlayerId, friendIds);

        return executor.mapReduce(new FindStatusesBySender(sendingPlayerId, friendIds, regiftablePeriod()));
    }

    private Set<BigDecimal> recipientsAvailableToGift(final BigDecimal sendingPlayerId,
                                                      final Set<BigDecimal> friendIds) {
        LOG.debug("Getting giftable players for player {} from candidates {}", sendingPlayerId, friendIds);

        return executor.mapReduce(new FindAvailableForGiftingBySender(sendingPlayerId, friendIds, regiftablePeriod()));
    }

    private DateTime regiftablePeriod() {
        return new DateTime().minusSeconds(giftProperties.regiftCooldownPeriodInSeconds());
    }

    @AutowireTask
    public static class FindStatusesBySender implements DistributedTask<HashSet<GiftableStatus>, Set<GiftableStatus>> {
        private static final long serialVersionUID = -5845138275375226224L;

        @Resource(name = "routing")
        private transient Routing routing;
        @Resource(name = "giftRepository")
        private transient GiftRepository giftRepository;

        private final BigDecimal senderPlayerId;
        private final Set<BigDecimal> friendIds;
        private final DateTime createdSince;

        public FindStatusesBySender(final BigDecimal senderPlayerId,
                                    final Set<BigDecimal> friendIds,
                                    final DateTime createdSince) {
            notNull(senderPlayerId, "senderPlayerId may not be null");
            notNull(friendIds, "friendIds may not be null");
            notNull(createdSince, "createdSince may not be null");

            this.senderPlayerId = senderPlayerId;
            this.friendIds = new HashSet<>(friendIds);
            this.createdSince = createdSince;
        }

        @Override
        public HashSet<GiftableStatus> execute() throws Exception {
            final Set<BigDecimal> giftRecipients = giftRepository.findLocalRecipientsBySender(senderPlayerId, createdSince);

            final HashSet<GiftableStatus> result = new HashSet<>();
            for (BigDecimal friendId : locallyRoutedFrom(friendIds)) {
                result.add(new GiftableStatus(friendId, statusFor(giftRecipients, friendId), null, null));
            }

            return result;
        }

        private Set<BigDecimal> locallyRoutedFrom(final Set<BigDecimal> ids) {
            final Set<BigDecimal> locallyRoutedIds = new HashSet<>();
            for (BigDecimal id : ids) {
                if (routing.isRoutedToCurrentPartition(id)) {
                    locallyRoutedIds.add(id);
                }
            }
            return locallyRoutedIds;
        }

        private Giftable statusFor(final Set<BigDecimal> giftRecipients, final BigDecimal playerId) {
            if (giftRecipients.contains(playerId)) {
                return GIFTED;
            }
            return GIFTABLE;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public Set<GiftableStatus> reduce(final List<AsyncResult<HashSet<GiftableStatus>>> asyncResults) throws Exception {
            final Set<GiftableStatus> result = new HashSet<>();
            for (AsyncResult<HashSet<GiftableStatus>> asyncResult : asyncResults) {
                if (asyncResult.getException() != null) {
                    LOG.error("Find statuses failed", asyncResult.getException());
                } else {
                    result.addAll(asyncResult.getResult());
                }
            }
            return result;
        }
    }

    @AutowireTask
    public static class FindAvailableForGiftingBySender implements DistributedTask<HashSet<BigDecimal>, Set<BigDecimal>> {
        private static final long serialVersionUID = -5845138275375226225L;

        @Resource(name = "giftRepository")
        private transient GiftRepository giftRepository;

        private final BigDecimal senderPlayerId;
        private final Set<BigDecimal> friendIds;
        private final DateTime createdSince;

        public FindAvailableForGiftingBySender(final BigDecimal senderPlayerId,
                                               final Set<BigDecimal> friendIds,
                                               final DateTime createdSince) {
            notNull(senderPlayerId, "senderPlayerId may not be null");
            notNull(friendIds, "friendIds may not be null");
            notNull(createdSince, "createdSince may not be null");

            this.senderPlayerId = senderPlayerId;
            this.friendIds = new HashSet<>(friendIds);
            this.createdSince = createdSince;
        }

        @Override
        public HashSet<BigDecimal> execute() throws Exception {
            final Set<BigDecimal> giftRecipients = giftRepository.findLocalRecipientsBySender(senderPlayerId, createdSince);

            final HashSet<BigDecimal> playersThatCanBeGifted = new HashSet<>();
            for (BigDecimal friendId : friendIds) {
                if (!giftRecipients.contains(friendId)) {
                    playersThatCanBeGifted.add(friendId);
                }
            }
            return playersThatCanBeGifted;
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public Set<BigDecimal> reduce(final List<AsyncResult<HashSet<BigDecimal>>> asyncResults) throws Exception {
            final Set<BigDecimal> result = new HashSet<>();
            for (AsyncResult<HashSet<BigDecimal>> asyncResult : asyncResults) {
                if (asyncResult.getException() != null) {
                    LOG.error("Find available for gifting failed", asyncResult.getException());
                } else {
                    result.addAll(asyncResult.getResult());
                }
            }
            return result;
        }
    }
}
