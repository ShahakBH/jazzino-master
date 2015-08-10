package com.yazino.host.community;

import com.yazino.host.account.StandaloneInternalWalletService;
import com.yazino.host.table.StandaloneTableInviteRepository;
import com.yazino.host.table.document.StandaloneDocumentDispatcher;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipRequest;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.model.community.LocationChangeNotification;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.RelationshipActionRequest;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.community.PlayerWorker;
import com.yazino.platform.session.LocationChangeType;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StandaloneCommunityService implements CommunityService {
    private final PlayerSessionRepository playerSessionRepository;
    private final StandalonePlayerRepository playerRepository;
    private final StandaloneInternalWalletService internalWalletService;
    private final StandaloneTableInviteRepository tableInviteRepository;
    private final PlayerWorker playerWorker = new PlayerWorker();
    private final StandaloneDocumentDispatcher documentDispatcher;
    private final StandaloneLocationChangeNotificationProcessor locationChangeNotificationProcessor;
    private final StandaloneRelationshipActionProcessor relationshipActionProcessor;

    @Autowired
    public StandaloneCommunityService(final StandalonePlayerRepository playerRepository,
                                      final StandaloneTableInviteRepository tableInviteRepository,
                                      final PlayerSessionRepository playerSessionRepository,
                                      @Qualifier("standaloneDocumentDispatcher")
                                      final StandaloneDocumentDispatcher documentDispatcher,
                                      final StandaloneLocationChangeNotificationProcessor
                                              locationChangeNotificationProcessor,
                                      final StandaloneInternalWalletService internalWalletService,
                                      final StandaloneRelationshipActionProcessor relationshipActionProcessor) {
        this.playerRepository = playerRepository;
        this.tableInviteRepository = tableInviteRepository;
        this.documentDispatcher = documentDispatcher;
        this.locationChangeNotificationProcessor = locationChangeNotificationProcessor;
        this.internalWalletService = internalWalletService;
        this.relationshipActionProcessor = relationshipActionProcessor;
        this.playerSessionRepository = playerSessionRepository;
    }

    @Override
    public void sendPlayerLoggedOn(@Routing final BigDecimal playerId, final BigDecimal sessionId) {
        final LocationChangeNotification notification = new LocationChangeNotification(playerId,
                sessionId, LocationChangeType.LOG_ON, null);
        locationChangeNotificationProcessor.processLocationChange(notification);
    }

    @Override
    public void asyncSendPlayerLoggedOn(@Routing final BigDecimal playerId, final BigDecimal sessionId) {
        sendPlayerLoggedOn(playerId, sessionId);
    }

    @Override
    public void updatePlayer(@Routing final BigDecimal playerId,
                             final String displayName,
                             final String pictureLocation,
                             final PaymentPreferences paymentPreferences) {

    }

    @Override
    public void asyncUpdatePlayer(@Routing final BigDecimal playerId,
                                  final String displayName,
                                  final String pictureLocation,
                                  final PaymentPreferences paymentPreferences) {
    }

    @Override
    public void publishBalance(@Routing final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        final Document document = playerWorker.buildPlayerBalanceDocument(player, internalWalletService);
        documentDispatcher.dispatch(document, player.getPlayerId());
    }

    @Override
    public void asyncPublishBalance(@Routing final BigDecimal playerId) {
        publishBalance(playerId);
    }

    @Override
    public void publishCommunityStatus(@Routing final BigDecimal playerId,
                                       final String gameType) {
        final Player player = playerRepository.findById(playerId);
        final Document document = playerWorker.buildRelationshipDocument(player, playerSessionRepository, tableInviteRepository);
        documentDispatcher.dispatch(document, playerId);
    }

    @Override
    public void asyncPublishCommunityStatus(@Routing final BigDecimal playerId, final String gameType) {
        publishCommunityStatus(playerId, gameType);
    }

    @Override
    public String getLatestSystemMessage() {
        return null;
    }

    @Override
    public void requestRelationshipAction(@Routing("getPlayerId") final RelationshipRequest actionRequest) {
        final RelationshipActionRequest relationshipActionRequest = new RelationshipActionRequest(
                actionRequest.getPlayerId(), actionRequest.getRelatedPlayers());

        relationshipActionProcessor.processRelationshipRequest(relationshipActionRequest);
    }

    @Override
    public void asyncRequestRelationshipAction(@Routing("getPlayerId") final RelationshipRequest actionRequest) {
        requestRelationshipAction(actionRequest);
    }

    @Override
    public void asyncRequestRelationshipChange(@Routing final BigDecimal playerId,
                                               final BigDecimal relatedPlayerId,
                                               final RelationshipAction relationshipAction) {
        requestRelationshipChange(playerId, relatedPlayerId, relationshipAction);
    }

    @Override
    public void requestRelationshipChange(@Routing final BigDecimal playerId,
                                          final BigDecimal relatedPlayerId,
                                          final RelationshipAction relationshipAction) {

        final Player actioning = playerRepository.findById(playerId);
        final Player related = playerRepository.findById(relatedPlayerId);

        final RelationshipActionRequest request1 = new RelationshipActionRequest(playerId,
                relatedPlayerId,
                related.getName(),
                relationshipAction,
                false);
        final RelationshipActionRequest request2 = new RelationshipActionRequest(relatedPlayerId,
                playerId,
                actioning.getName(),
                relationshipAction,
                true);
        relationshipActionProcessor.processRelationshipRequest(request1);
        relationshipActionProcessor.processRelationshipRequest(request2);
    }

}
