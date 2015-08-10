package com.yazino.web.controller.gameserver;

import com.yazino.platform.table.Command;
import com.yazino.platform.table.TableService;
import com.yazino.spring.security.AllowPublicAccess;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * @deprecated "This is a legacy controller. Newer clients should use the API version in web-lobby."
 */
@Controller("legacyGameCommandController")
public class GameCommandController {
    private static final Logger LOG = LoggerFactory.getLogger(GameCommandController.class);

    private static final int COMMAND_FIELDS = 3;

    private final TableService tableService;
    private final LobbySessionCache lobbySessionCache;

    static final String EPIC_FAIL = "EPIC_FAIL";

    @Autowired
    public GameCommandController(final TableService tableService,
                                 final LobbySessionCache lobbySessionCache) {
        notNull(tableService, "tableService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");

        this.tableService = tableService;
        this.lobbySessionCache = lobbySessionCache;
    }

    /*
    * Helper method for parsing constructing a command wrapper. Package protected for testing purposes.
    */
    Command constructCommand(final BigDecimal userId,
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
        final BigDecimal tableId = BigDecimal.valueOf(tableIdLong);
        final Long gameId = parseLong(postedStrings[1], commandParameter);
        final String[] args = Arrays.copyOfRange(postedStrings, 3, postedStrings.length);
        return new Command(tableId, gameId, userId, sessionId, postedStrings[2], args);
    }

    private Long parseLong(final String value,
                           final String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Invalid numeric value: " + value + "; message was " + message);
        }
        return Long.parseLong(value);
    }

    @AllowPublicAccess
    @RequestMapping(value = "/game-server/command/giga", method = RequestMethod.POST)
    public void handleCommand(final HttpServletRequest request,
                              final HttpServletResponse response)
            throws Exception {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.getWriter().write("_NO_SESSION");
            return;
        }
        final BufferedReader reader = request.getReader();
        final List<Command> commands = new ArrayList<Command>();
        try {
            String line = readNextLineFrom(reader);
            do {
                final Command wrapper = constructCommand(lobbySession.getPlayerId(), lobbySession.getSessionId(), line);
                if (wrapper != null) {
                    commands.add(wrapper);
                }
                line = readNextLineFrom(reader);
            } while (!isEmpty(line));

            for (Command command : commands) {
                LOG.debug("Sending command {}", command);

                tableService.asyncSendCommand(command.withTimestamp(new DateTime().toDate()));
            }
            response.setContentType("text/html");
            response.getWriter().write("OK");

        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid command: {}", e.getMessage());
            response.getWriter().write(EPIC_FAIL);

        } catch (Throwable e) {
            LOG.error("Error processing:[{}]", commands, e);
            response.getWriter().write(EPIC_FAIL);
        }
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
