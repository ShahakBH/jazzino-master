package com.yazino.web.parature.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfileAuthenticationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.parature.form.LoginForm;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.session.LobbySessionFactory;
import com.yazino.web.util.ClientContextConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.yazino.platform.Partner.YAZINO;

@Controller
public class LoginController {

    private final LobbySessionCache lobbySessionCache;
    private final LobbySessionFactory lobbySessionFactory;
    private final AuthenticationService authenticationService;

    @Autowired
    public LoginController(final LobbySessionCache lobbySessionCache,
                           final LobbySessionFactory lobbySessionFactory,
                           final AuthenticationService authenticationService) {
        this.lobbySessionCache = lobbySessionCache;
        this.lobbySessionFactory = lobbySessionFactory;
        this.authenticationService = authenticationService;
    }

    @RequestMapping(value = {"/strata.server.lobby.support/login", "/support/parature/login"},
            method = RequestMethod.GET)
    public String login(final ModelMap model,
                        final HttpServletRequest request) throws IOException {
        final LoginForm form = new LoginForm();
        form.setRedirectTo(request.getParameter("from"));
        model.addAttribute("loginForm", form);
        return "parature/login";
    }

    @RequestMapping(value = {"/strata.server.lobby.support/login", "/support/parature/login"},
            method = RequestMethod.POST)
    public String processSubmit(@ModelAttribute("loginForm") final LoginForm form,
                                final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ModelMap model) throws IOException {
        if (lobbySessionCache.getActiveSession(request) != null) {
            // throw exception
            return null;
        }

        final PlayerProfileAuthenticationResponse authenticationResponse = authenticationService.authenticateYazinoUser(
                form.getEmail(), form.getPassword());
        if (!authenticationResponse.isSuccessful()) {
            if (authenticationResponse.isBlocked()) {
                model.addAttribute("loginError", "Your account has been blocked. Please Contact Customer Support");
            } else {
                model.addAttribute("loginError", "Your username and/or password were incorrect.");
            }
            return "parature/login";
        }

        lobbySessionFactory.registerAuthenticatedSession(request, response,
                YAZINO, authenticationResponse.getPlayerId(),
                LoginResult.EXISTING_USER, true, Platform.WEB, ClientContextConverter.toMap(""), "");
        response.sendRedirect("/support/parature/auth");
        return null;
    }

}
