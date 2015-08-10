package com.yazino.web.controller;

import com.yazino.platform.community.PlayerTrophyService;
import com.yazino.platform.community.TrophyCabinet;
import com.yazino.platform.community.TrophySummary;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.web.domain.GameTypeMapper;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.achievement.AchievementCabinetBuilder;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class AchievementController {
    private static final Logger LOG = LoggerFactory.getLogger(AchievementController.class);

    private final LobbySessionCache lobbySessionCache;
    private final AchievementCabinetBuilder achievementCabinetBuilder;
    private final PlayerTrophyService playerTrophyService;
    private final SiteConfiguration siteConfiguration;
    private final GameConfigurationRepository gameConfigurationRepository;

    private final GameTypeMapper gameTypeMapper = new GameTypeMapper();
    private String trophyName = "";
    private String medalNames = "";

    @Autowired(required = true)
    public AchievementController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                                 final PlayerTrophyService playerTrophyService,
                                 final AchievementCabinetBuilder achievementCabinetBuilder,
                                 final SiteConfiguration siteConfiguration,
                                 final GameConfigurationRepository gameConfigurationRepository) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(achievementCabinetBuilder, "achievementCabinetBuilder is null");
        notNull(playerTrophyService, "playerTrophyService is null");
        notNull(siteConfiguration, "siteConfiguration is null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository is null");

        this.lobbySessionCache = lobbySessionCache;
        this.achievementCabinetBuilder = achievementCabinetBuilder;
        this.playerTrophyService = playerTrophyService;
        this.siteConfiguration = siteConfiguration;
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @RequestMapping("/achievements/{gameTypeViewName}")
    public String achievementsForGame(final HttpServletRequest request,
                                      final ModelMap modelMap,
                                      @PathVariable("gameTypeViewName") final String gameTypeViewName) {
        return achievements(request, gameTypeMapper.fromViewName(gameTypeViewName), modelMap);
    }

    @RequestMapping({"/achievements", "/lobby/achievements"})
    public String achievements(final HttpServletRequest request,
                               @RequestParam(value = "gameType", required = false) final String requestGameType,
                               final ModelMap modelMap) {

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new RuntimeException("No lobby session found");
        }

        String gameType = requestGameType;
        if (gameType == null) {
            gameType = siteConfiguration.getDefaultGameType();
        }

        modelMap.addAttribute("gameType", gameType);
        modelMap.addAttribute("levelData", achievementCabinetBuilder.buildAchievementCabinet(
                lobbySession.getPlayerId(), lobbySession.getPlayerName(), gameType));

        final GameConfiguration gameConfiguration = gameConfigurationRepository.find(gameType);
        modelMap.addAttribute("gameConfiguration", gameConfiguration);

        addTrophyInformationToModel(gameType, modelMap, lobbySession.getPlayerId());

        return "achievements-overlay";
    }

    private void addTrophyInformationToModel(final String gameType,
                                             final ModelMap modelMap,
                                             final BigDecimal playerId) {
        final List<String> trophyNames = buildTrophyNames(this.trophyName);

        final TrophyCabinet trophyCabinet = getPlayerTrophyCabinet(gameType, playerId, trophyNames);
        LOG.debug("Game: {}, Player {} had trophy cabinet {}", gameType, playerId, trophySummariesFrom(trophyCabinet));

        final List<String> trophyMedalNames = buildTrophyNames(this.medalNames);

        final TrophyCabinet medalCabinet = getPlayerTrophyCabinet(gameType, playerId, trophyMedalNames);
        LOG.debug("Game: {}, Player {} had medal cabinet {}", gameType, playerId, trophySummariesFrom(medalCabinet));

        modelMap.addAttribute("trophyCabinet", trophyCabinet);
        modelMap.addAttribute("medalCabinet", medalCabinet);
    }

    private Map<String, TrophySummary> trophySummariesFrom(final TrophyCabinet trophyCabinet) {
        if (trophyCabinet != null) {
            return trophyCabinet.getTrophySummaries();
        }
        return Collections.emptyMap();
    }

    private TrophyCabinet getPlayerTrophyCabinet(final String gameType,
                                                 final BigDecimal playerId,
                                                 final Collection<String> trophyNames) {
        notNull(gameType, "gameType must not be null");
        notNull(playerId, "playerId must not be null");
        notNull(trophyNames, "trophyNames must not be null");

        try {
            return playerTrophyService.findTrophyCabinetForPlayer(gameType, playerId, trophyNames);
        } catch (Exception e) {
            LOG.error("Couldn't find trophy cabinet for player: " + e.getMessage(), e);
            return null;
        }
    }

    @Value("${strata.server.lobby.trophy}")
    public void setTrophyName(final String trophyName) {
        this.trophyName = trophyName;
    }

    @Value("${strata.server.lobby.medals}")
    public void setMedalNames(final String medalNames) {
        this.medalNames = medalNames;
    }

    private static List<String> buildTrophyNames(final String namesCsv) {
        final List<String> result = new ArrayList<String>();
        final String[] names = namesCsv.split(",");
        for (String name : names) {
            result.add(name.trim());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Trophy names: " + result);
        }
        return result;
    }

}
