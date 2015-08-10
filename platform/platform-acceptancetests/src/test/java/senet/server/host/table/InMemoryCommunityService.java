package senet.server.host.table;


import com.yazino.platform.community.*;
import com.yazino.platform.test.InMemoryPlayerDetailsService;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class InMemoryCommunityService implements CommunityService {
    private final InMemoryPlayerDetailsService playerDetailsService;

    @Autowired
    public InMemoryCommunityService(final InMemoryPlayerDetailsService playerDetailsService) {
        this.playerDetailsService = playerDetailsService;
    }

    @Override
    public void sendPlayerLoggedOn(@Routing final BigDecimal userId,
                                   final BigDecimal sessionId) {
    }

    @Override
    public void asyncSendPlayerLoggedOn(@Routing final BigDecimal playerId,
                                        final BigDecimal sessionId) {
    }

    @Override
    public void updatePlayer(final BigDecimal playerId,
                             final String displayName,
                             final String pictureLocation,
                             final PaymentPreferences paymentPreferences) {
        playerDetailsService.save(new BasicProfileInformation(playerId, displayName, pictureLocation, playerId));
    }

    @Override
    public void asyncUpdatePlayer(@Routing final BigDecimal playerId,
                                  final String displayName,
                                  final String pictureLocation,
                                  final PaymentPreferences paymentPreferences) {
        playerDetailsService.save(new BasicProfileInformation(playerId, displayName, pictureLocation, playerId));
    }

    @Override
    public void publishBalance(final BigDecimal playerId) {
    }

    @Override
    public void asyncPublishBalance(@Routing final BigDecimal playerId) {
    }

    @Override
    public void publishCommunityStatus(final BigDecimal playerId,
                                       final String gameType) {
    }

    @Override
    public void asyncPublishCommunityStatus(@Routing final BigDecimal playerId, final String gameType) {
    }

    @Override
    public void requestRelationshipAction(final RelationshipRequest actionRequest) {
    }

    @Override
    public void asyncRequestRelationshipAction(@Routing("getPlayerId") final RelationshipRequest actionRequest) {
    }

    @Override
    public String getLatestSystemMessage() {
        return null;
    }

    @Override
    public void requestRelationshipChange(@Routing final BigDecimal playerId,
                                          final BigDecimal relatedPlayerId,
                                          final RelationshipAction relationshipAction) {
    }

    @Override
    public void asyncRequestRelationshipChange(@Routing final BigDecimal playerId,
                                               final BigDecimal relatedPlayerId,
                                               final RelationshipAction relationshipAction) {
    }

}
