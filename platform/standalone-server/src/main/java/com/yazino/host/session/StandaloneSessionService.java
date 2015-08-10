package com.yazino.host.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import org.joda.time.DateTime;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class StandaloneSessionService implements SessionService {
    @Autowired
    private StandalonePlayerSessionRepository playerSessionRepository;

    @Override
    public Session createSession(final BasicProfileInformation player,
                                 final Partner partnerId,
                                 final String referrer,
                                 final String ipAddress,
                                 final String emailAddress,
                                 final Platform platform, String loginUrl,
                                 final Map<String, Object> clientContext) {
        final PlayerSession session = new PlayerSession(BigDecimal.ZERO.subtract(player.getPlayerId()), player.getPlayerId(),
                String.valueOf(player.getPlayerId()), player.getPictureUrl(), player.getName(),
                partnerId, platform, ipAddress, BigDecimal.ZERO, "");
        playerSessionRepository.save(session);
        return asSession(session);
    }

    private Session asSession(final PlayerSession session) {
        DateTime timestamp = null;
        if (session.getTimestamp() != null) {
            timestamp = new DateTime(session.getTimestamp());
        }
        return new Session(session.getSessionId(), session.getPlayerId(),
                session.getPartnerId(),
                session.getPlatform(),
                session.getIpAddress(),
                session.getLocalSessionKey(),
                session.getNickname(),
                session.getEmail(),
                session.getPictureUrl(),
                session.getBalanceSnapshot(),
                timestamp,
                session.getLocations(),
                Collections.<String>emptySet());
    }

    @Override
    public void invalidateAllByPlayer(@Routing final BigDecimal playerId) {
        playerSessionRepository.removeAllByPlayer(playerId);
    }

    @Override
    public int countSessions(final boolean b) {
        return playerSessionRepository.findAllSessionStatuses().size();
    }

    @Override
    public PagedData<Session> findSessions(final int i) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Session authenticateAndExtendSession(final BigDecimal playerId, final String sessionKey) {
        final Collection<PlayerSession> allSessions = playerSessionRepository.findAllByPlayer(playerId);
        if (!allSessions.isEmpty()) {
            return asSession(allSessions.iterator().next());
        }
        return null;
    }

    @Override
    public void updatePlayerInformation(final BigDecimal playerId,
                                        final String playerNickname,
                                        final String pictureUrl) {
        final Collection<PlayerSession> playerSession = playerSessionRepository.findAllByPlayer(playerId);
        for (PlayerSession session : playerSession) {
            session.setNickname(playerNickname);
            session.setPictureUrl(pictureUrl);
        }
    }

    @Override
    public void invalidateByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        playerSessionRepository.removeAllByPlayer(playerId);
    }

    @Override
    public Collection<Session> findAllByPlayer(@Routing final BigDecimal playerId) {
        final Collection<Session> sessions = new HashSet<>();
        for (PlayerSession session : playerSessionRepository.findAllByPlayer(playerId)) {
            sessions.add(asSession(session));
        }
        return sessions;
    }

    @Override
    public Session findByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        final Collection<PlayerSession> sessions = playerSessionRepository.findAllByPlayer(playerId);
        if (!sessions.isEmpty()) {
            return asSession(sessions.iterator().next());
        }
        return null;
    }

    @Override
    public Set<PlayerLocations> getGlobalPlayerList() {
        return Collections.emptySet();
    }

    @Override
    public Set<PlayerSessionStatus> retrieveAllSessionStatuses() {
        return playerSessionRepository.findAllSessionStatuses();
    }
}
