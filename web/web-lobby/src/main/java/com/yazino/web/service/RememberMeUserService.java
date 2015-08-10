package com.yazino.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * An implementation of UserDetailsService which returns user details required by the Spring RememberMeService.
 */
@Service("rememberMeUserService")
public class RememberMeUserService implements UserDetailsService {

    /**
     * Token handler for retrieving users' remember me token.
     */
    private final RememberMeTokenHandler rememberMeTokenHandler;

    @Autowired(required = true)
    public RememberMeUserService(
            @Qualifier("rememberMeTokenHandler") final RememberMeTokenHandler rememberMeTokenHandler) {
        this.rememberMeTokenHandler = rememberMeTokenHandler;
    }

    @Override
    public UserDetails loadUserByUsername(final String s) throws UsernameNotFoundException, DataAccessException {

        // Extract the user profile id from the username field of the remember me cookie...
        final RememberMeUserInfo info = new RememberMeUserInfo(s);
        final BigDecimal userProfileId = info.getPlayerId();

        final String token = rememberMeTokenHandler.getTokenForUser(userProfileId);
        if (token == null) {
            throw new UsernameNotFoundException("User ID " + userProfileId + " is not valid for auto-login.");
        }

        return new User(s, token, true, true, true, true, new ArrayList<GrantedAuthority>());
    }

}
