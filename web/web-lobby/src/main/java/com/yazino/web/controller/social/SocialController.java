package com.yazino.web.controller.social;

import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.social.PlayerInformation;
import com.yazino.web.domain.social.PlayerInformationType;
import com.yazino.web.domain.social.PlayersInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/social")
public class SocialController {
    private static final Logger LOG = LoggerFactory.getLogger(SocialController.class);

    private final LobbySessionCache lobbySessionCache;
    private final PlayerService playerService;
    private final PlayersInformationService playersInformationService;
    private final JsonHelper jsonHelper = new JsonHelper();

    @Autowired
    public SocialController(final LobbySessionCache lobbySessionCache,
                            final PlayerService playerService,
                            final PlayersInformationService playersInformationService) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(playersInformationService, "playersInfoService may not be null");

        this.playersInformationService = playersInformationService;
        this.lobbySessionCache = lobbySessionCache;
        this.playerService = playerService;
    }

    @RequestMapping("/friendsSummary")
    public void publishSummary(final HttpServletRequest request,
                               final HttpServletResponse response) {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            writeResult(response, "no session");
        } else {
            playerService.asyncPublishFriendsSummary(session.getPlayerId());
            writeResult(response, "ok");
        }
    }

    @RequestMapping("/players")
    public void players(final HttpServletRequest request,
                        final HttpServletResponse response) {
        if (lobbySessionCache.getActiveSession(request) == null) {
            writeResult(response, "no session");
            return;
        }
        final List<BigDecimal> playerIds;
        try {
            playerIds = parsePlayerIds(request);
        } catch (Exception e) {
            writeResult(response, "invalid player ids");
            return;
        }
        final PlayerInformationType[] types = parseInformationTypes(request);
        final String gameType = request.getParameter("gameType");
        final List<PlayerInformation> infos = playersInformationService.retrieve(playerIds,
                gameType,
                types);
        writeSuccessResult(response, jsonHelper.serialize(infos));
    }

    private PlayerInformationType[] parseInformationTypes(final HttpServletRequest request) {
        final List<PlayerInformationType> types = new ArrayList<PlayerInformationType>();
        final String fieldsParameter = request.getParameter("details");
        if (!StringUtils.isBlank(fieldsParameter)) {
            final String[] fields = fieldsParameter.split(",");
            for (String field : fields) {
                try {
                    types.add(PlayerInformationType.valueOf(field.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    //ignore
                }
            }
        }
        return types.toArray(new PlayerInformationType[types.size()]);
    }

    private List<BigDecimal> parsePlayerIds(final HttpServletRequest request) {
        final String[] playerIdsStr = request.getParameter("playerIds").split(",");
        final List<BigDecimal> playerIds = new ArrayList<BigDecimal>();
        for (String playerIdStr : playerIdsStr) {
            playerIds.add(new BigDecimal(playerIdStr.trim()));
        }
        return playerIds;
    }

    private void writeSuccessResult(final HttpServletResponse response, final String players) {
        writeJson(response, String.format("{\"result\":\"ok\",\"players\":%s}", players));
    }

    private void writeJson(final HttpServletResponse response, final String result) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write(result);

        } catch (IOException e) {
            markResponseAsError(response, e);
        }
    }

    private void writeResult(final HttpServletResponse response, final String result) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            final String json = String.format("{\"result\":\"%s\"}", result);
            response.getWriter().write(json);

        } catch (IOException e) {
            markResponseAsError(response, e);
        }
    }

    private void markResponseAsError(final HttpServletResponse response, final IOException e) {
        LOG.error("Failed to request friendsSummary", e);
        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException e1) {
            //ignore
        }
    }

}
