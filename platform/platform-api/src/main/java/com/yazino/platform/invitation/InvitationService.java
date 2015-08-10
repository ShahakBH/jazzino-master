package com.yazino.platform.invitation;

import org.joda.time.DateTime;

import java.math.BigDecimal;

public interface InvitationService {

    void sendEmailInvitation(BigDecimal issuingPlayerId,
                             String issuingPlayerName,
                             String recipientEmail,
                             String customisedMessage,
                             String callToActionUrl, DateTime requestTime,
                             String gameType,
                             String screenSource);

    void invitationSent(BigDecimal issuingPlayerId,
                        String recipientIdentifier,
                        InvitationSource source,
                        DateTime createdTime,
                        String gameType,
                        String screenSource);

    void invitationAccepted(String recipientIdentifier,
                            InvitationSource source,
                            DateTime registrationTime,
                            BigDecimal recipientPlayerId);

}
