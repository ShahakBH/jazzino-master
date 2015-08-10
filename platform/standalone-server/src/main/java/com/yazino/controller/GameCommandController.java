package com.yazino.controller;

import com.yazino.host.community.StandaloneCommunityService;
import com.yazino.model.session.StandalonePlayerSession;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.platform.table.Command;
import com.yazino.platform.table.TableService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Controller
public class GameCommandController {
    private static final Logger LOG = LoggerFactory.getLogger(GameCommandController.class);
    public static final int COMMAND_TOKENS = 3;

    private final TableService tableService;
    private final StandalonePlayerSession lobbySessionCache;
    private final StandaloneCommunityService communityService;

    static final String EPIC_FAIL = "EPIC_FAIL";

    @Autowired
    public GameCommandController(final TableService tableService,
                                 final StandalonePlayerSession lobbySessionCache,
                                 final StandaloneCommunityService communityService) {
        this.tableService = tableService;
        this.lobbySessionCache = lobbySessionCache;
        this.communityService = communityService;
    }

    /*
    * Helper method for parsing constructing a command wrapper. Package protected for testing purposes.
    */
    Command constructCommand(final BigDecimal userID,
                             final String commandParameter)
            throws IllegalArgumentException {
        if (commandParameter == null) {
            throw new IllegalArgumentException("Null parameter passed to command object.");
        }
        final String[] postedStrings = commandParameter.split("\\|");
        if (postedStrings.length < COMMAND_TOKENS) {
            throw new IllegalArgumentException("Invalid number of parameters passed in POST, "
                    + "At least: TableId|GameId|Type required; message was " + commandParameter);
        }
        final BigDecimal tableId = BigDecimal.valueOf(parseLong(postedStrings[0], commandParameter));
        final Long gameId = parseLong(postedStrings[1], commandParameter);
        final String[] args = Arrays.copyOfRange(postedStrings, 3, postedStrings.length);
        return new Command(tableId, gameId, userID, null, postedStrings[2], args);
    }

    private Long parseLong(final String value,
                           final String message) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Invalid numeric value: " + value + "; message was " + message);
        }
        return Long.parseLong(value);
    }

    @RequestMapping(value = "game/giga", method = RequestMethod.POST)
    public ModelAndView handleCommand(final HttpServletRequest request,
                                      final HttpServletResponse response)
            throws Exception {
        if (!lobbySessionCache.isActive()) {
            response.getWriter().write("_NO_SESSION");
            return null;
        }
        final BigDecimal playerId = lobbySessionCache.getPlayerId();
        final BufferedReader reader = request.getReader();
        final List<Command> commands = new ArrayList<>();
        String message = "";
        try {
            String line = reader.readLine();
            do {
                message = message + line + "\n";
                final Command wrapper = constructCommand(playerId, line);
                if (wrapper != null) {
                    commands.add(wrapper);
                }
                line = reader.readLine();
            } while (!isEmpty(line));

            if (LOG.isDebugEnabled()) {
                LOG.debug("Messsage=\n" + message + " Number of lines=" + commands.size());
            }

            for (Command command : commands) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Sending command " + command.toString());
                }

                tableService.sendCommand(command.withTimestamp(new DateTime().toDate()));
            }
            response.setContentType("text/html");
            response.getWriter().write("OK");
            return null;

        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid command: " + e.getMessage());
            response.getWriter().write(EPIC_FAIL);
            return null;

        } catch (Throwable e) {
            LOG.error("Error processing:[" + message + "]", e);
            response.getWriter().write(EPIC_FAIL);
            return null;
        }
    }

    @RequestMapping("/game/community")
    public ModelAndView handleCommunity(final HttpServletRequest request,
                                        final HttpServletResponse response) throws Exception {
        final BufferedReader reader = request.getReader();
        final BigDecimal playerId = lobbySessionCache.getPlayerId();
        if (playerId == null) {
            response.sendError(HttpServletResponse.SC_OK, "Session Expired");
            return null;
        }
        try {
            final String command = reader.readLine();
            if (LOG.isDebugEnabled()) {
                LOG.debug("command [" + playerId + "] " + command);
            }
            if (command.startsWith("publish")) {
                final String[] tokens = command.split("\\|");
                String gameType = null;
                if (tokens.length == 2) {
                    gameType = tokens[1];
                }
                communityService.publishCommunityStatus(playerId, gameType);
//                playerXPPublisher.publishXP(playerId, gameType);

            } else if (command.startsWith("request|")) {
                final String[] args = command.split("\\|");
                communityService.requestRelationshipChange(playerId,
                        new BigDecimal(args[1]), RelationshipAction.valueOf(args[2]));
            }
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_OK, e.getMessage());
            return null;
        }
        response.setContentType("text/html");
        response.getWriter().write("OK");
        return null;
    }

}
