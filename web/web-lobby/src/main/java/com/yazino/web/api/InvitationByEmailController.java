package com.yazino.web.api;

import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.service.InvitationLobbyService;
import com.yazino.web.service.InvitationSendingResult;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/api/1.0/email/invitation")
public class InvitationByEmailController {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationByEmailController.class);

    private final LobbySessionCache lobbySessionCache;
    private final InvitationLobbyService invitationLobbyService;
    private final GameTypeResolver gameTypeResolver;
    private final WebApiResponses webApiResponses;

    @Autowired
    public InvitationByEmailController(final LobbySessionCache lobbySessionCache,
                                       final InvitationLobbyService invitationLobbyService,
                                       final GameTypeResolver gameTypeResolver,
                                       final WebApiResponses webApiResponses) {
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(invitationLobbyService, "invitationLobbyService may not be null");
        notNull(gameTypeResolver, "gameTypeResolver may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.invitationLobbyService = invitationLobbyService;
        this.gameTypeResolver = gameTypeResolver;
        this.webApiResponses = webApiResponses;
    }

    /**
     * Send an invitation by email
     *
     * @param emailAddressesCsv, never null, a csv of email addresses
     * @param source             the source to record
     * @param request            http request
     * @param response           http response
     */
    @RequestMapping(method = RequestMethod.POST)
    public void sendInvitationEmail(@RequestParam("emails") String emailAddressesCsv,
                                    @RequestParam("source") String source,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            sendAuthorised(response);
            return;
        }

        String[] emailAddresses = StringUtils.stripAll(emailAddressesCsv.split(","));

        InvitationSendingResult result = invitationLobbyService.sendInvitations(
                lobbySession.getPlayerId(),
                gameTypeResolver.appInfoFor(request, response, lobbySession),
                source,
                "",
                emailAddresses,
                false,
                request.getRemoteAddr());

        LOG.debug("Player {}'s request to send invites to {} yielded {} success and {} failures",
                lobbySession.getPlayerId(), emailAddressesCsv, result.getSuccessful(), result.getRejections().size());

        final Map<String, Object> model = new HashMap<>(2);
        model.put("sent", Integer.toString(result.getSuccessful()));
        model.put("rejected", rejectionsFrom(result));
        webApiResponses.writeOk(response, model);
    }

    private Map<String, Object> rejectionsFrom(final InvitationSendingResult result) {
        final Map<String, Object> rejections = new HashMap<>();
        for (InvitationSendingResult.Rejection rejection : result.getRejections()) {
            rejections.put(rejection.getEmail(), rejection.getResultCode().toString());
        }
        return rejections;
    }

    private void sendAuthorised(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException ignored) {
            // ignored
        }
    }
}
