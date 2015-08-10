package com.yazino.web.session;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.service.TopUpResultService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.message.TopUpRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.yazino.web.session.PlatformReportingHelper.getRequestUrl;
import static org.apache.commons.lang3.Validate.notNull;

@Service("lobbySessionFactory")
public class LobbySessionFactory {
    private static final Logger LOG = LoggerFactory.getLogger(LobbySessionFactory.class);
    private static final String KNOWN_USER = "knownUser";
    private static final int TWO_WEEKS_IN_SECONDS = 1209600;

    private final SessionFactory sessionFactory;
    private final LobbySessionCache lobbySessionCache;
    private final ReferrerSessionCache referrerSessionCache;
    private final QueuePublishingService<TopUpRequest> topUpRequestQueuePublishingService;
    private final TopUpResultService topUpResultService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public LobbySessionFactory(final SessionFactory sessionFactory,
                               final LobbySessionCache lobbySessionCache,
                               final ReferrerSessionCache referrerSessionCache,
                               @Qualifier("promotionRequestQueuePublishingService")
                               final QueuePublishingService<TopUpRequest> topUpRequestQueuePublishingService,
                               final TopUpResultService topUpResultService,
                               final YazinoConfiguration yazinoConfiguration) {
        notNull(sessionFactory, "sessionFactory may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(referrerSessionCache, "refererSession may not be null");
        notNull(topUpRequestQueuePublishingService, "topUpRequestQueuePublishingService may not be null");
        notNull(topUpResultService, "topUpResultService may not be null");
        notNull(yazinoConfiguration);

        this.yazinoConfiguration = yazinoConfiguration;
        this.sessionFactory = sessionFactory;
        this.lobbySessionCache = lobbySessionCache;
        this.referrerSessionCache = referrerSessionCache;
        this.topUpRequestQueuePublishingService = topUpRequestQueuePublishingService;
        this.topUpResultService = topUpResultService;
    }

    public LobbySession registerAuthenticatedSession(final HttpServletRequest request,
                                                     final HttpServletResponse response,
                                                     final Partner partnerId,
                                                     final BigDecimal playerId,
                                                     final LoginResult loginResult,
                                                     final boolean useSessionCookie,
                                                     final Platform platform,
                                                     final Map<String, Object> clientContext, final String gameType) {
        final String referrer = referrerSessionCache.getReferrer();
        final String ipAddress = resolveIpAddress(request);
        final PartnerSession partnerSession = new PartnerSession(
                referrer, ipAddress, partnerId,
                platform, getRequestUrl(request));

        final LobbySessionCreationResponse creationResponse =
                sessionFactory.registerNewSession(playerId, partnerSession, platform, loginResult, clientContext);
        if (creationResponse == null) {
            return null;
        }

        final LobbySession createdSession = creationResponse.getLobbySession();
        LOG.debug("Created session {}", createdSession);

        final List<Object> disabledGames = yazinoConfiguration.getList(
                "strata.server.lobby.progressive.bonus.disabled.games", new ArrayList<>()); // defaults to none disabled
        if (disabledGames == null || !disabledGames.contains(gameType)) {
            LOG.debug("gametype {} not disabled so sending topup request", gameType);
            publishTopUpRequest(playerId, createdSession.getSessionId(), platform);
        } else {
            LOG.debug("gametype {} disabled so not sending topup request", gameType);
        }


        lobbySessionCache.setLobbySession(createdSession);
        setCookiesInResponse(response, useSessionCookie);

        return createdSession;
    }

    private void publishTopUpRequest(final BigDecimal playerId,
                                     final BigDecimal sessionId,
                                     final Platform platform) {
        topUpResultService.clearTopUpStatus(playerId);
        final TopUpRequest topUpRequest = new TopUpRequest(playerId, platform, new DateTime(), sessionId);
        LOG.debug("publishing top request {}", topUpRequest);
        topUpRequestQueuePublishingService.send(topUpRequest);
    }

    private void setCookiesInResponse(final HttpServletResponse response, final boolean useSessionCookie) {
        notNull(response, "response may not be null");

        final Cookie isKnown = new Cookie(KNOWN_USER, String.valueOf(true));
        isKnown.setMaxAge(TWO_WEEKS_IN_SECONDS * 2);
        response.addCookie(isKnown);
        if (useSessionCookie) {
            final Cookie cookie = lobbySessionCache.generateLocalSessionCookie(null);
            if (cookie != null) {
                response.addCookie(cookie);
            }
        }
    }

    private String resolveIpAddress(final HttpServletRequest request) {
        final String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null) {
            return StringUtils.substringBefore(ipAddress, ",");
        }
        return request.getRemoteAddr();
    }
}
