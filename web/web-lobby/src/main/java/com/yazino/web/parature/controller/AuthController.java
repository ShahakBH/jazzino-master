package com.yazino.web.parature.controller;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.parature.service.ParatureSupportUserService;
import com.yazino.web.parature.service.SupportUserServiceException;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Controller
public class AuthController {
    private final LobbySessionCache lobbySessionCache;
    private final PlayerProfileService playerProfileService;
    private final ParatureSupportUserService supportUserService;
    private final String adminPassword;
    private final String adminEmail;
    private final String target;
    private final String loggedOutLink;

    @Autowired
    public AuthController(final LobbySessionCache lobbySessionCache,
                          final PlayerProfileService playerProfileService,
                          final ParatureSupportUserService supportUserService,
                          @Value("${parature.service.adminpassword}") final String adminPassword,
                          @Value("${parature.service.adminemail}") final String adminEmail,
                          @Value("${parature.service.signintarget}") final String target,
                          @Value("${external.support.loggedout.link}") final String loggedOutLink) {
        this.lobbySessionCache = lobbySessionCache;
        this.playerProfileService = playerProfileService;
        this.supportUserService = supportUserService;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.target = target;
        this.loggedOutLink = loggedOutLink;
    }

    @RequestMapping({"/strata.server.lobby.support/auth", "/support/parature/auth"})
    public String view(final ModelMap modelMap,
                       final HttpServletRequest request,
                       final HttpServletResponse response)
            throws NoSuchAlgorithmException, IOException, SupportUserServiceException {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {

            response.sendRedirect("/support/parature/login");
            return null;
        }

        final PlayerProfile userProfile = playerProfileService.findByPlayerId(lobbySession.getPlayerId());

        if (isBlank(userProfile.getEmailAddress())) {
            response.sendRedirect(loggedOutLink);
            return null;
        }

        if (!supportUserService.hasUserRegistered(lobbySession.getPlayerId())) {
            supportUserService.createSupportUser(lobbySession.getPlayerId(), userProfile);
        }

        final String email = userProfile.getEmailAddress();
        final String sessionID = lobbySession.getPlayerId() + adminPassword;

        final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
        mdEnc.update(sessionID.getBytes(), 0, sessionID.length());
        final String md5 = new BigInteger(1, mdEnc.digest()).toString(16);

        String firstName = userProfile.getFirstName();
        if (isBlank(firstName)) {
            firstName = userProfile.getDisplayName();
        }

        modelMap.put("cEmail", email);
        modelMap.put("cFname", firstName);
        modelMap.put("cLname", userProfile.getLastName());
        modelMap.put("cUname", lobbySession.getPlayerId());
        modelMap.put("sessID", md5);
        modelMap.put("adminEmail", adminEmail);
        modelMap.put("target", target);

        return "parature/auth";
    }

}
