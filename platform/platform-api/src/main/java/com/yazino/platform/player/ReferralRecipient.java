package com.yazino.platform.player;

import com.yazino.platform.invitation.InvitationSource;

import java.io.Serializable;

public interface ReferralRecipient extends Serializable {
    InvitationSource getInvitationSource();

    String getRecipientIdentifier();
}
