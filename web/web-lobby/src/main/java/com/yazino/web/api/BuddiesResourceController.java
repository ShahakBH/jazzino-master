package com.yazino.web.api;

import com.yazino.platform.community.PlayerService;
import com.yazino.web.service.BuddyDirectoryService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
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
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonMap;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.commons.lang3.Validate.notNull;

/**
 */
@Controller
@RequestMapping("/api/1.0/social/*")
public class BuddiesResourceController {
    private static final Logger LOG = LoggerFactory.getLogger(BuddiesResourceController.class);

    // do not change the values of these
    static final String BUDDIES = "buddies";
    static final String BUDDIES_NAMES = "buddiesNames";
    static final String BUDDY_REQUESTS = "buddyRequests";
    static final String PAGED_BUDDY_NAMES = "pagedBuddyNames";
    private final JsonHelper jsonHelper = new JsonHelper();

    private final PlayerService playerService;
    private final LobbySessionCache lobbySessionCache;
    private final WebApiResponses responseWriter;
    private final BuddyDirectoryService buddyDirectoryService;

    @Autowired
    public BuddiesResourceController(PlayerService playerService,
                                     LobbySessionCache lobbySessionCache,
                                     WebApiResponses responseWriter,
                                     BuddyDirectoryService buddyDirectoryService) {
        notNull(playerService);
        notNull(lobbySessionCache);
        notNull(responseWriter);
        notNull(buddyDirectoryService);
        this.playerService = playerService;
        this.lobbySessionCache = lobbySessionCache;
        this.responseWriter = responseWriter;
        this.buddyDirectoryService = buddyDirectoryService;
    }

    /**
     * Return's all the current player's friends.
     * Clients should include a parameter called "orderBy" to protect against ordering changes, however the only
     * current order supported is NAME. Without this orderBy parameter, the order of results is not guaranteed.
     * Order types should follow {@link com.yazino.web.domain.social.PlayerInformationType}.
     *
     * @param request the request, never null
     */
    @RequestMapping(value = BUDDIES, method = RequestMethod.GET)
    public void allBuddies(HttpServletRequest request,
                           final HttpServletResponse response) throws IOException {

        BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        Map<BigDecimal, String> buddies = playerService.getFriendsOrderedByNickname(playerId); //OrderedSet from LinkedHashMap
        List<BigDecimal> orderedBuddies = newArrayList(buddies.keySet());
        LOG.debug("Player {} has {} buddies", playerId, orderedBuddies.size());

        writeJson(BUDDIES, orderedBuddies, response);
    }

    /**
     * Return's all the current player's friends in a map with their name.
     *
     * @param request the request, never null
     */
    @RequestMapping(value = BUDDIES_NAMES, method = RequestMethod.GET)
    public void allBuddiesWithNames(HttpServletRequest request, HttpServletResponse response) {
        BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        Map<BigDecimal, String> buddies = playerService.getFriendsOrderedByNickname(playerId); //OrderedSet from LinkedHashMap
        LOG.debug("Player {} has {} buddies", playerId, buddies.size());
        final List<Object[]> orderedListOfBuddies = convertToList(buddies);

        writeJson(response, String.format("{\"result\":\"ok\",\"buddies\":%s}", jsonHelper.serialize(orderedListOfBuddies)));

    }

    List<Object[]> convertToList(final Map<BigDecimal, String> buddies) {
        final List<Object[]> strings = newArrayList();
        for (BigDecimal id : buddies.keySet()) {
            strings.add(new Object[]{id, buddies.get(id)});
        }
        return strings;
    }

    private void writeJson(final HttpServletResponse response, final String result) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            final Writer writer = response.getWriter();
            writer.write(result);
        } catch (IOException e) {
            LOG.error("Failed to request friendsSummary", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (IOException e1) {
                //ignore
            }
        }
    }

    /**
     * Return's the current player's outstanding friend requests.
     * See allBuddies re. "orderBy"
     *
     * @param request the request, never null
     */
    @RequestMapping(value = BUDDY_REQUESTS, method = RequestMethod.GET)
    public void allBuddyRequests(HttpServletRequest request,
                                 final HttpServletResponse response) throws IOException {

        BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        List<BigDecimal> orderedRequests = playerService.getFriendRequestsOrderedByNickname(playerId);

        LOG.debug("Player {} has {} buddy requests", playerId, orderedRequests.size());

        writeJson(BUDDY_REQUESTS, orderedRequests, response);
    }

    private void writeJson(final String key,
                           final List<BigDecimal> playerIds,
                           final HttpServletResponse response) throws IOException {
        responseWriter.writeOk(response, singletonMap(key, playerIds));
    }

    @RequestMapping(value = PAGED_BUDDY_NAMES, method = RequestMethod.GET)
    public void loadPageOfBuddies(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @RequestParam(required = false) String filter,
                                  @RequestParam(required = false) Integer pageSize,
                                  @RequestParam(defaultValue = "0") Integer pageIndex) throws IOException {
        LOG.info("load page of buddies (pageSize={}, pageIndex={}, filter={})", pageSize, pageIndex, filter);

        try {
            checkNotNull(pageSize, "pageSize");
            checkNotNull(pageIndex, "pageIndex");
            final LobbySession session = lobbySessionCache.getActiveSession(request);
            if (session == null) {
                LOG.warn("attempt to load a page of buddies with a null session.");
                responseWriter.writeError(response, SC_FORBIDDEN, "no session");
            } else {
                BigDecimal playerId = session.getPlayerId();
                final BuddyDirectoryService.PageOfBuddies page = buddyDirectoryService.loadPage(playerId, filter, pageSize, pageIndex);
                responseWriter.writeOk(response, page);
            }
        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void checkNotNull(Object value, final String parameterName) {
        // not using checkNotNull as this doesn't use IllegalArgumentException
        checkArgument(value != null, "parameter '" + parameterName + "' is missing");
    }


}
