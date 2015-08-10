package com.yazino.platform.invitation.message;

public enum InvitationMessageType  {

    INVITATION_SENT,
    INVITATION_ACCEPTED,
    INVITATION_NOT_ACCEPTED,
    EMAIL_INVITATION_REQUESTED;

    @Override
    public String toString() {
        return this.name();
    }
}
