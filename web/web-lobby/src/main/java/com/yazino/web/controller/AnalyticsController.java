package com.yazino.web.controller;

import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.api.RequestException;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class AnalyticsController {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsController.class);

    private WebApiResponses webApiResponses;
    private QueuePublishingService<ClientLogEvent> queueService;

    @Autowired
    public AnalyticsController(final WebApiResponses webApiResponses,
                               @Qualifier("clientLogEventQueuePublishingService") final QueuePublishingService<ClientLogEvent> queueService) {
        this.webApiResponses = webApiResponses;
        this.queueService = queueService;
    }

    @RequestMapping(value = "api/1.0/analytics/record", method = RequestMethod.POST)
    @AllowPublicAccess
    public void record(final HttpServletRequest request, final HttpServletResponse response, @RequestParam("ctx") String jsonContext) throws IOException {
        try {
            verifyCtxParameter(jsonContext);
            queueService.send(new ClientLogEvent(new DateTime(), jsonContext, ClientLogEventMessageType.LOG_ANALYTICS));
            webApiResponses.writeOk(response, "ok");
        } catch (RequestException requestException) {
            LOG.warn("ctx parameter does not contain a JSON string");
            webApiResponses.writeError(response, requestException.getHttpStatusCode(), requestException.getError());
        } catch (Exception e) {
            LOG.error("problem with adding message on to queue: {}, {}", jsonContext, e.getMessage());
            webApiResponses.writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "service is unavailable please try again later");
        }
    }

    private void verifyCtxParameter(final String jsonContext) throws RequestException {
        if (!StringUtils.isNotBlank(jsonContext)) {
            throw new RequestException(HttpServletResponse.SC_BAD_REQUEST, "ctx parameter must contain a JSON string");
        }
    }
}
