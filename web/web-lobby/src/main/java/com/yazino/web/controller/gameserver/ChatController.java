package com.yazino.web.controller.gameserver;

import com.yazino.platform.chat.ChatService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class ChatController {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);

    private static final String ERROR_PARSE_FAILED = "ERROR|Unable to parse message";
    private static final String ERROR_SESSION_EXPIRED = "ERROR|Session Expired";
    private static final String ERROR_METHOD_UNSUPPORTED = "ERROR|Only POST accepted";
    private static final String ERROR_COMMAND_REJECTED = "ERROR|Command rejected";
    private static final String OK_COMMAND_SENT = "OK|Command sent";

    private final LobbySessionCache lobbySessionCache;
    private final ChatService chatService;

    @Autowired
    public ChatController(final LobbySessionCache lobbySessionCache,
                          final ChatService chatService) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(chatService, "chatService may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.chatService = chatService;
    }

    private String[] getMessageParts(final HttpServletRequest request)
            throws IOException {
        try {
            final BufferedReader reader = request.getReader();
            if (reader == null) {
                LOG.warn("Couldn't obtain reader from request");
                return null;
            }

            final String line = reader.readLine();
            if (line == null) {
                LOG.debug("Empty chat request received");
                return null;
            }

            if (!line.contains("|")) {
                return new String[]{line};
            }

            return line.split("\\|");

        } catch (Exception e) {
            if (e.getClass().getName().equals("org.eclipse.jetty.io.EofException")) {
                LOG.warn("Message parsing failed due to EOF exception");
            } else {
                LOG.error("Message parsing failed", e);
            }
            return null;
        }
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/chat")
    public void handleChat(final HttpServletRequest request,
                           final HttpServletResponse response)
            throws IOException {
        if (!request.getMethod().equals("POST")) {
            response.sendError(HttpServletResponse.SC_OK, ERROR_METHOD_UNSUPPORTED);
            return;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            response.sendError(HttpServletResponse.SC_OK, ERROR_SESSION_EXPIRED);
            return;
        }

        final String[] toSend = getMessageParts(request);
        if (toSend == null) {
            response.sendError(HttpServletResponse.SC_OK, ERROR_PARSE_FAILED);
            return;
        }

        try {
            chatService.asyncProcessCommand(lobbySession.getPlayerId(), toSend);

            response.getWriter().write(OK_COMMAND_SENT);

        } catch (IllegalArgumentException e) {
            LOG.info("Invalid chat command received: {}", ArrayUtils.toString(toSend));
            response.sendError(HttpServletResponse.SC_OK, ERROR_COMMAND_REJECTED);

        } catch (Exception e) {
            LOG.error("Chat command processing failed: {}", ArrayUtils.toString(toSend), e);
            response.sendError(HttpServletResponse.SC_OK, ERROR_COMMAND_REJECTED);
        }
    }
}
