package com.yazino.web.interceptor;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.web.controller.TopUpResultViewHelper;
import com.yazino.web.service.TopUpResultService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import strata.server.lobby.api.promotion.TopUpResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

public class DailyAwardInterceptor extends HandlerInterceptorAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DailyAwardInterceptor.class);

    private static final String TOP_UP_RESULT = "topUpResult";

    private final LobbySessionCache lobbySessionCache;
    private final YazinoConfiguration yazinoConfiguration;
    private final TopUpResultService topUpResultService;
    private final TopUpResultViewHelper topUpResultViewHelper;

    @Autowired
    public DailyAwardInterceptor(final LobbySessionCache lobbySessionCache,
                                 final YazinoConfiguration yazinoConfiguration,
                                 final TopUpResultService topUpResultService,
                                 final TopUpResultViewHelper topUpResultViewHelper) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(topUpResultService, "topUpResultService may not be null");
        notNull(topUpResultViewHelper, "topUpResultViewHelper may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.topUpResultService = topUpResultService;
        this.lobbySessionCache = lobbySessionCache;
        this.topUpResultViewHelper = topUpResultViewHelper;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler,
                           final ModelAndView modelAndView)
            throws Exception {
        if (modelAndView != null) {
            addTopUpResult(request, modelAndView);
        }

        super.postHandle(request, response, handler, modelAndView);
    }

    /*
     * new style serverside topup places topUpResult into js yazino config object
     */
    private void addTopUpResult(HttpServletRequest request, ModelAndView modelAndView) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);

        if (lobbySession != null) {
            final TopUpResult topUpResult = topUpResultService.getTopUpResult(lobbySession.getPlayerId(),
                    lobbySession.getPlatform());
            final String resultAsJson = topUpResultViewHelper.serialiseAsJson(topUpResult);
            LOG.debug("Adding top up result to model for player {}:{}", lobbySession.getPlayerId(), resultAsJson);
            modelAndView.addObject(TOP_UP_RESULT, resultAsJson);
        } else {
            modelAndView.addObject(TOP_UP_RESULT, "{}");
        }
    }
}
