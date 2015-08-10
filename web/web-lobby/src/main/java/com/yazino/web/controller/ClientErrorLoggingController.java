package com.yazino.web.controller;

import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.spring.security.AllowPublicAccess;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

@Controller
public class ClientErrorLoggingController {
    public static final Logger LOG = LoggerFactory.getLogger(ClientErrorLoggingController.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final QueuePublishingService<ClientLogEvent> service;

    @Autowired
    public ClientErrorLoggingController(@Qualifier("clientLogEventQueuePublishingService") QueuePublishingService<ClientLogEvent> service) {
        this.service = service;
    }

    @RequestMapping(value = "/client/log", method = RequestMethod.POST)
    @AllowPublicAccess
    public void log(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Headers:\n");
            for (Enumeration i = request.getHeaderNames(); i.hasMoreElements(); ) {
                String headerName = (String) i.nextElement();
                String headerValue = request.getHeader(headerName);
                stringBuilder.append(headerName).append(": ").append(headerValue).append("\n");
            }
            stringBuilder.append("Parameters:\n");
            for (Enumeration i = request.getParameterNames(); i.hasMoreElements(); ) {
                String parameterName = (String) i.nextElement();
                String parameterValue = request.getParameter(parameterName);
                stringBuilder.append(parameterName).append(": ").append(parameterValue).append("\n");
            }
            stringBuilder.append("ts").append(": ").append(DATE_FORMAT.format(new Date()));
            LOG.info(stringBuilder.toString());
            if (request.getParameter("ctx") != null) {
                sendClientLogEvent(request);
            }
            response.getWriter().write("OK");

        } catch (Exception e) {
            LOG.error("Client logging failed", e);
            response.getWriter().write("FAILED");
        }
    }

    private void sendClientLogEvent(HttpServletRequest request) {
        final String json = request.getParameter("ctx");
        ClientLogEvent message = new ClientLogEvent(new DateTime(), json, ClientLogEventMessageType.LOG_EVENT);
        service.send(message);
    }

}
