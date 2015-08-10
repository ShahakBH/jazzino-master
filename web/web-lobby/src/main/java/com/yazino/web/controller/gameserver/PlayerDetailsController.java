package com.yazino.web.controller.gameserver;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import com.yazino.spring.security.AllowPublicAccess;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class PlayerDetailsController {
    private static final int HTTP_INTERNAL_ERROR = 500;

    private final PlayerService playerService;
    private final LobbySessionCache lobbySessionCache;

    private final JsonHelper jsonHelper = new JsonHelper();

    @Autowired
    public PlayerDetailsController(final PlayerService playerService,
                                   final LobbySessionCache lobbySessionCache) {
        notNull(playerService, "playerService is null");
        notNull(lobbySessionCache, "lobbySessionCache is null");

        this.playerService = playerService;
        this.lobbySessionCache = lobbySessionCache;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/details")
    public void details(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        if (activeSession == null) {
            response.sendError(HTTP_INTERNAL_ERROR, "session expired");
            return;
        }

        final String playerIdStr = request.getParameter("playerId");
        if (!StringUtils.isNumeric(playerIdStr)) {
            response.sendError(HTTP_INTERNAL_ERROR, "invalid playerId: " + playerIdStr);
            return;
        }

        final BigDecimal playerId = new BigDecimal(playerIdStr);
        final BasicProfileInformation information = playerService.getBasicProfileInformation(playerId);

        response.setContentType("text/javascript");
        if (information != null) {
            response.getWriter().write(jsonHelper.serialize(information));
        } else {
            response.getWriter().write("{}");
        }
    }

}
