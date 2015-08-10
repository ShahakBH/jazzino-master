package com.yazino.controller;

import com.yazino.model.session.StandalonePlayerSession;
import com.yazino.platform.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@Controller
public class ChatController {
    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);

    private static final String ERROR_PARSE_FAILED = "ERROR|Unable to parse message";
    private static final String ERROR_SESSION_EXPIRED = "ERROR|Session Expired";
    private static final String ERROR_METHOD_UNSUPPORTED = "ERROR|Only POST accepted";
    private static final String ERROR_COMMAND_REJECTED = "ERROR|Command rejected";
    private static final String OK_COMMAND_SENT = "OK|Command sent";
    private static final int HTTP_OK = 200;

    private final StandalonePlayerSession lobbySessionCache;
    private final ChatService chatService;

    @Autowired
    public ChatController(final StandalonePlayerSession lobbySessionCache,
                          final ChatService chatService) {

        this.lobbySessionCache = lobbySessionCache;
        this.chatService = chatService;
    }

    private String[] getMessageParts(final HttpServletRequest request,
                                     final HttpServletResponse response)
            throws IOException {
        try {
            final BufferedReader reader = request.getReader();
            final String line = reader.readLine();
            if (!line.contains("|")) {
                return new String[]{line};
            }

            return line.split("\\|");

        } catch (Exception e) {
            LOG.error("Message parsing failed", e);
            response.sendError(HTTP_OK, ERROR_PARSE_FAILED);
            return null;
        }
    }

    @RequestMapping("/game/chat")
    public void handleChat(final HttpServletRequest request,
                           final HttpServletResponse response)
            throws IOException {
        if (!request.getMethod().equals("POST")) {
            response.sendError(HTTP_OK, ERROR_METHOD_UNSUPPORTED);
            return;
        }

        if (!lobbySessionCache.isActive()) {
            response.sendError(HTTP_OK, ERROR_SESSION_EXPIRED);
            return;
        }

        final String[] toSend = getMessageParts(request, response);
        if (toSend == null) {
            return;
        }

        try {
            chatService.processCommand(lobbySessionCache.getPlayerId(), toSend);

        } catch (Exception e) {
            LOG.error("Chat command processing failed", e);
            response.sendError(HTTP_OK, ERROR_COMMAND_REJECTED);
            return;
        }

        response.getWriter().write(OK_COMMAND_SENT);
    }
}
