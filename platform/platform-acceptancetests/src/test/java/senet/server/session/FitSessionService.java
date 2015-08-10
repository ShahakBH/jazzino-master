package senet.server.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.session.*;
import org.joda.time.DateTime;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.platform.repository.session.PlayerSessionRepository;

import java.math.BigDecimal;
import java.util.*;

public class FitSessionService implements SessionService {

    private final PlayerSessionRepository playerSessionRepository;

    @Autowired
    public FitSessionService(final PlayerSessionRepository playerSessionRepository) {
        this.playerSessionRepository = playerSessionRepository;
    }

    @Override
    public Session createSession(@Routing("getPlayerId") final BasicProfileInformation player,
                                 final Partner partnerId,
                                 final String referrer,
                                 final String ipAddress,
                                 final String emailAddress,
                                 final Platform platform, String loginUrl,
                                 final Map<String, Object> clientContext) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void invalidateAllByPlayer(@Routing final BigDecimal playerId) {
        playerSessionRepository.removeAllByPlayer(playerId);
    }

    @Override
    public int countSessions(final boolean onlyPlaying) {
        return playerSessionRepository.countPlayerSessions(false);
    }

    @Override
    public PagedData<Session> findSessions(final int page) {
        final PagedData<PlayerSession> data = playerSessionRepository.findAll(page);

        final List<Session> sessions = new ArrayList<>();
        for (PlayerSession playerSession : data.getData()) {
            sessions.add(asSession(playerSession));
        }

        return new PagedData<>(data.getStartPosition(),
                data.getSize(),
                data.getTotalSize(),
                sessions);
    }

    private Session asSession(final PlayerSession playerSession) {
        if (playerSession == null) {
            return null;
        }

        DateTime timestamp = null;
        if (playerSession.getTimestamp() != null) {
            timestamp = new DateTime(playerSession.getTimestamp());
        }
        return new Session(playerSession.getSessionId(), playerSession.getPlayerId(), playerSession.getPartnerId(),
                playerSession.getPlatform(), playerSession.getIpAddress(), playerSession.getLocalSessionKey(),
                playerSession.getNickname(), playerSession.getEmail(), playerSession.getPictureUrl(),
                playerSession.getBalanceSnapshot(), timestamp, playerSession.getLocations(), Collections.<String>emptySet());
    }

    @Override
    public Session authenticateAndExtendSession(@Routing final BigDecimal playerId,
                                                final String sessionKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void updatePlayerInformation(@Routing final BigDecimal playerId,
                                        final String playerNickname,
                                        final String pictureUrl) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Collection<Session> findAllByPlayer(@Routing final BigDecimal playerId) {
        final Collection<Session> sessions = new HashSet<>();
        for (PlayerSession playerSession : playerSessionRepository.findAllByPlayer(playerId)) {
            sessions.add(asSession(playerSession));
        }

        return sessions;
    }

    @Override
    public void invalidateByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        playerSessionRepository.removeByPlayerAndSessionKey(playerId, sessionKey);
    }

    @Override
    public Session findByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        return asSession(playerSessionRepository.findByPlayerAndSessionKey(playerId, sessionKey));
    }

    @Override
    public Set<PlayerLocations> getGlobalPlayerList() {
        return null;
    }

    @Override
    public Set<PlayerSessionStatus> retrieveAllSessionStatuses() {
        return null;
    }
}
