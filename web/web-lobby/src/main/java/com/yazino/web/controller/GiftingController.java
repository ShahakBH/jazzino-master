package com.yazino.web.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.gifting.*;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.GiftLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.http.HttpStatus.*;

@Controller
public class GiftingController {
    private static final Logger LOG = LoggerFactory.getLogger(GiftingController.class);

    private final WebApiResponses responseWriter;
    private final LobbySessionCache lobbySessionCache;
    private final GiftLobbyService giftingService;
    private final PlayerProfileService playerProfileService;
    private final SiteConfiguration siteConfiguration;
    private final CommunityService communityService;

    @Autowired
    public GiftingController(final WebApiResponses responseWriter,
                             @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                             final GiftLobbyService giftService,
                             final PlayerProfileService playerProfileService,
                             final SiteConfiguration siteConfiguration,
                             final CommunityService communityService) {
        notNull(lobbySessionCache, "lobbySessionCache cannot be null");
        notNull(responseWriter, "responseWriter cannot be null");
        notNull(giftService, "giftService cannot be null");
        notNull(siteConfiguration, "siteConfiguration cannot be null");
        notNull(communityService, "communityService cannot be null");

        this.communityService = communityService;
        this.giftingService = giftService;
        this.playerProfileService = playerProfileService;
        this.lobbySessionCache = lobbySessionCache;
        this.responseWriter = responseWriter;
        this.siteConfiguration = siteConfiguration;
    }

