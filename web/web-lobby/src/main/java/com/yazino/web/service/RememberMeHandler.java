package com.yazino.web.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * Handles creating, updating and retrieving information from a Remember Me cookie for auto-login.
 */
@Service("rememberMeHandler")
public class RememberMeHandler {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RememberMeHandler.class);

    /**
     * Lobby Session Factory to instantiate new sessions for users on login.
     */
    private final LobbySessionFactory lobbySessionFactory;

    /**
     * Spring Security's service for handling remember me cookies.
     */
    private final RememberMeServices rememberMeService;

    /**
     * Token handler for generating remember me passwords.
     */
    private final RememberMeTokenHandler rememberMeTokenHandler;

    private final String rememberMeKey;
    private final String rememberMeCookieName;


    @Autowired
    public RememberMeHandler(@Qualifier("rememberMeServices") final RememberMeServices rememberMeService,
                             @Qualifier("rememberMeTokenHandler") final RememberMeTokenHandler rememberMeTokenHandler,
                             final LobbySessionFactory lobbySessionFactory,
                             @Value("${strata.web.rememberme.key}") final String rememberMeKey,
                             @Value("${strata.web.rememberme.cookieName}") final String rememberMeCookieName) {
        this.rememberMeService = rememberMeService;
        this.rememberMeTokenHandler = rememberMeTokenHandler;
        this.lobbySessionFactory = lobbySessionFactory;
        this.rememberMeKey = rememberMeKey;
        this.rememberMeCookieName = rememberMeCookieName;
    }

    /**
     * Creates or updates a 'Remember Me' cookie so that the user can be auto-logged in on a subsequent visit to
     * the application. Note that the cookie will only be set if the remember me parameter on the request is set to
     * true. The name of the parameter is configurable, but defaults to _spring_security_remember_me.
     *
     * @param partnerId The partner through which the user started their session (e.g. Play For Fun, Facebook...)
     * @param platform  The platform that the user is playing on (e.g. Yazino Web, Facebook, Mobile...)
     * @param playerId  The identifier of the user's profile.
     * @param username  The username / identifier that was used to log in.
     * @param request   The HTTP request. This must have the 'remember me' parameter set. (The parameter is configured
     *                  against the RememberMeServices bean in the Spring config xml).
     * @param response  The HTTP response. This will be updated with the remember me cookie.
     */
    public void storeRememberMeCookie(final Partner partnerId,
                                      Platform platform,
                                      final BigDecimal playerId,
                                      final String username,
                                      final HttpServletRequest request,
                                      final HttpServletResponse response) {

        // Extract the details required to create a new UserDetails object
        if (rememberMeKey == null) {
            LOG.error("The Remember Me key has not been configured. Cookie will not be created.");
            return;
        }

        // Because all we have to go on when we come back in again is a username and password, we'll store
        // multiple pieces of information in the username, separated by tabs.
        // The actual username might just be an ID (e.g. facebook) or an email address, depending on the platform.

        final RememberMeUserInfo userInfo = new RememberMeUserInfo(partnerId, platform, playerId, username);

        final String cookiePassword = rememberMeTokenHandler.generateTokenForUser(playerId);

        if (cookiePassword == null) {
            LOG.error("Remember Me token was not found for user " + playerId + ". Cookie will not be created.");
            return;
        }

        // Create a user details. Note we currently don't require any specific granted authorisations for the users.
        final UserDetails principal = new User(userInfo.toString(), cookiePassword, true,
                true, true, true, new ArrayList<SimpleGrantedAuthority>());

        // Now generate a token-based remember me cookie, using our key.
        final Authentication auth = new RememberMeAuthenticationToken(
                rememberMeKey, principal, new ArrayList<SimpleGrantedAuthority>());
        rememberMeService.loginSuccess(request, response, auth);
    }

    /**
     * Checks for the presence of a 'remember me' cookie on the request and if found (and validated)
     * performs an auto-login, recreating the user's lobby session.
     *
     * @param request          The HTTP Servlet Request
     * @param response         The HTTP Servlet Response
     * @param clientContextMap context map.
     * @return The newly created LobbySession if auto-login was successful, or null otherwise.
     */
    public LobbySession attemptAutoLogin(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Map<String, Object> clientContextMap,
                                         final String gameType) {

        // Attempt to auto-login using cookie if present.
        final Authentication auth = rememberMeService.autoLogin(request, response);

        if (auth != null) {
            // Recreate the lobby session
            final UserDetails user = (UserDetails) auth.getPrincipal();

            final RememberMeUserInfo userInfo = new RememberMeUserInfo(user.getUsername());
            if (userInfo.getPlayerId() == null) {
                return null;
            }

            LobbySession newSession = lobbySessionFactory.registerAuthenticatedSession(request,
                    response,
                    userInfo.getPartnerId(),
                    userInfo.getPlayerId(),
                    LoginResult.EXISTING_USER, true,
                    userInfo.getPlatform(), clientContextMap, gameType);

            if (newSession != null) {
                request.setAttribute("sessionRecreatedViaRememberMe", Boolean.TRUE);
            }
            return newSession;
        }
        return null;
    }

    /**
     * Removes any Remember Me cookie. User will no longer be auto-logged-in when returning to the site.
     *
     * @param response The HTTP Servlet response. This will be updated with the removed cookie.
     */
    public void forgetMe(final HttpServletResponse response) {
        final Cookie rmCookie = new Cookie(rememberMeCookieName, null);
        rmCookie.setMaxAge(0);
        rmCookie.setPath("/");
        response.addCookie(rmCookie);
    }

}
