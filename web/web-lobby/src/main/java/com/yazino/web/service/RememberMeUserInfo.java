package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;

/**
 * User attributes that we store within the 'username' field of the remember me cookie. The Spring Security RememberMe
 * framework only supports a username and password field, but we require more details than this so we store several
 * attributes together in the username field.
 */
public class RememberMeUserInfo {
    private static final Logger LOG = LoggerFactory.getLogger(RememberMeUserInfo.class);

    private static final int IDX_PARTNER_ID = 0;
    private static final int IDX_PLATFORM = 1;
    private static final int IDX_PLAYER_ID = 2;
    private static final int IDX_USERNAME = 3;

    private Partner partnerId;
    private BigDecimal playerId;
    private String username;
    private Platform platform;


    /**
     * Construct a RememberMeUserInfo from an existing cookie user string.
     *
     * @param user The user string held in the cookie. This should be in the same format as returned by
     *             RememberMeUserInfo.toString()
     */
    public RememberMeUserInfo(final String user) {

        // Split the username to extract the details we need
        final String[] tokens = user.split("\t");

        try {
            setPartnerId(Partner.parse(tokens[IDX_PARTNER_ID]));
        } catch (IllegalArgumentException e) {
            setPartnerId(Partner.YAZINO);
        }
        try {
            // this will fail if the 'old' platform (i.e. Platforms) is stored in the cookie and is other than IOS, MOBILE or ANDROID,
            // in this case we simply set platform to WEB as facebook (i.e. canvas) wasn't valid)
            platform = Platform.valueOf(tokens[IDX_PLATFORM]);
        } catch (Exception e) {
            platform = Platform.WEB;
        }

        final String tokenPlayerId = tokens[IDX_PLAYER_ID];
        if (tokenPlayerId.startsWith("P")) {
            try {
                setPlayerId(new BigDecimal(tokenPlayerId.substring(1)));
            } catch (NumberFormatException e) {
                LOG.debug("Received cookie with invalid player ID: {} from token {}; auto-login skipped",
                        tokenPlayerId.substring(1), ArrayUtils.toString(tokens));
            }
        }
        setUsername(tokens[IDX_USERNAME]);
    }

    /**
     * Create a RememberMeUserInfo, populating all the fields.
     *  @param partnerId The partner ID.
     * @param platform  The platformAndLoginMethod the user entered the system from.
     * @param playerId  The user's player ID.
     * @param username  The user's username.
     */
    public RememberMeUserInfo(final Partner partnerId,
                              final Platform platform,
                              final BigDecimal playerId,
                              final String username) {
        setPartnerId(partnerId);
        setPlayerId(playerId);
        setUsername(username);
        this.platform = platform;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final Partner partnerId) {
        this.partnerId = partnerId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setUsername(final String aUsername) {

        // For some authentication providers, the username may be a URL. Since Spring Security separates the elements
        // of the token by ':', we need to URL encode the username to ensure there are no ':' or other potentially
        // dangerous characters in it.
        try {
            username = URLEncoder.encode(aUsername, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            username = aUsername;
        }
    }

    public String toString() {
        return String.format("%s\t%s\tP%s\t%s",
                partnerId,
                platform.name(),
                playerId,
                username);
    }

}
