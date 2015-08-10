package strata.server.work.invitations.publisher;

import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class FitnesseInvitationService implements InvitationService {

    @Override
    public void sendEmailInvitation(BigDecimal issuingPlayerId, String issuingPlayerName, String recipientEmail, String customisedMessage, String callToActionUrl, DateTime requestTime, String gameType, String screenSource) {
    }

    @Override
    public void invitationSent(final BigDecimal issuingPlayerId, final String recipientIdentifier,
                               final InvitationSource source, final DateTime createdTime, final String gameType,
                               final String screenSource) {
        // no implementation required at present
    }

    @Override
    public void invitationAccepted(String recipientIdentifier, InvitationSource source, DateTime registrationTime,
                                   BigDecimal recipientPlayerId) {
        // no implementation required at present
    }

}
