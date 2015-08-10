package com.yazino.web.controller;

import com.google.common.base.Function;
import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.service.TopUpResultService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class DailyAwardController {
    private static final Logger LOG = LoggerFactory.getLogger(DailyAwardController.class);

    private final LobbySessionCache lobbySessionCache;
    private final TopUpResultService topUpResultService;
    private final TopUpResultViewHelper topUpResultViewHelper;
    private final WalletService walletService;
    private final PlayerService playerService;

    @Autowired
    public DailyAwardController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            final TopUpResultService topUpResultService,
            final TopUpResultViewHelper topUpResultViewHelper,
            final WalletService walletService,
            final PlayerService playerService) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(topUpResultService, "topUpResultService is null");
        notNull(topUpResultViewHelper, "topUpResultViewHelper is null");

        this.lobbySessionCache = lobbySessionCache;
        this.topUpResultService = topUpResultService;
        this.topUpResultViewHelper = topUpResultViewHelper;
        this.walletService = walletService;
        this.playerService = playerService;
    }

    /**
     * @deprecated topping up now happens automatically as part of session creation (see {@link com.yazino.web.session.LobbySessionFactory#registerAuthenticatedSession(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, String, java.math.BigDecimal, com.yazino.platform.player.LoginResult, boolean, com.yazino.platform.Platform, java.util.Map })
     * all mobile apps should be polling for the top up, see {@link #topUpResult(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)} and acknowledging the result vi
     * {@link #acknowledgeTopUp(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, Long)}
     */
    @RequestMapping(value = {"/dailyAward", "/dailyAward/ios", "/dailyAward/android"})
    public void dailyAward(final HttpServletRequest request,
                           final HttpServletResponse response) throws IOException {
        awardDailyTopUpForPlatform(request, response);
    }

    private void awardDailyTopUpForPlatform(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        String dailyAwardAsJson = "{}";

        if (activeSession == null) {
            LOG.info("no active lobby session");
            writeJsonToResponse(response, dailyAwardAsJson);
            return;
        }

        Platform platform = activeSession.getPlatform();
        if (platform == WEB || platform == FACEBOOK_CANVAS) {
            LOG.warn("awardDailyTopUpForPlatform called fo non mobile platform, platform={}", platform);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final BigDecimal playerId = activeSession.getPlayerId();
        DailyAwardResult dailyAwardResult = null;
        try {
            final TopUpResult topUpResult = topUpResultService.getTopUpResult(playerId, platform);
            if (topUpResult.getStatus() == TopUpStatus.CREDITED) {
                topUpResultService.acknowledgeTopUpResult(new TopUpAcknowledgeRequest(playerId, topUpResult.getLastTopUpDate()));
            }
            TopUpResultToDailyAwardTransformer topUpResultTransformer = new TopUpResultToDailyAwardTransformer(playerId, platform);
            dailyAwardResult = topUpResultTransformer.apply(topUpResult);
        } catch (Exception e) {
            LOG.error(String.format("Failed to get top up result for player[%s]", playerId), e);
        }
        if (dailyAwardResult != null) {
            dailyAwardAsJson = new JsonHelper().serialize(dailyAwardResult);
        }
        writeJsonToResponse(response, dailyAwardAsJson);
    }

    private void writeJsonToResponse(final HttpServletResponse response,
                                     final String dailyAwardAsJson) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(dailyAwardAsJson);
        response.flushBuffer();
    }

    private void writeJsonTopUpResultResponse(final HttpServletResponse response,
                                              final TopUpResult topUpResult) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(topUpResultViewHelper.serialiseAsJson(topUpResult));
        response.flushBuffer();
    }

    @RequestMapping("/dailyAward/topUpResult")
    public void topUpResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);

        if (session == null) {
            writeJsonToResponse(response, "{}");
            return;
        }

        final Platform platform = session.getPlatform();
        final DateTime currentDate = new DateTime();
        final BigDecimal playerId = session.getPlayerId();

        final TopUpResult topUpResult = topUpResultService.getTopUpResult(playerId, platform);
        LOG.debug("TopUpResult for player[{}], platform{}], currentDate[{}] is {}", playerId, platform.name(), currentDate, topUpResult);
        writeJsonTopUpResultResponse(response, topUpResult);
    }

    @RequestMapping(value = "dailyAward/topUpAcknowledged", method = RequestMethod.POST)
    public void acknowledgeTopUp(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 @RequestParam(value = "lastTopUpDate", required = false) final Long lastTopUpDate)
            throws IOException {
        if (!hasParameter("lastTopUpDate", lastTopUpDate, request, response)) {
            return;
        }

        final LobbySession session = lobbySessionCache.getActiveSession(request);

        if (session == null) {
            writeJsonToResponse(response, "{\"error\":\"No Session for Player\"}");
            LOG.error("Session does not exist so cannot acknowledge top up for player");
            return;
        }

        final BigDecimal playerId = session.getPlayerId();
        topUpResultService.acknowledgeTopUpResult(new TopUpAcknowledgeRequest(playerId, new DateTime(lastTopUpDate)));
        writeJsonToResponse(response, "{\"acknowledgement\":true}");
    }

    /*
    transforms a TopUpResult into DailyAwardResult. Null is return if status is not CREDITED (i.e. just topped up).
    Note we only transform mobile results as the transformation is only required by deprecated mobile methods
     */
    @Deprecated
    private class TopUpResultToDailyAwardTransformer implements Function<TopUpResult, DailyAwardResult> {
        private final BigDecimal playerId;
        private final Platform platform;

        private TopUpResultToDailyAwardTransformer(final BigDecimal playerId, final Platform platform) {
            this.playerId = playerId;
            this.platform = platform;
        }

        @Override
        public DailyAwardResult apply(TopUpResult topUpResult) {
            if (topUpResult == null || topUpResult.getStatus() != TopUpStatus.CREDITED) {
                return null;
            }
            MobileTopUpResult mobileTopUpResult = (MobileTopUpResult) topUpResult; // safe cast since CREDITED
            DailyAwardResult dailyAwardResult = new DailyAwardResult();
            dailyAwardResult.setConsecutiveDaysPlayed(mobileTopUpResult.getConsecutiveDaysPlayed());
            dailyAwardResult.setTopupAmount(mobileTopUpResult.getTotalTopUpAmount());
            DailyAwardConfig dailyAwardConfig = new DailyAwardConfig();
            dailyAwardResult.setDailyAwardConfig(dailyAwardConfig);
            dailyAwardConfig.setIosImage(mobileTopUpResult.getImageUrl());

            final BigDecimal accountId = playerService.getAccountId(playerId);
            try {
                final BigDecimal balance = walletService.getBalance(accountId);
                dailyAwardResult.setBalance(balance);
            } catch (WalletServiceException e) {
                LOG.warn("failed to get balance in deprecated android dailyAward request. player={}", playerId);
                // IOS app crashes if balance is null
                dailyAwardResult.setBalance(mobileTopUpResult.getTotalTopUpAmount());
            }
            return dailyAwardResult;
        }
    }
}
