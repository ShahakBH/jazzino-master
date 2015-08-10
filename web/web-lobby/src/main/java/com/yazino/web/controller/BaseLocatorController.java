package com.yazino.web.controller;

import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.session.LobbySession;
import org.springframework.ui.ModelMap;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class BaseLocatorController {
    private final GameAvailabilityService gameAvailabilityService;

    protected BaseLocatorController(final GameAvailabilityService gameAvailabilityService) {
        notNull(gameAvailabilityService, "gameAvailabilityServiceService may not be null");

        this.gameAvailabilityService = gameAvailabilityService;
    }

    protected void populateCommonLauncherProperties(final ModelMap model,
                                                    final LobbySession lobbySession,
                                                    final String gameType) {
        model.put("playerId", lobbySession.getPlayerId());
        model.put("playerName", lobbySession.getPlayerName());
        model.put("gameType", gameType);

        GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(gameType);
        if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
            model.put("countdown", availability.getMaintenanceStartsAtMillis());
        }
    }
}
