package com.yazino.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Component("inviteFriendsTracking")
public class InviteFriendsTracking {
    private final TrackingService trackingService;

    private static final String SUCCESSFUL_INVITE_FRIENDS_EVENT = "Successful Invite Friends";

    @Autowired
    public InviteFriendsTracking(final TrackingService trackingService) {
        notNull(trackingService, "trackingService is null");
        this.trackingService = trackingService;
    }

    public void trackSuccessfulInviteFriends(final BigDecimal playerId,
                                             final InvitationType type,
                                             final int numberOfInvitations) {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("type", type.toString());
        properties.put("numOfInvitations", Integer.toString(numberOfInvitations));
        trackingService.trackEvent(null, playerId, SUCCESSFUL_INVITE_FRIENDS_EVENT, properties);
    }

    public enum InvitationType {
        FACEBOOK,
        EMAIL
    }

}
