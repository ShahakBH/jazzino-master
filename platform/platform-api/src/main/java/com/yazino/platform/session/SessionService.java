package com.yazino.platform.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface SessionService {

    Session createSession(@Routing("getPlayerId") BasicProfileInformation player,
                          Partner partnerId,
                          String referrer,
                          String ipAddress,
                          String emailAddress,
                          Platform platform,
                          String loginUrl,
                          final Map<String, Object> clientContext);

    void invalidateAllByPlayer(@Routing BigDecimal playerId);

    void invalidateByPlayerAndSessionKey(@Routing BigDecimal playerId,
                                         String sessionKey);

    Session authenticateAndExtendSession(@Routing BigDecimal playerId,
                                         String sessionKey);

    Collection<Session> findAllByPlayer(@Routing BigDecimal playerId);

    Session findByPlayerAndSessionKey(@Routing BigDecimal playerId,
                                      String sessionKey);

    void updatePlayerInformation(@Routing BigDecimal playerId,
                                 String playerNickname,
                                 String pictureUrl);

    int countSessions(boolean onlyPlaying);

    PagedData<Session> findSessions(int page);

    Set<PlayerLocations> getGlobalPlayerList();

    Set<PlayerSessionStatus> retrieveAllSessionStatuses();
}
