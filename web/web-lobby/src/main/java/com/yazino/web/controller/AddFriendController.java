package com.yazino.web.controller;


import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.RelationshipAction;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class AddFriendController {
    private static final Logger LOG = LoggerFactory.getLogger(AddFriendController.class);

    private final CommunityService communityService;
    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public AddFriendController(final CommunityService communityService,
                               final LobbySessionCache lobbySessionCache) {
        notNull(communityService, "communityService may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");

        this.communityService = communityService;
        this.lobbySessionCache = lobbySessionCache;
    }

    @RequestMapping({"/friends/add", "/lobby/addFriend"})
    public void addFriend(final HttpServletRequest request,
                            final HttpServletResponse response,
                            @RequestParam(value = "playerId", required = false) final String newFriendIdAsString)
            throws IOException {
        if (!hasParameter("playerId", newFriendIdAsString, request, response)) {
            return;
        }

        BigDecimal newFriendId;
        try {
            newFriendId = new BigDecimal(newFriendIdAsString);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid friend ID received: {}", newFriendIdAsString);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new RuntimeException("No lobby session found");
        }

        boolean success = true;

        try {
            communityService.requestRelationshipChange(lobbySession.getPlayerId(),
                    newFriendId, RelationshipAction.ADD_FRIEND);
        } catch (Exception e) {
            LOG.error("Could not add relationship from {} to {}", lobbySession.getPlayerId(), newFriendId, e);
            success = false;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{success:" + success + "}");
    }
}
