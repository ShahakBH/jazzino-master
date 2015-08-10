package com.yazino.web.service;

import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.yazino.web.service.InviteFriendsTracking.InvitationType.FACEBOOK;

@Service
public class InvitationLobbyFacebookService {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationLobbyFacebookService.class);

    private final InvitationService invitationService;
    private final InviteFriendsTracking inviteFriendsTracking;

    @Autowired
    public InvitationLobbyFacebookService(final InvitationService invitationService,
                                          final InviteFriendsTracking inviteFriendsTracking) {
        this.invitationService = invitationService;
        this.inviteFriendsTracking = inviteFriendsTracking;
    }

    public void trackInvitesSent(final BigDecimal playerId,
                                 final String gameType,
                                 final String source,
                                 final String... facebookRequestIds) {
        if (facebookRequestIds == null || facebookRequestIds.length == 0) {
            LOG.warn("Tracking zero invites!");
            return;
        }

        inviteFriendsTracking.trackSuccessfulInviteFriends(playerId, FACEBOOK, facebookRequestIds.length);

        final DateTime invitationTime = new DateTime();

        for (String recipientId : facebookRequestIds) {
            invitationService.invitationSent(playerId,
                    recipientId,
                    InvitationSource.FACEBOOK,
                    invitationTime,
                    gameType,
                    source);
        }
    }
}
