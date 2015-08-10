package com.yazino.web.api;

import com.yazino.platform.table.Command;
import com.yazino.platform.table.TableService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/api/1.0/game/send-command")
public class GameCommandController {
    private static final Logger LOG = LoggerFactory.getLogger(GameCommandController.class);

    private static final int COMMAND_FIELDS = 3;

    private final TableService tableService;
    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public GameCommandController(final TableService tableService,
                                 final LobbySessionCache lobbySessionCache) {
        notNull(tableService, "tableService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");

        this.tableService = tableService;
        this.lobbySessionCache = lobbySessionCache;
    }

    @RequestMapping(method = RequestMethod.GET)
    public void getCommand(final HttpServletResponse response) throws IOException {
        writeErrorTo(response, "invalid_method", HttpServletResponse.SC_BAD_REQUEST);
    }

    @RequestMapping(method = RequestMethod.POST)
    public void postCommand(final HttpServletRequest request,
                            final HttpServletResponse response)
            throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            writeErrorTo(response, "no_session", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        List<Command> commands = null;
        try {
            commands = parseCommandsFrom(request, lobbySession.getPlayerId(), lobbySession.getSessionId());

            for (Command command : commands) {
                LOG.debug("Sending command {}", command);
                tableService.asyncSendCommand(command.withTimestamp(new DateTime().toDate()));
            }
            writeSuccessTo(response);

        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid command: {}", e.getMessage());
            writeErrorTo(response, "invalid_command", HttpServletResponse.SC_BAD_REQUEST);

        } catch (Throwable e) {
            LOG.error("Error processing:[{}]", commands, e);
            writeErrorTo(response, "server_error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /*
    * Helper method for parsing constructing a command wrapper. Package protected for testing purposes.
    */
    Command constructCommand(final BigDecimal playerId,
                             final BigDecimal sessionId,
                             final String commandParameter)
            throws IllegalArgumentException {
        if (commandParameter == null) {
            throw new IllegalArgumentException("Null parameter passed to command object.");
        }

        final String[] postedStrings = commandParameter.split("\\|");
        if (postedStrings.length < COMMAND_FIELDS) {
            throw new IllegalArgumentException("Invalid number of parameters passed in POST, "
                    + "At least: TableId|GameId|Type required; message was " + commandParameter);
        }

        final Long tableIdLong = parseLong(postedStrings[0], commandParameter);
        if (tableIdLong < 0) {
            LOG.warn("Received command for invalid tableId {}. Ignoring.", tableIdLong);
            return null;
        }

        final Long gameId = parseLong(postedStrings[1], commandParameter);
        final String[] args = Arrays.copyOfRange(postedStrings, 3, postedStrings.length);
        return new Command(BigDecimal.valueOf(tableIdLong), gameId, playerId, sessionId, postedStrings[2], args);
    }

    private Long parseLong(final String value,
                           final String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Invalid numeric value: " + value + "; message was " + message);
        }
        return Long.parseLong(value);
    }

    private void writeSuccessTo(final HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{}");
    }

    private void writeErrorTo(final HttpServletResponse response,
                              final String errorText,
                              final int responseCode) throws IOException {
        response.setStatus(responseCode);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + errorText + "\"}");
    }

    private List<Command> parseCommandsFrom(final HttpServletRequest request,
                                            final BigDecimal playerId,
                                            final BigDecimal sessionId)
            throws IOException {
        final List<Command> commands = new ArrayList<>();

        final BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));

        String line = readNextLineFrom(reader);
        do {
            final Command wrapper = constructCommand(playerId, sessionId, line);
            if (wrapper != null) {
                commands.add(wrapper);
            }
            line = readNextLineFrom(reader);
        } while (!isEmpty(line));

        return commands;
    }

    private String readNextLineFrom(final BufferedReader reader) throws IOException {
        try {
            return reader.readLine();
        } catch (IOException e) {
            LOG.info("Received IO exception when reading from input: {}", e.getMessage());
            return null;
        }
    }
}
