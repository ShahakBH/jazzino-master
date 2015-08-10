package com.yazino.web.service;

public class FacebookOAuthException extends RuntimeException {
    private static final long serialVersionUID = 3853636985585146779L;

    private final String facebookType;
    private final String facebookMessage;

    public FacebookOAuthException(final String facebookType,
                                  final String facebookMessage) {
        super(String.format("Facebook OAuth Error: Type: %s; Message: %s", facebookType, facebookMessage));

        this.facebookType = facebookType;
        this.facebookMessage = facebookMessage;
    }

    public String getFacebookType() {
        return facebookType;
    }

    public String getFacebookMessage() {
        return facebookMessage;
    }
}