    @RequestMapping(value = "/api/1.0/gifting/status", method = RequestMethod.POST)
    public void getGiftingStatus(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final String csvPlayerIds) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.warn("getGiftingStatus could not load session for player {}", request);
            responseWriter.writeError(response, UNAUTHORIZED.value(), "no session");
            return;
        }

        final BigDecimal playerId = session.getPlayerId();
        LOG.debug("gifting status for player ({})'giftIdAsString buddies {}", playerId.toPlainString(), csvPlayerIds);

        if (StringUtils.isBlank(csvPlayerIds)) {
            responseWriter.writeOk(response, new HashSet<GiftableStatus>());
            return;
        }

        final HashSet<BigDecimal> buddies = newHashSet();
        for (String buddyId : csvPlayerIds.split(",")) {
            try {
                buddies.add(new BigDecimal(buddyId));
            } catch (Exception e) {
                LOG.error("could not convert player Id = {} in {} to bigdecimal", buddyId, csvPlayerIds, e);
                responseWriter.writeError(response, BAD_REQUEST.value(), "could not parse player id. check log for details");
                return;
            }
        }

        responseWriter.writeOk(response, populatePlayerDetailsFor(giftingService.getGiftableStatusForPlayers(playerId, buddies)));
    }

    private Object populatePlayerDetailsFor(final Set<GiftableStatus> giftableStatuses) {
        // Player name/image lookup should occur on the client. In the absence of such, we need to do it at the lobby level.
        // The Grid will *not* return this information any more. See WEB-4809.

        final Map<BigDecimal, String> displayNames = playerProfileService.findDisplayNamesById(playerIdsFrom(giftableStatuses));

        for (GiftableStatus giftableStatus : giftableStatuses) {
            giftableStatus.setUrlForImage(String.format("%s/api/v1.0/player/picture?playerid=%s",
                    siteConfiguration.getHostUrl(), giftableStatus.getPlayerId()));
            giftableStatus.setDisplayName(displayNames.get(giftableStatus.getPlayerId()));
        }

        return giftableStatuses;
    }

    private Set<BigDecimal> playerIdsFrom(final Set<GiftableStatus> giftableStatuses) {
        final Set<BigDecimal> playerIds = new HashSet<>();
        for (GiftableStatus giftableStatus : giftableStatuses) {
            playerIds.add(giftableStatus.getPlayerId());
        }
        return playerIds;
    }

    // This endpoint needs to be accessed with a POST rather than a GET so that the csvPlayerIds parameter is
    // sent in the request body rather than as a query parameter since csvPlayerIds can easily exceed
    // the limit on URL length
    @Deprecated
    @RequestMapping(value = "/api/1.0/gifting/status", method = RequestMethod.GET)
    public void getGiftingStatusLegacy(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final String csvPlayerIds) throws IOException {
        getGiftingStatus(request, response, csvPlayerIds);
    }

    @RequestMapping(value = "/api/1.0/gifting/send", method = RequestMethod.POST)
    public void sendGifts(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final String csvPlayerIds) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }
        final BigDecimal sender = session.getPlayerId();
        LOG.debug("send Gifts from {} to  {}", sender.toPlainString(), csvPlayerIds);


        Set<BigDecimal> recipientIds = newHashSet();

        try {
            for (String receiver : csvPlayerIds.split(",")) {
                recipientIds.add(new BigDecimal(receiver));
            }
            final Set<BigDecimal> giftIds = giftingService.giveGifts(sender, recipientIds, session.getSessionId());
            responseWriter.writeOk(response, giftIds);
        } catch (NumberFormatException e) {
            LOG.error("could not convert player Id in {} to bigdecimal", csvPlayerIds, e);
            responseWriter.writeError(response, BAD_REQUEST.value(), "could not parse player id. check log for details");
        } catch (Exception e) {
            responseWriter.writeError(response, INTERNAL_SERVER_ERROR.value(), "problem with sending gifts to recipients");
            LOG.error("sendGifts problem with sending gifts from {} to {}", sender, csvPlayerIds, e);
        }
    }

    @RequestMapping(value = "/api/1.0/gifting/send/all", method = RequestMethod.POST)
    public void sendGiftsToAll(final HttpServletRequest request,
                               final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }

        final BigDecimal sender = session.getPlayerId();
        LOG.debug("Send Gifts from {} to all friends", sender.toPlainString());

        try {
            responseWriter.writeOk(response, giftingService.giveGiftsToAllFriends(sender, session.getSessionId()));

        } catch (Exception e) {
            responseWriter.writeError(response, INTERNAL_SERVER_ERROR.value(), "Problem with sending gifts to all friends");
            LOG.error("sendGifts problem with sending gifts from {} to all friends", sender, e);
        }
    }

    @RequestMapping(value = "/api/1.1/gifting/get", method = RequestMethod.GET)
    public void getAllGiftTypes(final HttpServletRequest request,
                                final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }

        List<AppToUserGift> appToUserGifts = newArrayList();
        try {
            LOG.info("getting looking up gifts for {}", session.getPlayerId());
            appToUserGifts = giftingService.getGiftingPromotions(session.getPlayerId());
        } catch (Exception e) { // if bi-promotion isn't available
            LOG.error("couldn't access giftingAccessPromoService defaulting to no gifts", e);
        }
        final Set<Gift> userToUserGifts = giftingService.getAvailableGifts(session.getPlayerId());
        AllGifts allGifts = new AllGifts(appToUserGifts, userToUserGifts);
        responseWriter.writeOk(response, allGifts);
    }

    @RequestMapping(value = "/api/1.0/gifting/get", method = RequestMethod.GET)
    public void getGifts(final HttpServletRequest request,
                         final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }
        final BigDecimal sender = session.getPlayerId();
        LOG.debug("getting Gifts for playerId {}", sender);

        try {
            final Set<Gift> gifts = giftingService.getAvailableGifts(sender);
            responseWriter.writeOk(response, gifts);
        } catch (Exception e) {
            LOG.error("getAvailableGifts problem with getting gifts for player", sender, e);
            responseWriter.writeError(response, BAD_REQUEST.value(), "could not parse player id. check log for details");
        }
    }

    private boolean isInvalidSession(final HttpServletResponse response, final LobbySession session) throws IOException {
        if (session == null) {
            LOG.warn("could not load session");
            responseWriter.writeError(response, UNAUTHORIZED.value(), "no session");
            return true;
        }
        return false;
    }

    @RequestMapping(value = "/api/1.0/gifting/acknowledge", method = RequestMethod.POST)
    public void acknowledgeExpiredGifts(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final String csvGiftIds) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }

        LOG.debug("acknowledging Gift IDs {} for {}", csvGiftIds, session.getPlayerId());
        try {
            Set<BigDecimal> result = newHashSet();
            final List<String> split = asList(csvGiftIds.split(","));
            for (String giftIdAsString : split) {
                result.add(new BigDecimal(giftIdAsString));
            }
            giftingService.acknowledgeViewedGifts(session.getPlayerId(), result);
            responseWriter.writeOk(response, emptyMap());

        } catch (Exception e) {
            LOG.warn("acknowledgeExpiredGifts problem: giftIds {}, {}", csvGiftIds, e);
            responseWriter.writeError(response, BAD_REQUEST.value(), "could not parse gift ids. check log for details");
        }

    }

    @RequestMapping(value = "/api/1.0/gifting/collectAppToUser/{promoId}", method = RequestMethod.POST)
    public void collectAppToUserGift(@PathVariable final String promoId,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }

        final BigDecimal playerId = session.getPlayerId();
        try {
            if (giftingService.logPlayerReward(playerId, Long.parseLong(promoId), session.getSessionId())) {
                communityService.asyncPublishBalance(playerId);

                responseWriter.writeOk(response, of("result", "success"));
            } else {
                responseWriter.writeError(response, FORBIDDEN.value(),
                        "gift collection failure: either the gift is already collected or it has expired");
            }
        } catch (Exception e) {
            LOG.error("Problem with collecting gift with promoId {}", e);
            responseWriter.writeError(response, BAD_REQUEST.value(), format("could not collect app to user gift with promoId '%s'", promoId));
        }
    }

    @RequestMapping(value = "/api/1.0/gifting/collect/{giftId}/{giftType}", method = RequestMethod.POST)
    public void collectGift(@PathVariable String giftId,
                            @PathVariable String giftType,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }
        final BigDecimal playerId = session.getPlayerId();
        try {
            BigDecimal reward = giftingService.collectGift(playerId, new BigDecimal(giftId), CollectChoice.valueOf(giftType), session.getSessionId());
            responseWriter.writeOk(response, new RewardResult(reward));
        } catch (GiftCollectionFailure e) {
            LOG.warn("gift collection failure for giftId {} with CollectChoice {} for player {}. inform client teams of:{}",
                    giftId, giftType, playerId, e.getCollectionResult().name());
            responseWriter.writeError(response,
                    UNAUTHORIZED.value(),
                    "gift collection failure: " + e.getCollectionResult().name());
        } catch (Exception e) {
            LOG.error("Problem with collecting gift {} or CollectChoice {} for player {}, error: {}",
                    giftId, giftType, playerId, e);
            responseWriter.writeError(response, BAD_REQUEST.value(), "could not parse params. check log for details");
        }
    }

    @RequestMapping(value = "/api/1.0/gifting/endOfGiftingPeriod", method = RequestMethod.GET)
    public void getEndOfGiftingPeriod(HttpServletResponse response) throws IOException {
        DateTime endOfGiftPeriod = giftingService.getEndOfGiftPeriod();
        responseWriter.writeOk(response, new GiftingPeriod(endOfGiftPeriod));
    }

    @RequestMapping(value = "/api/1.0/gifting/pushPlayerCollectionStatus", method = RequestMethod.GET)
    public void pushPlayerCollectionStatusToClient(HttpServletRequest request,
                                                   HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (isInvalidSession(response, session)) {
            return;
        }
        final BigDecimal playerId = session.getPlayerId();
        LOG.debug("pushing player collection status to client {}", playerId);
        try {
            responseWriter.writeOk(response, giftingService.pushPlayerCollectionStatus(playerId));
        } catch (Exception e) {
            LOG.warn("problem with pushing player collection status to client for {}", playerId);
            responseWriter.writeError(response,
                    BAD_REQUEST.value(),
                    "could not convert player id to BigDecimal - check log for details");
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    static class RewardResult {

        @JsonProperty
        private final BigDecimal reward;

        public RewardResult(final BigDecimal reward) {

            this.reward = reward;
        }

        BigDecimal getReward() {
            return reward;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            RewardResult rhs = (RewardResult) obj;
            return new EqualsBuilder()
                    .append(this.reward, rhs.reward)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(reward)
                    .toHashCode();
        }
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    static class GiftingPeriod {
        private final DateTime endOfGiftingPeriod;

        DateTime getEndOfGiftingPeriod() {
            return endOfGiftingPeriod;
        }

        GiftingPeriod(final DateTime endTime) {
            this.endOfGiftingPeriod = endTime;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            GiftingPeriod rhs = (GiftingPeriod) obj;
            return new EqualsBuilder()
                    .append(this.endOfGiftingPeriod, rhs.endOfGiftingPeriod)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(endOfGiftingPeriod)
                    .toHashCode();
        }
    }
}
