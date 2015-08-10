package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.LaunchConfiguration;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.MessagingHostResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches launch configuration information.
 */
@Controller
public class LaunchConfigurationController {
    private static final Logger LOG = LoggerFactory.getLogger(LaunchConfigurationController.class);

    private static final String SESSION_EXPIRED = "Your session expired.  Please log in again.";
    private static final int HTTP_UNAUTHORISED = 401;

    private Map<String, String> configuration;

    private final MessagingHostResolver messagingHostResolver;

    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public LaunchConfigurationController(final LaunchConfiguration launchConfiguration,
                                         final MessagingHostResolver messagingHostResolver,
                                         final LobbySessionCache lobbySessionCache) {
        this.messagingHostResolver = messagingHostResolver;
        this.lobbySessionCache = lobbySessionCache;
        configuration = new HashMap<String, String>();
        configuration.put("amqpVirtualHost", launchConfiguration.getMessagingVirtualHost());
        configuration.put("amqpHost", launchConfiguration.getMessagingHost());
        configuration.put("amqpPort", launchConfiguration.getMessagingPort());
        configuration.put("commandUrl", launchConfiguration.getCommandBaseUrl());
        configuration.put("contentUrl", launchConfiguration.getContentUrl());
        configuration.put("clientUrl", launchConfiguration.getClientUrl());
        configuration.put("permanentContentUrl", launchConfiguration.getPermanentContentUrl());
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/launchConfiguration")
    public void launchConfiguration(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.debug("Session expired for active user.");
            response.sendError(HTTP_UNAUTHORISED, SESSION_EXPIRED);
            return;
        }

        final String messagingHost = messagingHostResolver.resolveMessagingHostForPlayer(session.getPlayerId());
        configuration.put("amqpHost", messagingHost);

        response.setContentType(MediaType.APPLICATION_JSON.toString());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new JsonHelper().serialize(configuration));
    }
}
