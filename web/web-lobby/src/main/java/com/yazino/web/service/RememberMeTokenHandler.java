package com.yazino.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Generates, stores and retrieves randomly-generated tokens that are assigned to users for the 'Remember Me' feature.
 * The token acts as a 'salt' which contributes to the hashed value stored in the 'password' field of the remember me
 * cookie.
 */
@Service("rememberMeTokenHandler")
public class RememberMeTokenHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RememberMeTokenHandler.class);

    /**
     * The static value that we use for all tokens.
     */
    private String staticToken;

    @Autowired
    public RememberMeTokenHandler(@Value("${strata.web.rememberme.tokenValue}") final String rememberMeToken) {
        staticToken = rememberMeToken;

        if (staticToken == null) {
            LOG.warn("Environment has not been configured with a RememberMeToken value. Remember Me functionality "
                    + "will not work without this");
        }
    }

    /**
     * Generates a token for a given user to authenticate with as part of the Remember Me feature.
     *
     * @param playerId The user ID to generate a token for.
     * @return The generated token.
     */
    public String generateTokenForUser(final BigDecimal playerId) {
        return staticToken;
    }

    /**
     * Retrieves the previously generated token for a given user, or null if no such token exists or if the token
     * has expired.
     *
     * @param playerId The user to retrieve the token for.
     * @return The token for the given user, or null if no token exists.
     */
    public String getTokenForUser(final BigDecimal playerId) {
        return staticToken;
    }

}
