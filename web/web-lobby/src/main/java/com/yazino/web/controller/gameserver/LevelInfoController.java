package com.yazino.web.controller.gameserver;

import com.yazino.platform.playerstatistic.service.LevelInfo;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.data.LevelInfoRepository;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class LevelInfoController {

    private final LevelInfoRepository levelInfoRepository;
    private final LobbySessionCache lobbySessionCache;
    private final JsonHelper jsonHelper = new JsonHelper();

    @Autowired
    public LevelInfoController(final LobbySessionCache lobbySessionCache,
                               final LevelInfoRepository levelInfoRepository) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(levelInfoRepository, "levelInfoRepository is null");
        this.lobbySessionCache = lobbySessionCache;
        this.levelInfoRepository = levelInfoRepository;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/levelInfo")
    public void getLevelInfo(@RequestParam(value = "gameType", required = true) final String gameType,
                             final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "session expired");
            return;
        }
        final LevelInfo levelInfo = levelInfoRepository.getLevelInfo(session.getPlayerId(), gameType);
        if (levelInfo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "level info not found");
            return;
        }
        final Map<String, Object> body = new HashMap<String, Object>();
        body.put("gameType", gameType);
        body.put("level", levelInfo.getLevel());
        body.put("points", levelInfo.getPoints());
        body.put("toNextLevel", levelInfo.getToNextLevel());
        final String json = jsonHelper.serialize(body);
        response.setContentType("text/javascript");
        response.getWriter().write(json);
    }
}
