package com.yazino.web.controller;


import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.LobbyInformation;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.LobbyInformationService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.FacebookCanvasDetection;
import com.yazino.web.util.JsonHelper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class HomeController {
    private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

    private static final String CONTENT_TYPE = "application/json";
    private static final int CACHE_EXPIRY_IN_SECONDS = 60 * 60 * 24 * 365;

    private final JsonHelper jsonHelper = new JsonHelper();

    private final LobbyInformationService lobbyInformationService;
    private final LobbySessionCache lobbySessionCache;
    private final FacebookCanvasDetection facebookCanvasDetection;
    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;

    @Autowired
    public HomeController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                          @Qualifier("lobbyInformationService") final LobbyInformationService lobbyInformationService,
                          final FacebookCanvasDetection facebookCanvasDetection,
                          final CookieHelper cookieHelper,
                          final SiteConfiguration siteConfiguration) {
        notNull(lobbySessionCache, "lobbySessionCache is null");
        notNull(lobbyInformationService, "LobbyInformation Service is null");
        notNull(cookieHelper, "cookieHelper is null");
        notNull(siteConfiguration, "siteConfiguration is null");

        this.lobbySessionCache = lobbySessionCache;
        this.lobbyInformationService = lobbyInformationService;
        this.facebookCanvasDetection = facebookCanvasDetection;
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
    }

    @RequestMapping(value = {"/maintenance", "/maintenance/*", "/public/maintenance", "/public/maintenance/*"})
    public ModelAndView redirectToHome(final HttpServletRequest request, HttpServletResponse response) {
        return processHome(request, response);
    }

    @RequestMapping(value = {"/", "/index", "/public", "/public/verify"})
    public ModelAndView processHome(final HttpServletRequest request, HttpServletResponse response) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession != null) {
            if (facebookCanvasDetection.isOnCanvas(request) && facebookCanvasDetection.redirectionEnabled()) {
                return facebookCanvasDetection.createRedirection(request, response);
            }
            return new ModelAndView(new RedirectView(asGameUrl(currentGameType(request)), true, true, false));
        }
        return new ModelAndView("home");
    }

    private String currentGameType(final HttpServletRequest request) {
        return cookieHelper.getLastGameType(request.getCookies(), siteConfiguration.getDefaultGameType());
    }

    @RequestMapping({"/public/lobbyInformation", "/lobbyInformation"})
    public void lobbyInformation(final HttpServletResponse response,
                                 @RequestParam(value = "gameType", required = false) final String gameType) throws IOException {
        try {
            final LobbyInformation lobbyInfo = lobbyInformationService.getLobbyInformation(gameType);
            writeJsonToResponse(response, jsonHelper.serialize(lobbyInfo));

        } catch (RemoteAccessException e) {
            LOG.error("Failed to retrieve lobby information from remote service", e);
            response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
            writeJsonToResponse(response, "{}");
        }
    }

    @RequestMapping({"/public/browserWarning", "/browserWarning", "/browser"})
    @AllowPublicAccess
    public String processBrowserWarning() {
        return "browserWarning";
    }

    @RequestMapping("/channel.html")
    public void facebookChannelFile(final HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        response.setHeader("Pragma", "public");
        response.setHeader("Cache-Control", "max-age=" + CACHE_EXPIRY_IN_SECONDS);
        final DateTime expiryDate = new DateTime().plusSeconds(CACHE_EXPIRY_IN_SECONDS);
        response.setHeader("Expires", httpDateFormat().format(expiryDate.toDate()));

        // this is a roundabout way to stop jetty overwriting the expires header
        final PrintWriter writer = response.getWriter();
        writer.write("<script src=\"//connect.facebook.net/en_US/all.js\"></script>");
        writer.close();
    }

    private SimpleDateFormat httpDateFormat() {
        final SimpleDateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return httpDateFormat;
    }

    private void writeJsonToResponse(final HttpServletResponse response,
                                     final String jsonResult) throws IOException {
        response.setContentType(CONTENT_TYPE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResult);
    }

    private String asGameUrl(final String gameType) {
        return "/" + gameType.toLowerCase().replaceAll("_", "");
    }

}
