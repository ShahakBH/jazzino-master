package com.yazino.platform.community;


import org.openspaces.remoting.Routing;

import java.math.BigDecimal;

public interface CommunityService {

    void sendPlayerLoggedOn(@Routing BigDecimal playerId,
                            BigDecimal sessionId);

    void asyncSendPlayerLoggedOn(@Routing BigDecimal playerId,
                                 BigDecimal sessionId);

    void updatePlayer(@Routing BigDecimal playerId,
                      String displayName,
                      String pictureLocation,
                      PaymentPreferences paymentPreferences);

    void asyncUpdatePlayer(@Routing BigDecimal playerId,
                           String displayName,
                           String pictureLocation,
                           PaymentPreferences paymentPreferences);

    void publishBalance(@Routing BigDecimal playerId);

    void asyncPublishBalance(@Routing BigDecimal playerId);

    void publishCommunityStatus(@Routing BigDecimal playerId,
                                String gameType);

    void asyncPublishCommunityStatus(@Routing BigDecimal playerId,
                                     String gameType);

    String getLatestSystemMessage();

    void requestRelationshipAction(@Routing("getPlayerId") RelationshipRequest actionRequest);

    void asyncRequestRelationshipAction(@Routing("getPlayerId") RelationshipRequest actionRequest);

    void requestRelationshipChange(@Routing BigDecimal playerId,
                                   BigDecimal relatedPlayerId,
                                   RelationshipAction relationshipAction);

    void asyncRequestRelationshipChange(@Routing BigDecimal playerId,
                                        BigDecimal relatedPlayerId,
                                        RelationshipAction relationshipAction);

}
