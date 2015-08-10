package com.yazino.web.controller;

import com.yazino.platform.metrics.MetricsService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.session.SessionService;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableSearchOption;
import com.yazino.platform.table.TableService;
import com.yazino.platform.table.TableType;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class MonitorController {
    private static final Logger LOG = LoggerFactory.getLogger(MonitorController.class);

    private final TableService tableService;
    private final SessionService sessionService;
    private final PlayerProfileService userProfileService;
    private final MetricsService metricsService;
    private final WebApiResponses webApiResponses;

    @Autowired
    public MonitorController(final TableService tableService,
                             final SessionService sessionService,
                             final PlayerProfileService userProfileService,
                             final MetricsService metricsService,
                             final WebApiResponses webApiResponses) {
        notNull(tableService, "tableService may not be null");
        notNull(sessionService, "sessionService may not be null");
        notNull(userProfileService, "userProfileService may not be null");
        notNull(metricsService, "metricsService may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.tableService = tableService;
        this.sessionService = sessionService;
        this.userProfileService = userProfileService;
        this.metricsService = metricsService;
        this.webApiResponses = webApiResponses;
    }

    @RequestMapping("/command/metrics/{range}")
    public void showMetrics(final HttpServletResponse response,
                            @PathVariable("range") final String requestRange) throws IOException {
        MetricsService.Range range;
        try {
            notNull(response, "response may not be null");
            range = MetricsService.Range.valueOf(requestRange.toUpperCase());

        } catch (NullPointerException | IllegalArgumentException e) {
            webApiResponses.writeError(response, SC_BAD_REQUEST, "Invalid range: " + requestRange);
            return;
        }

        try {
            webApiResponses.writeOk(response, metricsService.getMeters(range));

        } catch (Exception e) {
            LOG.error("Failed to return meters for range {}", requestRange, e);
            webApiResponses.writeError(response, SC_INTERNAL_SERVER_ERROR, "Failed to return meters for range " + requestRange);
        }
    }

    @RequestMapping({"/publicCommand/monitor", "/command/monitor"})
    public void listMonitoringInformation(final HttpServletResponse response) throws IOException {
        notNull(response, "response may not be null");

        try {
            final Map<String, Object> state = new HashMap<>();
            state.put("players", playerCount());
            state.put("userProfiles", userProfileCount());
            state.put("tables", tableInformation());
            state.put("gameTypes", gameTypeInformation());

            webApiResponses.writeOk(response, state);

        } catch (Exception e) {
            LOG.error("Failed to write monitoring response", e);
            webApiResponses.writeError(response, SC_INTERNAL_SERVER_ERROR, "Failed to build monitoring state");
        }

    }

    private int playerCount() {
        try {
            return sessionService.countSessions(true);
        } catch (Exception e) {
            LOG.error("Failed to read session count", e);
            return -1;
        }
    }

    private int userProfileCount() {
        try {
            return userProfileService.count();
        } catch (Exception e) {
            LOG.error("Failed to read user profile count", e);
            return -1;
        }
    }

    private Map<String, Object> gameTypeInformation() {
        try {
            final Map<String, Object> gameTypeInformation = new HashMap<>();
            for (GameTypeInformation gameType : tableService.getGameTypes()) {
                gameTypeInformation.put(gameType.getId(), gameType.isAvailable());
            }
            return gameTypeInformation;

        } catch (Exception e) {
            LOG.error("Failed to read game type information", e);
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> tableInformation() {
        try {
            final Map<String, Object> tableInformation = new HashMap<>();
            tableInformation.put("inErrorState", tableService.countByType(TableType.ALL, TableSearchOption.IN_ERROR_STATE));
            return tableInformation;

        } catch (Exception e) {
            LOG.error("Failed to read table statistics", e);
            return Collections.emptyMap();
        }
    }

}
