package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.table.TableException;
import com.yazino.platform.table.TableSummary;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.form.TableLocatorForm;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.TableLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

@Controller
public class MobileTableLocatorController {
    private static final Logger LOG = LoggerFactory.getLogger(MobileTableLocatorController.class);

    private static final String TABLE_LOCATOR_FORM = "tableLocatorForm";
    static final String ERROR_NO_LOBBY_SESSION = "NO_LOBBY_SESSION";
    static final String ERROR_GAME_DISABLED = "GAME_DISABLED";

    private final WebApiResponses responseWriter;
    private final TableLobbyService tableLobbyService;
    private final SiteConfiguration siteConfiguration;
    private final LobbySessionCache lobbySessionCache;
    private final GameAvailabilityService gameAvailabilityService;
    private final YazinoConfiguration yazinoConfiguration;


    @Autowired(required = true)
    public MobileTableLocatorController(
            final WebApiResponses responseWriter,
            @Qualifier("tableLobbyService") final TableLobbyService tableLobbyService,
            @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            final GameAvailabilityService gameAvailabilityService,
            final YazinoConfiguration yazinoConfiguration) {
        Validate.notNull(lobbySessionCache, "Lobby Session Cache may not be null");
        this.responseWriter = responseWriter;
        this.lobbySessionCache = lobbySessionCache;
        this.tableLobbyService = tableLobbyService;
        this.siteConfiguration = siteConfiguration;
        this.gameAvailabilityService = gameAvailabilityService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @RequestMapping("/lobbyCommand/mobile/tableLocator")
    public void findTableId(@ModelAttribute(TABLE_LOCATOR_FORM) final TableLocatorForm form,
                            final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        final TableLocatorResponse tableLocatorResponse = new TableLocatorResponse();

        if (lobbySession == null) {
            tableLocatorResponse.setError(ERROR_NO_LOBBY_SESSION);
            responseWriter.writeOk(response, tableLocatorResponse);
            return;
        }
        final String error = form.validate();
        if (StringUtils.isBlank(error)) {
            final String variationName = resolveVariationName(form.getVariationName());
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("TableLocatorForm {} ", ReflectionToStringBuilder.reflectionToString(form));
                    LOG.debug("Variation: {}", variationName);
                }

                GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(form.getGameType());
                if (availability.getAvailability() == GameAvailabilityService.Availability.DISABLED) {
                    tableLocatorResponse.setError(ERROR_GAME_DISABLED);
                } else {
                    final Set<String> tags = Collections.<String>emptySet();
                    final TableSummary tableSummary = tableLobbyService.findOrCreateTableByGameTypeAndVariation(
                            form.getGameType(), variationName,
                            form.getClientId(), lobbySession.getPlayerId(), tags);
                    tableLocatorResponse.setTableId(tableSummary.getId());
                    tableLocatorResponse.setAvailability(availability.getAvailability());
                    if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
                        tableLocatorResponse.setMaintenanceStartsAtMillis(availability.getMaintenanceStartsAtMillis());
                    }
                }
            } catch (Throwable e) {
                LOG.warn(String.format("Failed to find a table for gameType [%s], variation [%s], client [%s].",
                        form.getGameType(), variationName, form.getClientId()), e);
                tableLocatorResponse.setError(e.getMessage());
            }
        } else {
            tableLocatorResponse.setError(error);
        }

        responseWriter.writeOk(response, tableLocatorResponse);
    }

    private String resolveVariationName(String variationName) {
        final String sanitizedVariationName = variationName.replaceAll("[^\\w]", "_");
        final String key = "table-locator.mappings." + sanitizedVariationName;
        if (yazinoConfiguration.containsKey(key)) {
            return yazinoConfiguration.getString(key);
        }
        return variationName;
    }

    @RequestMapping("/api/1.0/private_table")
    public void resumeGame(@ModelAttribute(TABLE_LOCATOR_FORM) final TableLocatorForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        final TableLocatorResponse tableLocatorResponse = new TableLocatorResponse();

        if (lobbySession == null) {
            tableLocatorResponse.setError(ERROR_NO_LOBBY_SESSION);
            responseWriter.writeOk(response, tableLocatorResponse);
            return;
        }

        final BigDecimal playerId = lobbySession.getPlayerId();

        try {
            GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(form.getGameType());
            if (availability.getAvailability() == GameAvailabilityService.Availability.DISABLED) {
                tableLocatorResponse.setError(ERROR_GAME_DISABLED);
            } else {
                final TableSummary tableSummary = tableLobbyService.findPrivateTable(playerId, form.getGameType(), form.getVariationName(), form.getClientId());
                tableLocatorResponse.setTableId(tableSummary.getId());
                tableLocatorResponse.setAvailability(availability.getAvailability());
                if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
                    tableLocatorResponse.setMaintenanceStartsAtMillis(availability.getMaintenanceStartsAtMillis());
                }
            }
        } catch (TableException e) {
            tableLocatorResponse.setError(e.getMessage());
        }

        responseWriter.writeOk(response, tableLocatorResponse);
    }

    static class TableLocatorResponse {
        private BigDecimal tableId;
        private String error;
        private GameAvailabilityService.Availability availability;
        private Long maintenanceStartsAtMillis;

        public String getError() {
            return error;
        }

        public void setError(final String error) {
            this.error = error;
        }

        public BigDecimal getTableId() {
            return tableId;
        }

        public void setTableId(final BigDecimal tableId) {
            this.tableId = tableId;
        }

        public GameAvailabilityService.Availability getAvailability() {
            return availability;
        }

        public void setAvailability(GameAvailabilityService.Availability availability) {
            this.availability = availability;
        }

        public Long getMaintenanceStartsAtMillis() {
            return maintenanceStartsAtMillis;
        }

        public void setMaintenanceStartsAtMillis(Long maintenanceStartsAtMillis) {
            this.maintenanceStartsAtMillis = maintenanceStartsAtMillis;
        }
    }
}
