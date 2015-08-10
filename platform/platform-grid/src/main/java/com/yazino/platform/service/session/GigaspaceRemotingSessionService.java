package com.yazino.platform.service.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.GlobalPlayerList;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.session.SessionKeyPersistenceRequest;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.processor.session.PlayerSessionWorker;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.GlobalPlayerListRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.session.transactional.TransactionalSessionService;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import org.joda.time.DateTime;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingSessionService implements SessionService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingSessionService.class);

    private static final int EIGHT_BIT_MASK = 0xFF;

    private final SequenceGenerator sequenceGenerator;
    private final PlayerSessionRepository playerSessionRepository;
    private final TransactionalSessionService transactionalSessionService;
    private final GigaSpace sessionSpace;
    private final InternalWalletService internalWalletService;
    private final GlobalPlayerListRepository globalPlayerListRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public GigaspaceRemotingSessionService(
            final SequenceGenerator sequenceGenerator,
            final PlayerSessionRepository playerSessionRepository,
            final TransactionalSessionService transactionalSessionService,
            final InternalWalletService internalWalletService,
            @Qualifier("gigaSpace") final GigaSpace sessionSpace,
            final GlobalPlayerListRepository globalPlayerListRepository,
            final PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
        notNull(sequenceGenerator, "sequenceGenerator is null");
        notNull(playerSessionRepository, "playerSessionRepository is null");
        notNull(transactionalSessionService, "transactionalSessionService is null");
        notNull(internalWalletService, "internalWalletService may not be null");
        notNull(sessionSpace, "sessionSpace may not be null");
        notNull(globalPlayerListRepository, "globalPlayerListRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");

        this.sequenceGenerator = sequenceGenerator;
        this.playerSessionRepository = playerSessionRepository;
        this.transactionalSessionService = transactionalSessionService;
        this.sessionSpace = sessionSpace;
        this.internalWalletService = internalWalletService;
        this.globalPlayerListRepository = globalPlayerListRepository;
    }

    @Override
    public Session createSession(@Routing("getPlayerId") final BasicProfileInformation player,
                                 final Partner partnerId,
                                 final String referrer,
                                 final String ipAddress,
                                 final String emailAddress,
                                 final Platform platform,
                                 String loginUrl,
                                 final Map<String, Object> clientContext) {

        final BigDecimal sessionId = sequenceGenerator.next();
        final PlayerSession playerSession = new PlayerSession(sessionId, player.getPlayerId(),
                generateSessionKey(sessionId, player, ipAddress, referrer, platform, loginUrl, clientContext),
                player.getPictureUrl(), player.getName(), partnerId, platform, ipAddress, balanceOf(player), emailAddress);

        playerSessionRepository.save(playerSession);

        return convertToSession(playerSession);
    }

    private Session convertToSession(PlayerSession ps) {
        if (ps == null) {
            return null;
        }
        final Player player = playerRepository.findById(ps.getPlayerId());
        return new Session(ps.getSessionId(),
                ps.getPlayerId(),
                ps.getPartnerId(),
                ps.getPlatform(),
                ps.getIpAddress(),
                ps.getLocalSessionKey(),
                ps.getNickname(),
                ps.getEmail(),
                ps.getPictureUrl(),
                ps.getBalanceSnapshot(),
                asDateTime(ps.getTimestamp()),
                ps.getLocations(),
                player.getTags());
    }

    private DateTime asDateTime(final Date timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new DateTime(timestamp);
    }

    private BigDecimal balanceOf(final BasicProfileInformation player) {
        try {
            return internalWalletService.getBalance(player.getAccountId());
        } catch (final WalletServiceException e) {
            LOG.warn("Couldn't retrieve player's {} balance from account {}", player.getPlayerId(), player.getAccountId(), e);
        }
        return null;
    }

    private String generateSessionKey(final BigDecimal sessionId,
                                      final BasicProfileInformation player,
                                      final String ipAddress,
                                      final String referrer,
                                      final Platform platform,
                                      String loginUrl,
                                      final Map<String, Object> clientContext) {
        LOG.debug("entering generateSessionKey {}", player);

        final String sessionKey = generateKey(sessionId);
        writeAuditRecordForSessionKey(sessionId, player, ipAddress, referrer, platform, sessionKey, loginUrl, clientContext);

        LOG.debug("generated session key {} for {}", sessionKey, player.getPlayerId());

        return sessionKey;
    }

    private void writeAuditRecordForSessionKey(final BigDecimal sessionId,
                                               final BasicProfileInformation player,
                                               final String ipAddress,
                                               final String referrer,
                                               final Platform platform,
                                               final String sessionKey,
                                               final String loginUrl,
                                               final Map<String, Object> clientContext) {
        try {
            sessionSpace.write(new SessionKeyPersistenceRequest(
                    new SessionKey(sessionId, player.getAccountId(), player.getPlayerId(),
                            sessionKey, ipAddress, referrer, platform.name(), loginUrl, clientContext)));

        } catch (Exception e) {
            LOG.error("Failed to audit session key creation: profile={}; ipAddress={}; referrer={}; platform={}",
                    player, ipAddress, referrer, platform.name(), e);
        }
    }

    private String generateKey(final BigDecimal sessionId) {
        final String sessionKey = sessionId.toPlainString();

        try {
            final MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(sessionKey.getBytes());

            final StringBuilder hexString = new StringBuilder();
            for (final byte aMessageDigest : algorithm.digest()) {
                hexString.append(Integer.toHexString(EIGHT_BIT_MASK & aMessageDigest));
            }

            return hexString.toString();

        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to load MD5 algorithm", e);
        }
    }

    @Override
    public void invalidateAllByPlayer(@Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        playerSessionRepository.removeAllByPlayer(playerId);
    }

    @Override
    public void invalidateByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        notNull(playerId, "playerId may not be null");
        notNull(sessionKey, "sessionKey may not be null");

        playerSessionRepository.removeByPlayerAndSessionKey(playerId, sessionKey);
    }

    @Override
    public int countSessions(final boolean onlyPlaying) {
        return playerSessionRepository.countPlayerSessions(onlyPlaying);
    }

    @Override
    public PagedData<Session> findSessions(final int page) {
        final PagedData<PlayerSession> playerSessions = playerSessionRepository.findAll(page);
        final List<Session> sessions = new ArrayList<>();
        for (PlayerSession ps : playerSessions.getData()) {
            sessions.add(convertToSession(ps));
        }
        return new PagedData<>(playerSessions.getStartPosition(),
                playerSessions.getSize(),
                playerSessions.getTotalSize(),
                sessions);
    }

    @Override
    public Session authenticateAndExtendSession(@Routing final BigDecimal playerId,
                                                final String sessionKey) {
        notNull(playerId, "playerId is null");
        LOG.debug("authenticateAndExtendLobbySession {} {}", playerId, sessionKey);
        final PlayerSessionWorker worker = new PlayerSessionWorker(playerSessionRepository);
        final PlayerSession ps = worker.authenticate(playerId, sessionKey);
        if (ps == null) {
            return null;
        }
        playerSessionRepository.extendCurrentSession(playerId, sessionKey);
        return convertToSession(ps);
    }

    @Override
    public void updatePlayerInformation(@Routing final BigDecimal playerId,
                                        final String playerNickname,
                                        final String pictureUrl) {
        transactionalSessionService.updatePlayerInformation(playerId, playerNickname, pictureUrl);
    }

    @Override
    public Collection<Session> findAllByPlayer(@Routing final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final Collection<Session> playerSessions = new HashSet<>();
        for (PlayerSession playerSession : playerSessionRepository.findAllByPlayer(playerId)) {
            playerSessions.add(convertToSession(playerSession));
        }
        return playerSessions;
    }

    @Override
    public Session findByPlayerAndSessionKey(@Routing final BigDecimal playerId, final String sessionKey) {
        notNull(playerId, "playerId may not be null");
        notNull(sessionKey, "sessionKey may not be null");

        return convertToSession(playerSessionRepository.findByPlayerAndSessionKey(playerId, sessionKey));
    }

    @Override
    public Set<PlayerLocations> getGlobalPlayerList() {
        final GlobalPlayerList globalPlayerList = globalPlayerListRepository.read();
        return globalPlayerList.currentLocations();
    }

    @Override
    public Set<PlayerSessionStatus> retrieveAllSessionStatuses() {
        return playerSessionRepository.findAllSessionStatuses();
    }

}
