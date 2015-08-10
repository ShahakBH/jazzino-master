package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;

/**
 * Builds a {@link com.yazino.web.domain.email.EmailRequest} to reset a users password.
 */
public class ResetPasswordEmailBuilder extends AbstractEmailBuilder {

    private static final String SUBJECT = "Hi %s, your Yazino password update";
    private static final String TEMPLATE = "reset-password.vm";

    public ResetPasswordEmailBuilder(final String email,
                                     final String name,
                                     final String password) {
        setTemplateProperty("realName", name);
        setTemplateProperty("password", password);
        setOtherProperty("email", email);
    }

    public String getName() {
        return (String) getTemplateProperty("realName");
    }

    public String getPassword() {
        return (String) getTemplateProperty("password");
    }

    public String getEmail() {
        return (String) getOtherProperty("email");
    }

    @Override
    public EmailRequest buildRequest(final PlayerProfileService profileService) {
        final String subject = String.format(SUBJECT, getTemplateProperty("realName"));
        return new EmailRequest(TEMPLATE, subject, getTemplateProperties(),(String) getOtherProperty("email"));
    }
}
