package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import strata.server.lobby.api.promotion.InGameMessage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class BuyChipsController {
    private static final Logger LOG = LoggerFactory.getLogger(BuyChipsController.class);

    private final LobbySessionCache lobbySessionCache;
    private final BuyChipsPromotionService buyChipsPromotionService;

    @Autowired
    public BuyChipsController(
            @Qualifier("safeBuyChipsPromotionService") final BuyChipsPromotionService buyChipsPromotionService,
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache) {
        notNull(buyChipsPromotionService, "promotionService is null");
        notNull(lobbySessionCache, "lobbySessionCache is null");
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.lobbySessionCache = lobbySessionCache;
    }

    @RequestMapping("/buyChips/inGameMessage")
    public void provideInGameMessage(final HttpServletRequest request,
                                     final HttpServletResponse response) throws IOException {
        String inGameMessageAsJson = "{}";
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        if (activeSession == null) {
            LOG.info("no active lobby session");
        } else {
            final BigDecimal playerId = activeSession.getPlayerId();
            final Platform platform = activeSession.getPlatform();

            LOG.debug("requesting buy chips promotional message for player: {}, platform: {}", playerId, platform.name());
            InGameMessage inGameMessage = null;
            try {
                inGameMessage = buyChipsPromotionService.getInGameMessageFor(playerId, platform);
            } catch (Exception e) {
                LOG.error("Failed to get buy chips promotional message for player[{}]", playerId, e);
            }
            if (inGameMessage != null) {
                inGameMessageAsJson = new JsonHelper().serialize(inGameMessage);
            }

            LOG.debug("buy chips promotional message for player[{}]: {}", playerId, inGameMessageAsJson);
        }

        writeTo(response, inGameMessageAsJson);
    }

    private void writeTo(final HttpServletResponse response, final String jsonMessage) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write(jsonMessage);
            response.flushBuffer();
        } catch (IOException e) {
            LOG.warn("Failed to write response: {}", e.getMessage());
        }
    }
}
