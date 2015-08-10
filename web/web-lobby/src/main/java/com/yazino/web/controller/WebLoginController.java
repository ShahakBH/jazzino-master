package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.form.WebLoginForm;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.Boolean.FALSE;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Controller
public class WebLoginController {
    private static final Logger LOG = LoggerFactory.getLogger(WebLoginController.class);
    public static final String JS_REDIRECT_TARGET = "jsRedirectLocation";
    public static final String LOGIN_VIEW = "login";
    public static final String REDIRECT_TO = "redirectTo";
    public static final String PARTIAL = "partial";

    private final LobbySessionCache lobbySessionCache;
    private final CookieHelper cookieHelper;
    private final RegistrationHelper registrationHelper;
    private final YazinoWebLoginService yazinoWebLoginService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired(required = true)
    public WebLoginController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("cookieHelper") final CookieHelper cookieHelper,
            final YazinoWebLoginService yazinoWebLoginService,
            final RegistrationHelper registrationHelper, final YazinoConfiguration yazinoConfiguration) {
        this.registrationHelper = registrationHelper;
        this.lobbySessionCache = lobbySessionCache;
        this.cookieHelper = cookieHelper;
        this.yazinoWebLoginService = yazinoWebLoginService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @RequestMapping(value = {"/login", "/public/login"}, method = RequestMethod.GET)
    public ModelAndView login(
            final ModelMap model,
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = REDIRECT_TO, required = false) final String redirectTo,
            @RequestParam(value = PARTIAL, defaultValue = "false", required = false) final boolean partial,
            @RequestParam(value = "game", defaultValue = "slots", required = false) final String gameTypeShortName)
            throws IOException {

        final WebLoginForm form = new WebLoginForm();

        if (StringUtils.isNotBlank(redirectTo)) {
            form.setRedirectTo(redirectTo);
            cookieHelper.setRedirectTo(response, redirectTo);
        }

        form.setRegistered("existing-user");
        form.setOptIn(FALSE);

        if (lobbySessionCache.getActiveSession(request) != null) {

            final String location = redirectFor(form.getRedirectTo());
            model.addAttribute(JS_REDIRECT_TARGET, location);
            model.addAttribute(PARTIAL, "true");
            return new ModelAndView("partials/loginRedirection", model);
        }

        model.addAttribute("game", gameTypeShortName);
        model.addAttribute("loginForm", form);

        if (partial) {
            model.addAttribute(PARTIAL, partial);
            return new ModelAndView("partials/loginPanel", model);
        }

        return new ModelAndView(LOGIN_VIEW, model);
    }

    @RequestMapping(value = {"/login", "/public/login"}, method = RequestMethod.POST)
    public String processSubmit(@ModelAttribute("loginForm") final WebLoginForm form,
                                final BindingResult result,
                                final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ModelMap model,
                                @RequestParam(value = PARTIAL, defaultValue = "false", required = false) final String partial) {

        if (lobbySessionCache.getActiveSession(request) != null) {
            return redirectTo(redirectFor(form.getRedirectTo()), response);
        }
        Partner partner = Partner.YAZINO;
        final String successfulLoginView = getDefaultedLoginView(form);
        final String resultView;

        final boolean existingUser = form.getRegistered() == null || form.getRegistered().equals("existing-user");
        cleanOptInCheckboxValue(form);

        if (existingUser) {
            resultView = loginExistingUser(form, request, response, model, successfulLoginView, partner);
        } else {

            final String gameType = getGameTypeFromRequestModelOrCookie(request, model);
            resultView = registerNewUser(form, result, request, response, successfulLoginView, gameType, partner);
        }

        if (successfulLoginView.equals(resultView)) {
            model.addAttribute(JS_REDIRECT_TARGET, resultView);
            model.addAttribute(PARTIAL, "true");
            return "partials/loginRedirection";
        }

        if ("true".equalsIgnoreCase(partial)) {
            model.addAttribute(PARTIAL, partial);
            return "partials/loginPanel";
        }

        return LOGIN_VIEW;
    }

    private String registerNewUser(final WebLoginForm form,
                                   final BindingResult result,
                                   final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final String successfulLoginView,
                                   final String gameType,
                                   final Partner partnerId) {

        final RegistrationResult registerResult = registrationHelper.register(form,
                result,
                request,
                response,
                Platform.WEB,
                gameType,
                partnerId);

        if (registerResult == RegistrationResult.SUCCESS) {
            return successfulLoginView;
        }
        return LOGIN_VIEW;
    }

    private String loginExistingUser(final WebLoginForm form,
                                     final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final ModelMap model,
                                     final String successfulLoginView,
                                     final Partner partnerId) {
        final LoginResult loginResult = yazinoWebLoginService.login(request, response,
                form.getEmail(), form.getRegisteredPassword(), Platform.WEB, partnerId);

        switch (loginResult) {
            case BLOCKED:
                model.put("loginError", "Your account has been blocked, please contact customer support.");
                return "blocked";

            case FAILURE:
                model.put("loginError", "Your username and/or password were incorrect.");
                model.put("redirectTo", redirectFor(successfulLoginView));
                return "login";

            default:
                return successfulLoginView;
        }
    }

    private void cleanOptInCheckboxValue(final WebLoginForm form) {
        if (form.getOptIn() == null) {
            form.setOptIn(false);
        }
    }

    private String redirectTo(final String location, final HttpServletResponse response) {
        if (!response.isCommitted()) {
            try {
                response.sendRedirect(location);

            } catch (IOException e) {
                LOG.info("Unable to send login redirect to {}", location, e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return null;
            }
        }
        return null;
    }

    private String getDefaultedLoginView(final WebLoginForm form) {
        String successfulLoginView = "/welcome";
        if (StringUtils.isNotBlank(form.getRedirectTo())) {
            successfulLoginView = form.getRedirectTo();
        }
        return successfulLoginView;
    }

    public String redirectFor(final String redirectTo) {
        if (isBlank(redirectTo)) {
            return "/";
        } else {
            return redirectTo;
        }
    }

    private String getGameTypeFromRequestModelOrCookie(final HttpServletRequest request,
                                                       final ModelMap model) {
        String gameType = (String) model.get("gameType");

        if (isBlank(gameType)) {
            gameType = request.getParameter("gameType");
            if (isBlank(gameType)) {
                gameType = cookieHelper.getLastGameType(request.getCookies(), null);
            } else {
                if (gameType.endsWith("/")) {
                    gameType = gameType.substring(0, gameType.length() - 1);
                }
                gameType = gameType.toUpperCase();
            }
        }

        return gameType;
    }

}
