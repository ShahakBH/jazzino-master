package com.yazino.platform.invitation.emailService;

import com.yazino.email.EmailException;
import com.yazino.platform.email.AsyncEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.Validate.notNull;

@Service("acceptedFriendsEmailService")
public class AcceptedInviteFriendsEmailServiceImpl implements AcceptedInviteFriendsEmailService {

    public static final String ACCEPTED_INVITE_EMAIL_TEMPLATE = "platform/AcceptedInviteConfirmationEmail.vm";
    public static final String ACCEPTED_INVITE_SUBJECT_TEMPLATE = "Your Invitation was Accepted";

    private final AsyncEmailService emailService;

    @Autowired(required = true)
    public AcceptedInviteFriendsEmailServiceImpl(final AsyncEmailService emailService) {
        notNull(emailService, "emailService may not be null");

        this.emailService = emailService;
    }

    @Override
    public Boolean sendInviteFriendsAcceptedEmail(final String name,
                                                  final String email,
                                                  final String inviteeFirstName,
                                                  final String sender
    ) throws EmailException {

        final Map<String, Object> templateProperties = newHashMap();
        templateProperties.put("name", name);
        templateProperties.put("inviteeFirstName", inviteeFirstName);

        emailService.send(email, sender, ACCEPTED_INVITE_SUBJECT_TEMPLATE,
                ACCEPTED_INVITE_EMAIL_TEMPLATE, templateProperties);

        return true;
    }


}
