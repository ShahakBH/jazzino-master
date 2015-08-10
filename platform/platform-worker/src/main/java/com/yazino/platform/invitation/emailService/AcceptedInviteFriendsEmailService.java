package com.yazino.platform.invitation.emailService;

import com.yazino.email.EmailException;

public interface AcceptedInviteFriendsEmailService {


    Boolean sendInviteFriendsAcceptedEmail(final String name,
                                           final String email,
                                           final String inviteeFirstName,
                                           final String sender) throws EmailException;

}
