package com.yazino.web.data;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.*;

public class SessionServiceStub implements SessionService {

    private final Set<PlayerSessionStatus> statuses = new HashSet<PlayerSessionStatus>();

    @Override
    public Session createSession(@Routing("getPlayerId") BasicProfileInformation player,
                                 Partner partnerId,
                                 String referrer,
                                 String ipAddress,
                                 String emailAddress,
                                 Platform platform,
                                 String loginUrl,
                                 Map<String, Object> clientContext) {
        return null;
    }

    @Override
    public void invalidateByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {

    }

    @Override
    public void invalidateAllByPlayer(@Routing final BigDecimal playerId) {

    }

    @Override
    public int countSessions(boolean onlyPlaying) {
        return 0;
    }

    @Override
    public PagedData<Session> findSessions(int page) {
        return null;
    }

    @Override
    public Session authenticateAndExtendSession(@Routing BigDecimal playerId, String sessionKey) {
        return null;
    }

    @Override
    public void updatePlayerInformation(@Routing BigDecimal playerId, String playerNickname, String pictureUrl) {
    }

    @Override
    public Collection<Session> findAllByPlayer(@Routing final BigDecimal playerId) {
        return null;
    }

    @Override
    public Session findByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        return null;
    }

    @Override
    public Set<PlayerLocations> getGlobalPlayerList() {
        return null;
    }

    @Override
    public Set<PlayerSessionStatus> retrieveAllSessionStatuses() {
        return Collections.unmodifiableSet(statuses);
    }

    public synchronized void addStatus(PlayerSessionStatus playerSessionStatus) {
        statuses.add(playerSessionStatus);
    }

    public synchronized void removeStatus(PlayerSessionStatus playerSessionStatus) {
        statuses.remove(playerSessionStatus);
    }
}
