package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.table.CountdownService;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.TableService;
import com.yazino.platform.table.TableSummary;
import com.yazino.web.data.AchievementInfoRepository;
import com.yazino.web.data.LevelInfoRepository;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.domain.TableLobbyDetails;
import com.yazino.web.domain.TournamentLobbyCache;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.LandingUrlRegistry;
import com.yazino.web.util.MobilePlatformSniffer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class GameController {
    private static final Logger LOG = LoggerFactory.getLogger(GameController.class);

    private static final String GAME_VIEW = "games";
    private static final int HTTP_BAD_REQUEST = 400;

    private final TableService tableService;
    private final TournamentLobbyCache tournamentLobbyCache;
    private final LobbySessionCache lobbySessionCache;
    private final WalletService walletService;
    private final PlayerService playerService;
    private final LobbyInformationService lobbyInformationService;
    private final LevelInfoRepository levelInfoRepository;
    private final AchievementInfoRepository achievementInfoRepository;
    private final CommunityService communityService;
    private final GameConfigurationRepository gameConfigurationRepository;
    private final CountdownService countdownService;
    private final YazinoConfiguration yazinoConfiguration;
    private final MobilePlatformSniffer mobilePlatformSniffer;
    private final LandingUrlRegistry landingUrlRegistry;

    @Autowired(required = true)
    public GameController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("tableService") final TableService tableService,
            @Qualifier("gameTypeResolver") final GameTypeResolver gameTypeResolver,
            @Qualifier("lobbyInformationService") final LobbyInformationService lobbyInformationService,
            @Qualifier("tournamentLobbyCache") final TournamentLobbyCache tournamentLobbyCache,
            @Qualifier("levelInfoRepository") final LevelInfoRepository levelInfoRepository,
            @Qualifier("achievementInfoRepository") final AchievementInfoRepository achievementInfoRepository,
            final CommunityService communityService,
            final PlayerService playerService,
            final WalletService walletService,
            final GameConfigurationRepository gameConfigurationRepository,
            final CountdownService countdownService,
            final MobilePlatformSniffer mobilePlatformSniffer,
            final LandingUrlRegistry landingUrlRegistry,
            final YazinoConfiguration yazinoConfiguration) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(tableService, "tableService is null");
        notNull(gameTypeResolver, "gameTypeResolver is null");
        notNull(lobbyInformationService, "lobbyInformationService is null");
        notNull(tournamentLobbyCache, "tournamentLobbyCache is null");
        notNull(levelInfoRepository, "levelInfoRepository is null");
        notNull(achievementInfoRepository, "achievementInfoRepository is null");
        notNull(communityService, "communityService is null");
        notNull(playerService, "playerService is null");
        notNull(walletService, "walletService is null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository is null");
        notNull(countdownService, "countdownService is null");
        notNull(mobilePlatformSniffer, "platformSniffer is null");
        notNull(landingUrlRegistry, "gamePagePlatformSpecificRedirector is null");
        notNull(yazinoConfiguration, "yazinoConfiguration is null");

        this.lobbySessionCache = lobbySessionCache;
        this.tableService = tableService;
        this.lobbyInformationService = lobbyInformationService;
        this.tournamentLobbyCache = tournamentLobbyCache;
        this.levelInfoRepository = levelInfoRepository;
        this.achievementInfoRepository = achievementInfoRepository;
        this.communityService = communityService;
        this.playerService = playerService;
        this.walletService = walletService;
        this.gameConfigurationRepository = gameConfigurationRepository;
        this.countdownService = countdownService;
        this.mobilePlatformSniffer = mobilePlatformSniffer;
        this.landingUrlRegistry = landingUrlRegistry;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @RequestMapping({"/{gameType}", "/{gameType}/play", "/public/home{gameType}"})
    public ModelAndView gameLobby(@PathVariable("gameType") final String gameTypeOrAlias,
                                  final HttpServletRequest request,
                                  final HttpServletResponse response) {
        return handleGame(request, response, gameTypeOrAlias);
    }

    private ModelAndView handleGame(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final String gameTypeOrAlias) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);

        GameConfiguration gameConfiguration = null;
        if (gameTypeOrAlias != null) {
            gameConfiguration = gameConfigurationRepository.find(gameTypeOrAlias);
        }

        if (gameConfiguration != null) {
            String landingUrl = customLandingUrlForPlatformAndGame(request, gameConfiguration);
            if (landingUrl != null) {
                return externalRedirect(landingUrl);
            }
        }

        if (lobbySession == null) {
            if (gameTypeOrAlias == null || gameConfiguration == null) {
                return internalRedirect("/");
            }
            final String viewPrefix = yazinoConfiguration.getString(
                    String.format("lobby.game.%s.splash.view-prefix", gameConfiguration.getGameId()), "home");
            return new ModelAndView(viewPrefix + gameConfiguration.getGameId());
        }

        if (gameConfiguration == null) {
            LOG.warn("Game configuration is not present for game type or alias {}", gameTypeOrAlias);
            sendNotFoundStatus(response);
            return null;
        }

        final String gameType = gameConfiguration.getGameId();
        final BigDecimal playerId = lobbySession.getPlayerId();

        final BigDecimal balance = balanceForPlayer(playerId);
        if (balance == null) {
            throw new RuntimeException("Balance couldn't be retrieved for player " + playerId);
        }

        ModelMap modelMap = new ModelMap();
        modelMap.addAttribute("gameType", gameType);
        modelMap.addAttribute("playerBalance", balance);
        modelMap.addAttribute("systemMessage", communityService.getLatestSystemMessage());
        modelMap.addAttribute("privateTableId", findTableByGameTypeAndOwnerId(gameType, playerId));
        modelMap.addAttribute("tournament", tournamentLobbyCache.getNextTournament(playerId, gameType));
        modelMap.addAttribute("lobbyInformation", lobbyInformationService.getLobbyInformation(gameType));
        modelMap.addAttribute("tableDetails",
                TableLobbyDetails.transform(tableService.findAllTablesOwnedByPlayer(playerId)));
        modelMap.addAttribute("levelInfo", levelInfoRepository.getLevelInfo(playerId, gameType));
        modelMap.addAttribute("achievementInfo", achievementInfoRepository.getAchievementInfo(playerId, gameType));
        modelMap.addAttribute("gameConfiguration", gameConfiguration);

        if (StringUtils.equals(gameConfiguration.getProperty("usesFlashLobby"), "true")) {
            addCountdownToModel(modelMap, gameType);
        }

        return new ModelAndView(GAME_VIEW, modelMap);
    }

    private ModelAndView internalRedirect(String url) {
        return new ModelAndView(new RedirectView(url, true, true, false));
    }

    private ModelAndView externalRedirect(String customHomeUrl) {
        return new ModelAndView(new RedirectView(customHomeUrl, false, true, false));
    }

    private String customLandingUrlForPlatformAndGame(HttpServletRequest request, GameConfiguration gameConfiguration) {
        MobilePlatform mobilePlatform = mobilePlatformSniffer.inferPlatform(request);
        if (mobilePlatform == null) {
            return null;
        } else {
            return landingUrlRegistry.getLandingUrlForGameAndPlatform(gameConfiguration.getGameId(), mobilePlatform);
        }
    }

    private void sendNotFoundStatus(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (Exception e) {
            // ignored
        }
    }

    private void addCountdownToModel(final ModelMap modelMap, final String gameType) {
        final Map<String, Long> countdowns = countdownService.findAll();
        for (String countdownId : countdowns.keySet()) {
            if ("ALL".equals(countdownId) || gameType.equalsIgnoreCase(countdownId)) {
                modelMap.addAttribute("countdown", countdowns.get(countdownId));
                return;
            }
        }
    }

    private BigDecimal balanceForPlayer(final BigDecimal playerId) {
        try {
            return walletService.getBalance(playerService.getAccountId(playerId));
        } catch (final WalletServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @RequestMapping("/lobby/gameItem")
    public ModelAndView gameItem(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 @RequestParam(value = "gameType", required = false) final String gameType)
            throws IOException {
        if (isBlank(gameType)) {
            LOG.warn("GameType is not present for /gameItem request");
            response.sendError(HTTP_BAD_REQUEST);
            return null;
        }

        ModelAndView gameModelAndView = handleGame(request, response, gameType);
        return new ModelAndView("partials/game" + gameType, gameModelAndView.getModel());
    }

    @RequestMapping({"/welcome", "/lobby/welcome", "/games", "/lobby/games", "/game", "/lobby/game", "/public/homeGames"})
    public ModelAndView welcome() {
        return internalRedirect("/");
    }

    @RequestMapping("/welcome/{gameType}")
    public ModelAndView welcomeToGameType(@PathVariable("gameType") final String gameType) {
        return internalRedirect("/");
    }

    public BigDecimal findTableByGameTypeAndOwnerId(final String gameType,
                                                    final BigDecimal ownerId) {
        final TableSummary table = tableService.findTableByGameTypeAndPlayerId(gameType, ownerId);
        if (table != null) {
            return table.getId();
        }
        return null;
    }
}
