package com.yazino.web.interceptor;

import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.domain.ProfileInformationRepository;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

public class ProfileInformationInterceptor extends HandlerInterceptorAdapter {
    private final LobbySessionCache lobbySessionCache;
    private final GameTypeResolver gameTypeResolver;
    private final ProfileInformationRepository profileInformationRepository;

    @Autowired
    public ProfileInformationInterceptor(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("gameTypeResolver") final GameTypeResolver gameTypeResolver,
            @Qualifier("profileInformationRepository") final ProfileInformationRepository profileInformationRepository) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(gameTypeResolver, "gameTypeResolver may not be null");
        notNull(profileInformationRepository, "profileInformationRepository may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.gameTypeResolver = gameTypeResolver;
        this.profileInformationRepository = profileInformationRepository;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        if (modelAndView != null) {
            final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
            if (lobbySession != null) {
                String gameType = (String) modelAndView.getModel().get("gameType");
                if (gameType == null) {
                    // Ideally we'd rely on the value set by the common properties interceptor;
                    // however, Spring doesn't guarantee order of the interceptors so we can't.
                    gameType = gameTypeResolver.resolveGameType(request, response);
                }

                modelAndView.addObject("profileInformation",
                        profileInformationRepository.getProfileInformation(lobbySession.getPlayerId(), gameType));
            }
        }
        super.postHandle(request, response, handler, modelAndView);
    }
}
