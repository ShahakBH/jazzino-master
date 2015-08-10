package com.yazino.platform.processor.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerSessionDocumentProperties;
import com.yazino.platform.model.session.PlayerSessionsSummary;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.community.PlayerWorker;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
import com.yazino.platform.session.LocationChange;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 3)
public class LocationChangeNotificationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(LocationChangeNotificationProcessor.class);

    private final InternalWalletService internalWalletService;
    private final PlayerRepository playerRepository;
    private final DocumentDispatcher documentDispatcher;
    private final TableInviteRepository tableInviteRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final TransactionalSessionService transactionalSessionService;

    @SuppressWarnings("UnusedDeclaration")
    LocationChangeNotificationProcessor() {
        // CGLib constructor

        this.documentDispatcher = null;
        this.playerRepository = null;
        this.playerSessionRepository = null;
        this.internalWalletService = null;
        this.tableInviteRepository = null;
        this.transactionalSessionService = null;
    }

    @Autowired(required = true)
    public LocationChangeNotificationProcessor(
            @Qualifier("spaceGlobalDocumentDispatcher") final DocumentDispatcher documentDispatcher,
            final PlayerRepository playerRepository,
            final InternalWalletService internalWalletService,
            final TableInviteRepository tableInviteRepository,
            final PlayerSessionRepository playerSessionRepository,
            final TransactionalSessionService transactionalSessionService) {
        notNull(playerSessionRepository, "playerSessionRepository may not be null");
        notNull(transactionalSessionService, "transactionalSessionService may not be null");

        this.documentDispatcher = documentDispatcher;
        this.playerRepository = playerRepository;
        this.internalWalletService = internalWalletService;
        this.tableInviteRepository = tableInviteRepository;
        this.playerSessionRepository = playerSessionRepository;
        this.transactionalSessionService = transactionalSessionService;
    }

    @EventTemplate
    public LocationChangeNotification template() {
        return new LocationChangeNotification();
    }

    @SpaceDataEvent
    public void processLocationChange(final LocationChangeNotification notification)
            throws ConcurrentModificationException {
        LOG.debug("Processing LocationChangeNotification {}", notification);
        try {
            final Player player = playerRepository.findById(notification.getPlayerId());
            if (player == null) {
                return; // no such player
            }
            final PlayerWorker pw = new PlayerWorker();

            final PlayerSessionsSummary session = updateSession(player, notification);

            final Set<BigDecimal> candidates = player.listRelationships(RelationshipType.FRIEND).keySet();
            final Set<BigDecimal> onlineFriends = playerSessionRepository.findOnlinePlayers(new HashSet<>(candidates));
            if (notification.getLocation() != null && notification.getNotificationType() != null) {
                LOG.debug("Sending notification to original player {}", player.getPlayerId());
                documentDispatcher.dispatch(pw.buildOwnLocationDocument(notification), player.getPlayerId());
            }

            LOG.debug("Sending notification to interested players: {}", onlineFriends);
            for (BigDecimal friend : onlineFriends) {
                final PlayerSessionDocumentProperties playerSessionProperties;
                playerSessionProperties = pw.getLocationDocumentProperties(friend, session, tableInviteRepository);
                documentDispatcher.dispatch(pw.buildLocationDocument(
                        player.getPlayerId(), playerSessionProperties), friend);
            }

        } catch (Exception e) {
            LOG.error("Failed to process a location change notification: {}", notification, e);
        }
    }

    private PlayerSessionsSummary updateSession(final Player player,
                                                final LocationChangeNotification notification) {
        BigDecimal balance = null;
        try {
            balance = internalWalletService.getBalance(player.getAccountId());
        } catch (WalletServiceException e) {
            LOG.error("Couldn't read balance snapshot", e);
        }

        final LocationChange change = new LocationChange(notification.getPlayerId(),
                                                         notification.getSessionId(), notification.getNotificationType(), notification.getLocation());

        playerSessionRepository.updateGlobalPlayerList(notification.getPlayerId());
        return transactionalSessionService.updateSession(change, balance);
    }

}
