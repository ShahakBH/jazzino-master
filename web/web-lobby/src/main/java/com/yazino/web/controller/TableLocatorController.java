package com.yazino.web.controller;

import com.yazino.platform.table.TableService;
import com.yazino.platform.table.TableSummary;
import com.yazino.web.domain.GameTypeMapper;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.form.TableLocatorForm;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.GameConfigurationRepository;
import com.yazino.web.service.TableLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.apache.commons.lang3.Validate.notNull;
import static org.tuckey.web.filters.urlrewrite.utils.StringUtils.isBlank;

@Controller
public class TableLocatorController extends BaseLocatorController {
    private static final Logger LOG = LoggerFactory.getLogger(TableLocatorController.class);

    private static final String TABLE_LOCATOR_FORM = "tableLocatorForm";
    private static final String REASON = "reason";
    private static final String NO_TABLE_FOUND = "No table detail found for tableId: %s";
    private static final String SESSION_EXPIRED = "Your session expired.  Please log in again.";

    private final GameTypeMapper gameTypeMapper = new GameTypeMapper();

    private final TableLobbyService tableLobbyService;
    private final SiteConfiguration siteConfiguration;
    private final LobbySessionCache lobbySessionCache;
    private final CookieHelper cookieHelper;
    private final TableService tableService;
    private final GameConfigurationRepository gameConfigurationRepository;

    @Autowired(required = true)
    public TableLocatorController(
            @Qualifier("tableLobbyService") final TableLobbyService tableLobbyService,
            @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            final TableService tableService,
            final CookieHelper cookieHelper,
            final GameAvailabilityService gameAvailabilityService,
            final GameConfigurationRepository gameConfigurationRepository) {
        super(gameAvailabilityService);

        notNull(lobbySessionCache, "Lobby Session Cache may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(tableService, "tableService may not be null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.tableLobbyService = tableLobbyService;
        this.siteConfiguration = siteConfiguration;
        this.cookieHelper = cookieHelper;
        this.tableService = tableService;
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @RequestMapping("/table/find/{gameType}/{variationName}/{clientId}")
    public String findTableFromURI(final ModelMap modelMap,
                                   final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   @PathVariable("gameType") final String gameType,
                                   @PathVariable("variationName") final String variationName,
                                   @PathVariable("clientId") final String clientId) {
        if (gameType == null || variationName == null || clientId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final TableLocatorForm form = new TableLocatorForm();
        form.setGameType(gameType);
        form.setVariationName(variationName.replaceAll("_", " "));
        form.setClientId(clientId.replaceAll("_", " "));

        return createTableWithParameters(form, modelMap, request, response);
    }

    @RequestMapping("/table/find/{gameType}")
    public String findTableForGameTypeFromURI(final ModelMap modelMap,
                                              final HttpServletRequest request,
                                              final HttpServletResponse response,
                                              @PathVariable("gameType") final String gameType) {
        if (gameType == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final TableLocatorForm form = new TableLocatorForm();
        form.setGameType(gameType);

        return createTableWithParameters(form, modelMap, request, response);
    }

    @RequestMapping({"/lobby/tableLocator", "/tableLocator", "/table/find"})
    public String findTableId(@ModelAttribute(TABLE_LOCATOR_FORM) final TableLocatorForm form,
                              final ModelMap modelMap,
                              final HttpServletRequest request,
                              final HttpServletResponse response) {
        return createTableWithParameters(form, modelMap, request, response);
    }

    @RequestMapping("/lobby/tableSimilar")
    public String findSimilarTableId(@RequestParam(value = "tableId", required = false) final String tableIdAsString,
                                     final ModelMap modelMap,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response) {
        if (!hasParameter("tableId", tableIdAsString, request, response)) {
            return null;
        }

        final BigDecimal tableId = parseTableId(tableIdAsString, response);
        if (tableId == null) {
            return null;
        }

        return launchTableSimilarTo(tableId, modelMap, request, response);
    }

    private BigDecimal parseTableId(final String tableIdAsString,
                                    final HttpServletResponse response) {
        try {
            return new BigDecimal(tableIdAsString);

        } catch (NumberFormatException e) {
            LOG.warn("Invalid table ID received: {}", tableIdAsString);
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IOException e1) {
                // ignored
            }
            return null;
        }
    }

    @RequestMapping("/table/like/{tableId}")
    public String findSimilarTableIdInURI(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          final ModelMap modelMap,
                                          @PathVariable("tableId") final String tableIdAsString) {
        if (!hasParameter("tableId", tableIdAsString, request, response)) {
            return null;
        }

        final BigDecimal tableId = parseTableId(tableIdAsString, response);
        if (tableId == null) {
            return null;
        }

        return launchTableSimilarTo(tableId, modelMap, request, response);
    }

    @RequestMapping("/lobby/tableLauncher")
    public String launchTableWithId(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final ModelMap modelMap,
                                    @RequestParam(value = "tableId", required = false) final String tableIdAsString) {
        if (!hasParameter("tableId", tableIdAsString, request, response)) {
            return null;
        }

        final BigDecimal tableId = parseTableId(tableIdAsString, response);
        if (tableId == null) {
            return null;
        }

        return launchTableById(request, response, modelMap, tableId);
    }

    @RequestMapping("/table/{tableId}")
    public String launchTableWithIdInURI(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final ModelMap modelMap,
                                         @PathVariable("tableId") final String tableIdAsString)
            throws IOException {
        if (tableIdAsString == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final BigDecimal tableId = parseTableId(tableIdAsString, response);
        if (tableId == null) {
            return null;
        }

        return launchTableById(request, response, modelMap, tableId);
    }

    private String createTableWithParameters(final TableLocatorForm form,
                                             final ModelMap modelMap,
                                             final HttpServletRequest request,
                                             final HttpServletResponse response) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        final String error = form.validate();
        if (isBlank(error)) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("TableLocatorForm {}", ReflectionToStringBuilder.reflectionToString(form));
                }

                if (!tableLobbyService.isGameTypeAvailable(form.getGameType())) {
                    return warnUserGameIsDisabled(request, response, form.getGameType());

                } else {
                    final TableSummary tableDetails = tableLobbyService.findOrCreateTableByGameTypeAndVariation(
                            form.getGameType(), form.getVariationName(),
                            form.getClientId(), lobbySession.getPlayerId(), Collections.<String>emptySet());
                    return launchTable(tableDetails, request, response, modelMap);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to find table for game type: [{}] client: [{}] variation: [{}]",
                        form.getGameType(), form.getClientId(), form.getVariationName(), e);
                modelMap.addAttribute(REASON, e.getMessage());
            }
        } else {
            modelMap.addAttribute(REASON, error);
        }
        return "tableLocation";
    }

