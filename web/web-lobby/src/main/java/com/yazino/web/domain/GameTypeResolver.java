package com.yazino.web.domain;

import com.yazino.web.session.LobbySession;
import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

@Service("gameTypeResolver")
public class GameTypeResolver {
    private static final Logger LOG = LoggerFactory.getLogger(GameTypeResolver.class);

    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;

    @Autowired
    public GameTypeResolver(@Qualifier("cookieHelper") final CookieHelper cookieHelper,
                            @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration) {
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
    }

    /**
     * Resolve the game type from the possible sources.
     * <p/>
     * This should only be used by login controllers. All other interested parties should
     * rely on the value set by the common properties interceptor.
     *
     * @param request  the HTTP request.
     * @param response the HTTP response.
     * @return the resolved game type. The default if no other is found.
     */
    public String resolveGameType(final HttpServletRequest request,
                                  final HttpServletResponse response) {
        notNull(request, "request is null");
        String gameType = request.getParameter("gameType");
        LOG.debug("gameType from request={}", gameType);

        if (gameType == null) {
            final String defaultGameType = siteConfiguration.getDefaultGameType();
            LOG.debug("Resolving gameType from cookies, using {} as fallback", defaultGameType);
            gameType = cookieHelper.getLastGameType(request.getCookies(), defaultGameType);

        } else if (gameType.endsWith("/")) {
            gameType = gameType.substring(0, gameType.length() - 1);
        }

        LOG.debug("Resolved gameType={}. Storing for future reference", gameType);
        cookieHelper.setLastGameType(response, gameType);
        return gameType;
    }

    public ApplicationInformation appInfoFor(final HttpServletRequest request,
                                             final HttpServletResponse response,
                                             final LobbySession lobbySession) {
        return new ApplicationInformation(
                resolveGameType(request, response),
                lobbySession.getPartnerId(),
                lobbySession.getPlatform());
    }
}
