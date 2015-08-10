package com.yazino.platform.invitation.emailService;

import java.util.HashMap;
import java.util.Map;

public class EmailInvitation {
    public static final String INVITE_FRIENDS_TEMPLATE = "lobby/invite-friends.vm";
    private static final String INVITE_FRIENDS_SUBJECT_TEMPLATE = "%s has invited you to play at Yazino";
    private static final String USER_NAME_KEY = "userName";
    private static final String MESSAGE_KEY = "message";
    private static final String TARGET_URL_KEY = "targetUrl";

    private final Map<String, Object> properties = new HashMap<String, Object>();
    private final String subject;
    private final String playerName;

    public EmailInvitation(final String message,
                           final String callToActionUrl,
                           final String playerName) {
        this.playerName = playerName;
        properties.put(MESSAGE_KEY, message);
        properties.put(TARGET_URL_KEY, callToActionUrl);
        properties.put(USER_NAME_KEY, playerName);
        subject = String.format(INVITE_FRIENDS_SUBJECT_TEMPLATE, playerName);
    }

    public String getSender(String sender) {
        if (sender.indexOf("<") > 0) {
            final String email = sender.split("<")[1];
            final int end;
            if (email.lastIndexOf(">") > 0) {
                end = email.lastIndexOf(">");
            } else {
                end = email.length();
            }
            return String.format("%s <%s>", playerName, email.substring(0, end));

        }
        return String.format("%s <%s>", playerName, sender);
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

}
