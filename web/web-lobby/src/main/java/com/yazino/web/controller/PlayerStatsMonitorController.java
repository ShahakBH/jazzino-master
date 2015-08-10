package com.yazino.web.controller;

import com.yazino.web.service.PlayerStatsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This is a monitor servlet to expose a HTTP endpoint that does a synchronous remote
 * call to the player stats worker.
 * <p/>
 * This allows us to test response times for player stats from lobby boxes.
 */
@Controller
public class PlayerStatsMonitorController {

    private final PlayerStatsService playerStatsService;

    @Autowired
    public PlayerStatsMonitorController(final PlayerStatsService playerStatsService) {
        notNull(playerStatsService, "playerStatsService may not be null");

        this.playerStatsService = playerStatsService;
    }

    @RequestMapping("/command/player-stats-monitor/{gameType}")
    public void playerStatsMonitor(@PathVariable final String gameType,
                                   final HttpServletResponse response)
            throws IOException {
        if (StringUtils.isBlank(gameType)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            playerStatsService.getAchievementDetails(gameType);
            response.getWriter().write("{\"status\":\"okay\"}");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"status\":\"error\"}");
        }
    }
}