    private void sendError(final HttpServletResponse response, final int errorCode) {
        try {
            response.sendError(errorCode);
        } catch (Exception e) {
            // ignore
        }
    }

    private String launchTableSimilarTo(final BigDecimal tableId,
                                        final ModelMap modelMap,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        try {
            final TableSummary newSummary = tableLobbyService.findOrCreateSimilarTable(
                    tableId, lobbySession.getPlayerId());
            return launchTable(newSummary, request, response, modelMap);

        } catch (Exception e) {
            LOG.error("A similar table could not be created for table {} and player {}", tableId, lobbySession.getPlayerId(), e);
            modelMap.addAttribute(REASON, e.getMessage());
            return "tableLocation";
        }
    }

    private String launchTableById(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final ModelMap modelMap,
                                   final BigDecimal tableId) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        try {
            final TableSummary tableSummary = tableService.findSummaryById(tableId);
            if (tableSummary == null) {
                final String msg = String.format(NO_TABLE_FOUND, tableId);
                LOG.warn(msg);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg);
                return null;
            }

            return launchTable(tableSummary, request, response, modelMap);

        } catch (Exception e) {
            LOG.warn("Failed to launch table {} for player {}", tableId, lobbySession.getPlayerId(), e);
            modelMap.addAttribute(REASON, e.getMessage());
        }
        return "tableLocation";
    }

    private String warnUserGameIsDisabled(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          final String gameType)
            throws IOException {
        if (cookieHelper.isOnCanvas(request, response)) {
            response.sendRedirect("/disabled/" + gameType);
            return null;
        }

        return "redirect:/" + gameTypeMapper.getViewName(gameType);
    }

    private String launchTable(final TableSummary tableSummary,
                               final HttpServletRequest request,
                               final HttpServletResponse response,
                               final ModelMap model) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            LOG.debug("Session expired for active user.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, SESSION_EXPIRED);
            return null;
        }

        populateCommonLauncherProperties(model, lobbySession, tableSummary.getGameType().getId());

        String clientFileName = tableSummary.getClientFile();
        if (clientFileName == null) {
            clientFileName = "default";
        }

        model.put("tableName", tableSummary.getName());
        model.put("tableId", tableSummary.getId());
        model.put("tableVariation", tableSummary.getTemplateName());
        model.put("swfName", "games/" + tableSummary.getGameType().getId().toLowerCase() + "/" + clientFileName);
        model.put("gameConfiguration", gameConfigurationRepository.find(tableSummary.getGameType().getId()));
        if (tableSummary.getTemplateName().contains(" Fast ")) {
            model.put("speedName", "FAST");
        } else {
            model.put("speedName", "NORMAL");
        }
        model.put("title", "GameClient");

        return "playGame";
    }
}
