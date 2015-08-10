package com.yazino.web.controller;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.tournament.TournamentDetail;
import com.yazino.platform.tournament.TournamentService;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class TournamentLocatorController extends BaseLocatorController {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentLocatorController.class);

    private static final String NULL_TOURNAMENT_ID = "Null Tournament Id parameter passed to url.";
    private static final String NO_TOURNAMENT_FOUND = "No tournament detail found for tournamentId: %s";

    private final TournamentService tournamentService;
    private final LobbySessionCache lobbySessionCache;
    private final GameConfigurationRepository gameConfigurationRepository;

    @Autowired
    public TournamentLocatorController(final TournamentService tournamentService,
                                       final LobbySessionCache lobbySessionCache,
                                       final GameAvailabilityService gameAvailabilityService,
                                       final GameConfigurationRepository gameConfigurationRepository) {
        super(gameAvailabilityService);

        notNull(tournamentService, "tournamentService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository is null");

        this.tournamentService = tournamentService;
        this.lobbySessionCache = lobbySessionCache;
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @RequestMapping("/tournament/{tournamentId}")
    public ModelAndView launchTournament(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final ModelMap model,
                                         @PathVariable("tournamentId") final String tournamentIdAsString)
            throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            LOG.debug("Session is not present/has expired");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Your session expired. Please log in again.");
            return null;
        }

        final BigDecimal tournamentId = asBigDecimal(tournamentIdAsString);
        if (tournamentId == null) {
            LOG.debug(NULL_TOURNAMENT_ID);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, NULL_TOURNAMENT_ID);
            return null;
        }

        final TournamentDetail detail = getDetailFor(tournamentId);
        if (detail == null) {
            final String msg = String.format(NO_TOURNAMENT_FOUND, tournamentId);
            LOG.error(msg);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            return null;
        }

        populateCommonLauncherProperties(model, lobbySession, detail.getGameType());

        model.put("tournamentId", tournamentId);
        model.put("tournamentName", detail.getName());
        model.put("swfName", "games/" + detail.getGameType().toLowerCase() + "/");
        model.put("title", "TournamentClient");

        final GameConfiguration gameConfiguration = gameConfigurationRepository.find(detail.getGameType());
        model.put("gameConfiguration", gameConfiguration);

        return new ModelAndView("playGame", model);
    }

    private BigDecimal asBigDecimal(final String bigDecimalAsString) {
        if (bigDecimalAsString != null) {
            try {
                return new BigDecimal(bigDecimalAsString);
            } catch (NumberFormatException e) {
                LOG.debug("Failed to convert to big decimal: {}", bigDecimalAsString);
                return null;
            }
        }
        return null;
    }

    private TournamentDetail getDetailFor(final BigDecimal tournamentId) {
        try {
            return tournamentService.findDetailById(tournamentId);
        } catch (Exception e) {
            LOG.debug("Couldn't find detail for tournament {}", tournamentId);
            return null;
        }
    }

}
