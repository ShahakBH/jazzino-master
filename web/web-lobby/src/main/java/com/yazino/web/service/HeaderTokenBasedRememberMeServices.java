package com.yazino.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An extension of the {@link TokenBasedRememberMeServices} to allow the token to reside
 * in the headers rather than as a cookie. This is because the Flex HTTP client is rubbish.
 */
@Service("rememberMeServices")
public class HeaderTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    @Autowired
    public HeaderTokenBasedRememberMeServices(@Value("${strata.web.rememberme.key}") final String key,
                                              @Value("${strata.web.rememberme.cookieName}") final String cookieName,
                                              final UserDetailsService userDetailsService) {
        super(key, userDetailsService);

        setCookieName(cookieName);
    }

    @Override
    protected void setCookie(final String[] tokens,
                             final int maxAge,
                             final HttpServletRequest request,
                             final HttpServletResponse response) {
        super.setCookie(tokens, maxAge, request, response);

        response.setHeader(getCookieName(), encodeCookie(tokens));
    }

    @Override
    protected String extractRememberMeCookie(final HttpServletRequest request) {
        final String cookieValue = super.extractRememberMeCookie(request);
        if (cookieValue != null) {
            return cookieValue;
        }

        return request.getHeader(getCookieName());
    }
}
