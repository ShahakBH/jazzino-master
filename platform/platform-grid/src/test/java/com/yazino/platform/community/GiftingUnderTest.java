package com.yazino.platform.community;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.GiftAuditingService;
import com.yazino.platform.event.message.GiftCollectedEvent;
import com.yazino.platform.event.message.GiftSentEvent;
import com.yazino.platform.gifting.*;
import com.yazino.platform.grid.Executor;
import com.yazino.platform.grid.ExecutorTestUtils;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.*;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.processor.community.AcknowledgeGiftProcessor;
import com.yazino.platform.processor.community.CollectGiftProcessor;
import com.yazino.platform.processor.community.SendGiftProcessor;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.community.GiftGamblingService;
import com.yazino.platform.service.community.GiftProperties;
import com.yazino.platform.service.community.GigaspaceRemotingGiftService;
import org.joda.time.DateTime;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class GiftingUnderTest {
    public static final BigDecimal SESSION_ID = BigDecimal.TEN;

    @Mock
    private GiftGamblingService giftGamblingService;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;
    @Mock
    private InternalWalletService internalWalletService;
    @Mock
    private Routing routing;

    private final List<String> playerNames = newArrayList("BOB", "JIM", "FREDDY");
    private final Map<BigDecimal, Set<BigDecimal>> friends = new HashMap<>();
    private final DummyGiftCollectedQueuePublishing<GiftCollectedEvent> giftCollectEventQueue;
    private final DummyGiftRepository giftRepository = new DummyGiftRepository();

    private Set<GiftableStatus> currentlyShownPage;
    private DummyGiftSentQueuePublishing<GiftSentEvent> giftSentEventQueue;

    private GiftService giftService;
    private SendGiftProcessor sendGiftProcessor;
    private CollectGiftProcessor collectGiftProcessor;
    private AcknowledgeGiftProcessor acknowledgeGiftProcessor;

    public GiftingUnderTest() {
        MockitoAnnotations.initMocks(this);

        when(sequenceGenerator.next())
                .thenReturn(BigDecimal.valueOf(100))
                .thenReturn(BigDecimal.valueOf(200))
                .thenThrow(new IllegalStateException("Unexpected sequence call"));
        when(sequenceGenerator.next(anyInt())).then(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Set<BigDecimal> ids = new HashSet<>();
                for (int i = 0; i < (Integer) invocation.getArguments()[0]; ++i) {
                    ids.add(BigDecimal.valueOf(100 + i));
                }
                return ids;
            }
        });
        when(routing.isRoutedToCurrentPartition(any(BigDecimal.class))).thenReturn(true);

        final Map<String, Object> injectedServices = newHashMap();
        injectedServices.put("playerRepository", playerRepository);
        injectedServices.put("giftRepository", giftRepository);
        injectedServices.put("routing", routing);
        final Executor executor = ExecutorTestUtils.mockExecutorWith(1, injectedServices);

        final YazinoConfiguration configuration = new YazinoConfiguration();
        giftSentEventQueue = new DummyGiftSentQueuePublishing<>();
        giftCollectEventQueue = new DummyGiftCollectedQueuePublishing<>();

        final GiftProperties giftProperties = new GiftProperties(configuration);
        final GiftAuditingService giftAuditingService = new GiftAuditingService(giftSentEventQueue, giftCollectEventQueue);
        giftService = new GigaspaceRemotingGiftService(
                giftRepository,
                playerRepository,
                giftGamblingService,
                giftProperties,
                sequenceGenerator,
                executor);
        sendGiftProcessor = new SendGiftProcessor(giftAuditingService, giftRepository, giftProperties);
        acknowledgeGiftProcessor = new AcknowledgeGiftProcessor(giftRepository);
        collectGiftProcessor = new CollectGiftProcessor(giftRepository, playerRepository, giftAuditingService, internalWalletService, giftProperties);
    }

    public void createPlayersWhoAreFriends(final BigDecimal... playerIds) {
        for (BigDecimal playerId : playerIds) {
            final Player player = new Player(playerId);
            player.setName(playerNames.get(playerId.intValue()));
            when(playerRepository.findById(playerId)).thenReturn(player);
            friends.put(playerId, newHashSet(playerIds));
        }
    }

    public Set<BigDecimal> playerGivesGiftTo(final BigDecimal sender, final BigDecimal... receiver) {
        return giftService.giveGifts(sender, newHashSet(receiver), SESSION_ID);
    }

    public void playerGoesToHisGiftingStatusPage(final BigDecimal playerId) {
        currentlyShownPage = giftService.getGiftableStatusForPlayers(playerId, friends.get(playerId));
    }


    public Giftable playersGiftingStatusShows(final BigDecimal buddy) {
        for (GiftableStatus giftableStatus : currentlyShownPage) {
            if (giftableStatus.getPlayerId().equals(buddy)) {
                return giftableStatus.getGiftable();
            }
        }

        throw new RuntimeException("player should be in giftable status");
    }

    public boolean playerGiftReceivedMessageFrom(final BigDecimal receiver) {
        return giftRepository.getReceivedPublished().contains(receiver);
    }

    public boolean biRecordsGivenGiftisGiven(final BigDecimal giftId) {
        return giftSentEventQueue.contains(giftId);
    }

    public void playerCollectsGift(final BigDecimal playerId, final BigDecimal giftId, CollectChoice choice) throws GiftCollectionFailure {
        giftService.collectGift(playerId, giftId, choice, SESSION_ID);
    }

    public boolean playerReceivedCollectionStatusPush(final BigDecimal playerId,
                                                      final int expectedGiftsRemaining,
                                                      final int expectedCollectableGiftCount) {
        final Set<PlayerCollectionStatus> storedStatus = giftRepository.getCollectionStatusPublished().get(playerId);
        return storedStatus != null
                && storedStatus.contains(new PlayerCollectionStatus(expectedGiftsRemaining, expectedCollectableGiftCount));
    }

    public boolean playerReceivedCollectionStatusPush(final BigDecimal playerId) {
        return giftRepository.getCollectionStatusPublished().containsKey(playerId);
    }

    public boolean giftCollectedEventSent(final BigDecimal giftId) {
        return giftCollectEventQueue.contains(giftId);
    }

    public class DummyGiftRepository implements GiftRepository {
        private final Map<BigDecimal, Set<PlayerCollectionStatus>> collectionStatusPublished = new HashMap<>();
        private final Set<BigDecimal> receivedPublished = new HashSet<>();
        private final Map<BigDecimal, Gift> gifts = new HashMap<>();

        @Override
        public Set<BigDecimal> findLocalRecipientsBySender(final BigDecimal sendingPlayerId, final DateTime createdSince) {
            final Set<BigDecimal> matches = new HashSet<>();
            for (Gift gift : gifts.values()) {
                if (gift.getSendingPlayer().equals(sendingPlayerId) && gift.getCreated().isAfter(createdSince)) {
                    matches.add(gift.getRecipientPlayer());
                }
            }
            return matches;
        }

        @Override
        public Set<Gift> findAvailableByRecipient(final BigDecimal recipientPlayerId) {
            final Set<Gift> matches = new HashSet<>();
            for (Gift gift : gifts.values()) {
                if (gift.getRecipientPlayer().equals(recipientPlayerId)
                        && (gift.getExpiry().isAfter(new DateTime()) || !gift.getAcknowledged())
                        && gift.getCollected() == null) {
                    matches.add(gift);
                }
            }
            return matches;
        }

        @Override
        public Gift findByRecipientAndId(final BigDecimal recipientPlayerId, final BigDecimal giftId) {
            for (Gift gift : gifts.values()) {
                if (gift.getRecipientPlayer().equals(recipientPlayerId) && gift.getId().equals(giftId)) {
                    return gift;
                }
            }
            return null;
        }

        @Override
        public Gift lockByRecipientAndId(final BigDecimal recipientPlayerId, final BigDecimal giftId) {
            for (Gift gift : gifts.values()) {
                if (gift.getRecipientPlayer().equals(recipientPlayerId) && gift.getId().equals(giftId)) {
                    return gift;
                }
            }
            return null;
        }

        @Override
        public int countCollectedOn(final BigDecimal recipientPlayerId, final DateTime startOfDay) {
            int collected = 0;
            for (Gift gift : gifts.values()) {
                if (gift.getRecipientPlayer().equals(recipientPlayerId)
                        && gift.getCollected() != null
                        && gift.getCollected().isAfter(startOfDay)) {
                    ++collected;
                }
            }
            return collected;
        }

        @Override
        public int countAvailableForCollection(final BigDecimal recipientPlayerId) {
            int available = 0;
            for (Gift gift : gifts.values()) {
                if (gift.getRecipientPlayer().equals(recipientPlayerId)
                        && gift.getCollected() == null
                        && gift.getExpiry().isAfter(new DateTime())) {
                    ++available;
                }
            }
            return available;
        }

        @Override
        public void save(final Gift gift) {
            gifts.put(gift.getId(), gift);
        }

        @Override
        public void cleanUpOldGifts(final int retentionInHours) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void publishReceived(final BigDecimal recipientPlayerId) {
            receivedPublished.add(recipientPlayerId);
        }

        @Override
        public void publishCollectionStatus(final BigDecimal playerId, final PlayerCollectionStatus playerCollectionStatus) {
            if (!collectionStatusPublished.containsKey(playerId)) {
                collectionStatusPublished.put(playerId, new HashSet<PlayerCollectionStatus>());
            }
            collectionStatusPublished.get(playerId).add(playerCollectionStatus);
        }

        @Override
        public void requestSendGifts(final BigDecimal sendingPlayerId,
                                     final BigDecimal sessionId,
                                     final Map<BigDecimal, BigDecimal> recipientPlayerIdToGiftIds) {
            for (BigDecimal recipientPlayerId : recipientPlayerIdToGiftIds.keySet()) {
                sendGiftProcessor.process(new SendGiftRequest(sendingPlayerId, recipientPlayerId, recipientPlayerIdToGiftIds.get(recipientPlayerId), sessionId));
            }
        }

        @Override
        public void requestAcknowledgement(final BigDecimal recipientPlayerId,
                                           final Set<BigDecimal> giftIds) {
            for (BigDecimal giftId : giftIds) {
                acknowledgeGiftProcessor.process(new AcknowledgeGiftRequest(recipientPlayerId, giftId));
            }
        }

        @Override
        public void requestCollection(final BigDecimal recipientPlayerId,
                                      final BigDecimal giftId,
                                      final BigDecimal sessionId,
                                      final BigDecimal giftWinnings,
                                      final CollectChoice choice) {
            collectGiftProcessor.process(new CollectGiftRequest(recipientPlayerId, giftId, sessionId, giftWinnings, choice));
        }

        public Map<BigDecimal, Set<PlayerCollectionStatus>> getCollectionStatusPublished() {
            return collectionStatusPublished;
        }

        public Set<BigDecimal> getReceivedPublished() {
            return receivedPublished;
        }
    }

    public class DummyGiftSentQueuePublishing<T extends GiftSentEvent> implements QueuePublishingService<T> {

        Map<BigDecimal, GiftSentEvent> handledMessages = new HashMap<>();

        @Override
        public void send(T event) {
            handledMessages.put(event.getGiftId(), event);
        }

        public boolean contains(BigDecimal giftId) {
            return handledMessages.containsKey(giftId);
        }
    }

    public class DummyGiftCollectedQueuePublishing<T extends GiftCollectedEvent> implements QueuePublishingService<T> {

        Map<BigDecimal, GiftCollectedEvent> handledMessages = new HashMap<>();

        @Override
        public void send(T event) {
            handledMessages.put(event.getGiftId(), event);
        }

        public boolean contains(BigDecimal giftId) {
            return handledMessages.containsKey(giftId);
        }
    }
}
