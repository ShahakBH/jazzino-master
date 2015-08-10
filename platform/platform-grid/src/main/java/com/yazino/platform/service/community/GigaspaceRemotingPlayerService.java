package com.yazino.platform.service.community;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.*;
import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.ProfileInformationBuilder;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import com.yazino.platform.service.account.InternalWalletService;
import org.joda.time.DateTime;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.player.GuestStatus.NON_GUEST;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingPlayerService implements PlayerService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingPlayerService.class);

    private final PlayerRepository playerRepository;
    private final PlayerLevelsRepository playerLevelsRepository;
    private final InternalWalletService internalWalletService;
    private final SequenceGenerator sequenceGenerator;
    private final ProfileInformationBuilder profileInformationBuilder;
    private final QueuePublishingService<PlayerEvent> playerEventService;

    @Autowired
    public GigaspaceRemotingPlayerService(final PlayerRepository playerRepository,
                                          final PlayerLevelsRepository playerLevelsRepository,
                                          final SequenceGenerator sequenceGenerator,
                                          final InternalWalletService internalWalletService,
                                          final ProfileInformationBuilder profileInformationBuilder,
                                          @Qualifier("playerEventQueuePublishingService") QueuePublishingService<PlayerEvent> playerEventService) {
        notNull(playerRepository, "playerRepository may not be null");
        notNull(playerLevelsRepository, "playerLevelsRepository may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(internalWalletService, "internalWalletService may not be null");
        notNull(profileInformationBuilder, "profileInformationBuilder may not be null");
        notNull(playerEventService, "playerEventService may not be null");

        this.playerRepository = playerRepository;
        this.playerLevelsRepository = playerLevelsRepository;
        this.internalWalletService = internalWalletService;
        this.sequenceGenerator = sequenceGenerator;
        this.profileInformationBuilder = profileInformationBuilder;
        this.playerEventService = playerEventService;
    }

    @Override
    public BasicProfileInformation createNewPlayer(final String displayName,
                                                   final String playerPictureUrl,
                                                   final GuestStatus guestStatus,
                                                   final PaymentPreferences paymentPreferences,
                                                   final PlayerCreditConfiguration playerCreditConfiguration) {
        notNull(displayName, "displayName may not be null");
        notNull(playerPictureUrl, "playerPictureUrl may not be null");
        notNull(guestStatus, "guestStatus may not be null");
        notNull(paymentPreferences, "paymentPreferences may not be null");
        notNull(playerCreditConfiguration, "playerCreditConfiguration may not be null");

        final BigDecimal accountId = createPlayerAccount(displayName, playerCreditConfiguration, guestStatus);
        final BigDecimal playerId = sequenceGenerator.next();
        final Player player = new Player(playerId, displayName, accountId, playerPictureUrl, paymentPreferences, new DateTime(), null);

        playerRepository.save(player);
        playerLevelsRepository.save(new PlayerLevels(playerId, Collections.<String, PlayerLevel>emptyMap()));

        playerEventService.send(asEvent(player));

        return new BasicProfileInformation(playerId, displayName, playerPictureUrl, accountId);
    }

    private PlayerEvent asEvent(final Player player) {
        return new PlayerEvent(player.getPlayerId(),
                    player.getCreationTime(),
                    player.getAccountId(),
                    player.getTags());
    }

    private BigDecimal createPlayerAccount(final String concatenatedName,
                                           final PlayerCreditConfiguration creditConfig,
                                           final GuestStatus guestStatus) {
        final BigDecimal accountId;
        try {
            accountId = internalWalletService.createAccount(concatenatedName);
            if (guestStatus == NON_GUEST && creditConfig.getInitialAmount().compareTo(BigDecimal.ZERO) > 0) {
                internalWalletService.postTransaction(accountId, creditConfig.getInitialAmount(), "Create Account",
                        "account setup", TransactionContext.EMPTY);
            }
        } catch (final WalletServiceException e) {
            throw new RuntimeException(e);
        }
        return accountId;
    }

    @Override
    public void updatePaymentPreferences(@Routing final BigDecimal playerId,
                                         final PaymentPreferences paymentPreferences) {
        notNull(playerId, "playerId may not be null");
        notNull(paymentPreferences, "paymentPreferences may not be null");

        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            return;
        }

        player.setPaymentPreferences(paymentPreferences);
        playerRepository.save(player);
    }

    @Override
    public void asyncRegisterFriends(@Routing final BigDecimal playerId,
                                     final Set<BigDecimal> friendPlayerIds) {
        // as per http://www.gigaspaces.com/wiki/display/XAP96/Executor+Based+Remoting, the
        // implementation is empty and #sendCommand will be invoked
    }

    @Override
    public void registerFriends(@Routing final BigDecimal playerId,
                                final Set<BigDecimal> friendPlayerIds) {
        notNull(playerId, "playerId may not be null");
        notNull(friendPlayerIds, "friendPlayerIds may not be null");

        if (friendPlayerIds.isEmpty()) {
            return;
        }

        playerRepository.requestFriendRegistration(playerId, friendPlayerIds);
    }

    @Override
    public BigDecimal getAccountId(@Routing final BigDecimal playerId) {
        return getPlayer(playerId).getAccountId();
    }

    @Override
    public Set<BigDecimal> getFriends(@Routing final BigDecimal playerId) {
        return newHashSet(getPlayer(playerId).retrieveFriends().keySet());
    }

    @Override
    public Map<BigDecimal, String> getFriendsOrderedByNickname(@Routing BigDecimal playerId) {
        return sortedFriendsMatchingType(playerId, RelationshipType.FRIEND);
    }

    @Override
    public List<BigDecimal> getFriendRequestsOrderedByNickname(@Routing BigDecimal playerId) {
        final ArrayList<BigDecimal> requests = newArrayList();
        requests.addAll(sortedFriendsMatchingType(playerId, RelationshipType.INVITATION_RECEIVED).keySet());
        return requests;
    }

    @Override
    public PaymentPreferences getPaymentPreferences(@Routing final BigDecimal playerId) {
        return getPlayer(playerId).getPaymentPreferences();
    }

    @Override
    public String getPictureUrl(@Routing final BigDecimal playerId) {
        return getPlayer(playerId).getPictureUrl();
    }

    @Override
    public Map<BigDecimal, Relationship> getRelationships(@Routing final BigDecimal playerId) {
        return getPlayer(playerId).retrieveRelationships();
    }

    @Override
    public BigDecimal postTransaction(@Routing final BigDecimal playerId,
                                      final BigDecimal sessionId,
                                      final BigDecimal amountOfChips,
                                      final String transactionType,
                                      final String reference) throws WalletServiceException {
        final BigDecimal accountId = getPlayer(playerId).getAccountId();
        return internalWalletService.postTransaction(accountId, amountOfChips, transactionType, reference, transactionContextFor(sessionId));
    }

    private TransactionContext transactionContextFor(final BigDecimal sessionId) {
        if (sessionId != null) {
            return transactionContext().withSessionId(sessionId).build();
        }
        return TransactionContext.EMPTY;
    }

    @Override
    public ProfileInformation getProfileInformation(@Routing final BigDecimal playerId, final String gameType) {
        final Player player = getPlayer(playerId);
        return profileInformationBuilder.buildProfileInformation(player, gameType);
    }

    @Override
    public BasicProfileInformation getBasicProfileInformation(@Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final Player player = playerRepository.findById(playerId);
        if (player != null) {
            return new BasicProfileInformation(player.getPlayerId(), player.getName(),
                    player.getPictureUrl(), player.getAccountId());
        }
        return null;
    }

    @Override
    public void publishFriendsSummary(@Routing final BigDecimal playerId) {
        LOG.debug("Publishing friends summary for player {}", playerId);

        if (playerId != null) {
            playerRepository.publishFriendsSummary(playerId);
        }
    }

    @Override
    public void asyncPublishFriendsSummary(@Routing final BigDecimal playerId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #sendCommand will be invoked
    }

    @Override
    public void addTag(@Routing final BigDecimal playerId, final String tag) {
        notNull(playerId, "playerId may not be null");
        notNull(tag, "tag may not be null");

        playerRepository.addTag(playerId, tag);
    }

    @Override
    public void removeTag(@Routing final BigDecimal playerId, final String tag) {
        notNull(playerId, "playerId may not be null");
        notNull(tag, "tag may not be null");

        playerRepository.removeTag(playerId, tag);
    }

    private Player getPlayer(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        notNull(player, "Player with id [" + playerId + "] was not found.");
        return player;
    }

    private Map<BigDecimal, String> sortedFriendsMatchingType(BigDecimal playerId, RelationshipType type) {
        Player player = getPlayer(playerId);
        Map<BigDecimal, Relationship> relationships = player.listRelationships(type);

        if (relationships.isEmpty()) {
            return Collections.emptyMap();
        }

        Comparator<Map.Entry<BigDecimal, Relationship>> byNickname = new Comparator<Map.Entry<BigDecimal, Relationship>>() {

            @Override
            public int compare(Map.Entry<BigDecimal, Relationship> entryA, Map.Entry<BigDecimal, Relationship> entryB) {
                String nicknameA = Strings.nullToEmpty(entryA.getValue().getNickname());
                String nicknameB = Strings.nullToEmpty(entryB.getValue().getNickname());

                return nicknameA.compareToIgnoreCase(nicknameB);
            }

        };
        Set<Map.Entry<BigDecimal, Relationship>> entries = relationships.entrySet();
        Set<Map.Entry<BigDecimal, Relationship>> sorted = Sets.newTreeSet(byNickname);
        sorted.addAll(entries);

        Map<BigDecimal, String> sortedIds = new LinkedHashMap<>(sorted.size());
        for (Map.Entry<BigDecimal, Relationship> entry : sorted) {
            sortedIds.put(entry.getKey(), entry.getValue().getNickname());
        }
        return sortedIds;
    }

}
