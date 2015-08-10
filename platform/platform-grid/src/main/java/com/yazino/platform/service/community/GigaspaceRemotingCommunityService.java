package com.yazino.platform.service.community;

import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.community.RelationshipRequest;
import com.yazino.platform.model.community.*;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.SystemMessageRepository;
import com.yazino.platform.session.LocationChangeType;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingCommunityService implements CommunityService {

    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingCommunityService.class);

    private final GigaSpace communityGigaSpace;
    private final GigaSpace communityGlobalGigaSpace;
    private final PlayerRepository playerRepository;
    private final SystemMessageRepository systemMessageRepository;

    @Autowired
    public GigaspaceRemotingCommunityService(
            @Qualifier("gigaSpace") final GigaSpace communityGigaSpace,
            @Qualifier("globalGigaSpace") final GigaSpace communityGlobalGigaSpace,
            final PlayerRepository playerRepository,
            final SystemMessageRepository systemMessageRepository) {
        notNull(communityGigaSpace, "communityGigaSpace may not be null");
        notNull(communityGlobalGigaSpace, "communityGlobalGigaSpace may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(systemMessageRepository, "systemMessageRepository may not be null");

        this.communityGigaSpace = communityGigaSpace;
        this.communityGlobalGigaSpace = communityGlobalGigaSpace;
        this.playerRepository = playerRepository;
        this.systemMessageRepository = systemMessageRepository;
    }

    @Override
    public void sendPlayerLoggedOn(@Routing final BigDecimal playerId, final BigDecimal sessionId) {
        communityGigaSpace.write(new LocationChangeNotification(playerId, sessionId, LocationChangeType.LOG_ON, null));
    }

    @Override
    public void asyncSendPlayerLoggedOn(@Routing final BigDecimal playerId, final BigDecimal sessionId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #requestRelationshipChange will be invoked
    }

    @Override
    public void updatePlayer(@Routing final BigDecimal playerId,
                             final String displayName,
                             final String pictureLocation,
                             final PaymentPreferences paymentPreferences) {
        notNull(playerId, "Player ID may not be null");
        notNull(displayName, "Display Name may not be null");
        notNull(pictureLocation, "Picture location may not be null");
        notNull(paymentPreferences, "Payment Preferences may not be null");

        communityGigaSpace.write(new UpdatePlayerRequest(playerId, displayName, pictureLocation, paymentPreferences));
    }

    @Override
    public void asyncUpdatePlayer(@Routing final BigDecimal playerId,
                                  final String displayName,
                                  final String pictureLocation,
                                  final PaymentPreferences paymentPreferences) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #requestRelationshipChange will be invoked
    }

    @Override
    public void publishBalance(@Routing final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        LOG.debug("Publishing statistics for player {}", playerId);

        communityGigaSpace.write(new PublishStatusRequest(playerId, PublishStatusRequestType.PLAYER_BALANCE));
    }

    @Override
    public void asyncPublishBalance(@Routing final BigDecimal playerId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #requestRelationshipChange will be invoked
    }

    @Override
    public void publishCommunityStatus(@Routing final BigDecimal playerId,
                                       final String gameType) {
        notNull(playerId, "Player ID may not be null");

        LOG.debug("Publishing relationships for player {}", playerId);

        communityGigaSpace.write(new PublishStatusRequest(playerId, gameType));
    }

    @Override
    public void asyncPublishCommunityStatus(@Routing final BigDecimal playerId, final String gameType) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #publishCommunityStatus will be invoked
    }


    @Override
    public void requestRelationshipAction(@Routing("getPlayerId") final RelationshipRequest actionRequest) {
        notNull(actionRequest, "Relationship Action Request may not be null");

        LOG.debug("Requesting relationship action {}", actionRequest);

        final RelationshipActionRequest relationshipActionRequest = new RelationshipActionRequest(
                actionRequest.getPlayerId(), actionRequest.getRelatedPlayers());

        communityGigaSpace.write(relationshipActionRequest);
    }

    @Override
    public void asyncRequestRelationshipAction(@Routing("getPlayerId") final RelationshipRequest actionRequest) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #requestRelationshipChange will be invoked
    }

    @Override
    public void requestRelationshipChange(@Routing final BigDecimal playerId,
                                          final BigDecimal relatedPlayerId,
                                          final RelationshipAction relationshipAction) {
        LOG.debug("requestRelationshipChange {} {} {}", playerId, relatedPlayerId, relationshipAction);

        final Player actioning = playerRepository.findById(playerId);
        final Player related = playerRepository.findById(relatedPlayerId);

        communityGigaSpace.write(new RelationshipActionRequest(playerId, relatedPlayerId, related.getName(),
                relationshipAction, false));
        communityGlobalGigaSpace.write(new RelationshipActionRequest(relatedPlayerId, playerId, actioning.getName(),
                relationshipAction, true));
    }

    @Override
    public void asyncRequestRelationshipChange(@Routing final BigDecimal playerId,
                                               final BigDecimal relatedPlayerId,
                                               final RelationshipAction relationshipAction) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #requestRelationshipChange will be invoked
    }

    @Override
    public String getLatestSystemMessage() {
        final List<SystemMessage> messages = systemMessageRepository.findValid();
        if (messages.size() > 0) {
            return messages.get(0).getMessage();
        }
        return null;
    }

}
