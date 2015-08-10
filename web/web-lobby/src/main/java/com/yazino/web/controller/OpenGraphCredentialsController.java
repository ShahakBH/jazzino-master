package com.yazino.web.controller;

import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.bi.opengraph.OpenGraphCredentialsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller
public class OpenGraphCredentialsController {

    private static final Logger LOG = LoggerFactory.getLogger(OpenGraphCredentialsController.class);

    private QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService;

    @Autowired
    public OpenGraphCredentialsController(
            @Qualifier("openGraphCredentialsService")
            final QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService) {
        this.openGraphCredentialsService = openGraphCredentialsService;
    }

    @RequestMapping(value = "/opengraph/credentials", method = RequestMethod.POST)
    public void updateCredentials(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  @RequestParam(value = "playerId", required = false) final BigInteger playerId,
                                  @RequestParam(value = "gameType", required = false) final String gameType,
                                  @RequestParam(value = "accessToken", required = false) final String accessToken) throws IOException {

        if (!hasParameter("playerId", playerId, request, response)
                || !hasParameter("gameType", gameType, request, response)
                || !hasParameter("accessToken", accessToken, request, response)) {
            LOG.warn("FBOG callback missing parameter: playerID:{} gameType:{} accessToken:{}", playerId, gameType, accessToken);
            return;
        }

        response.setContentType(MediaType.APPLICATION_JSON.getType());
        response.setStatus(HttpServletResponse.SC_ACCEPTED); // TODO review response

        response.getWriter().write("{}");

        request.getSession().setAttribute("facebookAccessToken." + gameType, accessToken);
        openGraphCredentialsService.send(new OpenGraphCredentialsMessage(playerId, gameType, accessToken));
    }
}
