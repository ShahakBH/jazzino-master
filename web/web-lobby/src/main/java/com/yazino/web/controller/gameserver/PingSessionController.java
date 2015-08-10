package com.yazino.web.controller.gameserver;

import com.yazino.platform.messaging.publisher.SpringAMQPRoutedTemplates;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;

@Controller
public class PingSessionController {
    private static final Logger LOG = LoggerFactory.getLogger(PingSessionController.class);

    private static final String PING_DOCUMENT_TYPE = "PING";

    private final LobbySessionCache lobbySessionCache;
    private final SpringAMQPRoutedTemplates publisherTemplates;
    private final MessageProperties properties;

    @Autowired(required = true)
    public PingSessionController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("playerRabbitMQRoutedPublishers") final SpringAMQPRoutedTemplates publisherTemplates) {
        this.lobbySessionCache = lobbySessionCache;
        this.publisherTemplates = publisherTemplates;

        properties = new MessageProperties();
        properties.setContentType(PING_DOCUMENT_TYPE);
    }

    @AllowPublicAccess
    @RequestMapping({"/game-server/command/pingSession", "/game-server/command/ping" })
    public ModelAndView pingSession(final HttpServletRequest request,
                                    final HttpServletResponse response)
            throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received Ping Session request...");
        }

        final LobbySession lobbysession = lobbySessionCache.getActiveSession(request);

        response.setContentType("text/plain");
        if (lobbysession == null) {
            response.getWriter().write("_NO_SESSION");
            return null;
        }
        response.getWriter().write("OK|Session Pinged.");

        final BufferedReader reader = request.getReader();
        final String payload = reader.readLine();
        if (payload != null && payload.trim().length() > 0 && publisherTemplates != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("sending ping message  player=[%s] message=[%s]",
                        lobbysession.getPlayerId(), payload));
            }


            final byte[] messageBodyBytes = payload.getBytes("UTF-8");
            final String routingKey = "PLAYER." + lobbysession.getPlayerId();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Publishing ping message with routing key " + routingKey);
            }

            final Message message = new Message(messageBodyBytes, properties);
            send(lobbysession.getPlayerId(), routingKey, message);
        }
        return null;
    }

    private void send(final BigDecimal playerId, final String routingKey, final Message message) {
        final String messagingHost = publisherTemplates.hostFor(playerId);
        try {
            publisherTemplates.templateFor(messagingHost).send(routingKey, message);

        } catch (AmqpConnectException e) {
            LOG.warn("AMQP connection exception received, blacklisting {} and retrying", messagingHost, e);
            publisherTemplates.blacklist(messagingHost);
            send(playerId, routingKey, message);
        }
    }
}
