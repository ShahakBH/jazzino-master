package com.yazino.web.controller;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.spring.mvc.ExternalRedirectView;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.GameTypeMapper;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.session.LobbySessionCache;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CONNECT;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@Controller
public class MarketingURLController {
    private static final Logger LOG = LoggerFactory.getLogger(MarketingURLController.class);

    private static final String BASE_URL = "/fb/";
    private static final Pattern URL_PATTERN = Pattern.compile(BASE_URL + "([^/]+)/?([^/]*)?");
    private static final Pattern HTTP_80_PATTERN = Pattern.compile("(http://[^:]+):80(/)");
    private static final Pattern HTTPS_443_PATTERN = Pattern.compile("(https://[^:]+):443(/)");
    private static final String NO_REFERENCE = "";
    private static final int REF_GROUP = 2;
    private static final String ADGRPID = "adgrpid";
    private final GameTypeMapper gameTypeMapper = new GameTypeMapper();

    private final String adparlorurl;
    private final GameTypeRepository gameTypeRepository;
    private final SiteConfiguration siteConfiguration;
    private final FacebookConfiguration facebookConfiguration;
    private final MessageFormat loginUrlFormat;
    private final LogoutHelper logoutHelper;
    private final LobbySessionCache lobbySessionCache;

    @Autowired
    public MarketingURLController(final SiteConfiguration siteConfiguration,
                                  @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration,
                                  final GameTypeRepository gameTypeRepository,
                                  @Value("${lobby.web.marketingUrlController.adparlor.url}") final String adParlorUrl,
                                  final LogoutHelper logoutHelper,
                                  final LobbySessionCache lobbySessionCache) {
        notNull(siteConfiguration, "siteConfiguration may not be null");
        notNull(facebookConfiguration, "facebookConfiguration may not be null");
        notNull(gameTypeRepository, "gameTypeRepository may not be null");
        notNull(adParlorUrl, "adParlorUrl may not be null");
        notNull(logoutHelper, "logoutHelper may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");

        this.siteConfiguration = siteConfiguration;
        this.facebookConfiguration = facebookConfiguration;
        this.gameTypeRepository = gameTypeRepository;
        this.adparlorurl = adParlorUrl;
        this.logoutHelper = logoutHelper;
        this.lobbySessionCache = lobbySessionCache;

        loginUrlFormat = createLoginURLFormat();
    }

    @RequestMapping(value = BASE_URL + "{gameType}/", params = "ref")
    public View redirectGamePseudonymWithSocialContext(final HttpServletRequest request,
                                                       @PathVariable("gameType") final String gameType,
                                                       @RequestParam(value = "ref", required = true) final String ref) {
        return redirect(gameTypeFor(gameType), "?ref=" + ref, request);
    }

    @RequestMapping(value = BASE_URL + "/mig/{gameTypeOrAlias}")
    public View redirectGameFromCanvasToWebsite(@PathVariable("gameTypeOrAlias") final String gameTypeOrAlias,
                                                final HttpServletRequest request,
                                                final HttpServletResponse response) {
        logoutHelper.logout(request.getSession(), request, response);
        return redirect(gameTypeFor(gameTypeOrAlias), null, request);
    }

    @Deprecated
    @RequestMapping(value = BASE_URL + "{gameType}/", params = ADGRPID)
    public View redirectAdvertWithSocialContext(@RequestParam(value = ADGRPID, required = true) final String ref) {
        LOG.warn("Deprecated entry point. Marketing links using it should be fixed. ({}={})", ADGRPID, ref);
        return redirectToAdParlour(ref);
    }

    private View redirectToAdParlour(final String ref) {
        notNull(ref, "ref may not be null");

        final String loginUrl = adparlorurl + ADGRPID + "=" + ref;
        LOG.debug("Redirecting to {}", loginUrl);
        return new RedirectView(loginUrl, false, true, false) {
            @Override
            protected void sendRedirect(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final String targetUrl,
                                        final boolean http10Compatible) throws IOException {
                response.sendRedirect(targetUrl);
            }
        };
    }

    @RequestMapping(BASE_URL + "**/*")
    public View redirectGamePseudonym(final HttpServletRequest request) {
        LOG.debug("Redirecting to game pseudonym with path {}", request.getPathInfo());
        final Matcher matcher = parse(request.getPathInfo());
        return redirect(gameTypeFor(pseudonymFrom(matcher)), parametersFrom(matcher), request);
    }

    @RequestMapping(value = {BASE_URL + "/splash/{gameTypeOrAlias}", BASE_URL + "/splash/{gameTypeOrAlias}/"})
    public ModelAndView showFacebookSplash(@PathVariable("gameTypeOrAlias") final String gameTypeOrAlias) {
        final FacebookAppConfiguration appConfig = facebookConfiguration.getAppConfigFor(
                gameTypeFor(gameTypeOrAlias), CANVAS, LOOSE);
        return new ModelAndView("fbredirect/fanPage")
                .addObject("gameType", gameTypeFor(gameTypeOrAlias))
                .addObject("targetUrl", appConfig.getRedirectUrl());
    }

    private String pseudonymFrom(final Matcher matcher) {
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return siteConfiguration.getDefaultGameType();
    }

    private Matcher parse(final String pathInfo) {
        return URL_PATTERN.matcher(pathInfo);
    }

    private String parametersFrom(final Matcher matcher) {
        if (matcher.matches()) {
            final String ref = matcher.group(REF_GROUP);
            if (StringUtils.isNotBlank(ref)) {
                return "?ref=" + ref;
            }
        }
        return NO_REFERENCE;
    }

    private String encode(final String ref) {
        if (ref == null) {
            return "";
        }

        try {
            return URLEncoder.encode(ref, "UTF8");

        } catch (UnsupportedEncodingException e) {
            LOG.error("The JVM doesn't appear to support UTF8", e);
            return ref;
        }
    }

    private String gameTypeFor(final String pseudonym) {
        for (GameTypeInformation gameTypeInformation : gameTypeRepository.getGameTypes().values()) {
            LOG.debug("Available pseudonyms are: {}", gameTypeInformation.getGameType().getPseudonyms());
            if (gameTypeInformation.getGameType().getId().equals(pseudonym)
                    || gameTypeInformation.getGameType().getPseudonyms().contains(pseudonym)) {
                return gameTypeInformation.getId();
            }
        }
        return siteConfiguration.getDefaultGameType();
    }

    private View redirect(final String gameType,
                          final String urlParameters,
                          final HttpServletRequest request) {
        notNull(gameType, "gameType may not be null");

        if (lobbySessionCache.getActiveSession(request) != null) {
            LOG.debug("User has an existing session, redirecting to game {}", gameType);
            return new RedirectView(gameUrlFor(gameType), true, true, false);

        } else {
            final String loginUrl = loginUrlFormat.format(
                    new Object[]{facebookAppIdFor(gameType), gameType, encode(urlParameters)});
            LOG.debug("Redirecting to {}", loginUrl);

            return new ExternalRedirectView(loginUrl);
        }
    }

    private String gameUrlFor(final String gameType) {
        final String viewName = gameTypeMapper.getViewName(gameType);
        if (viewName != null) {
            return "/" + viewName;
        }

        return "/" + gameType;
    }

    private MessageFormat createLoginURLFormat() {
        if (facebookConfiguration.getLoginUrl() == null) {
            throw new IllegalStateException("No Facebook login URL is configured");
        }

        String loginUrl = facebookConfiguration.getLoginUrl();

        loginUrl = HTTP_80_PATTERN.matcher(loginUrl).replaceAll("$1$2");
        loginUrl = HTTPS_443_PATTERN.matcher(loginUrl).replaceAll("$1$2");

        return new MessageFormat(loginUrl);
    }

    private String facebookAppIdFor(final String gameType) {
        final FacebookAppConfiguration appConfig = facebookConfiguration.getAppConfigFor(gameType, CONNECT, LOOSE);
        if (appConfig == null) {
            throw new IllegalStateException("No configuration is available for game type " + gameType);
        }

        final String applicationId = appConfig.getApplicationId();
        if (applicationId == null) {
            throw new IllegalStateException("No application ID is available for game type " + gameType);
        }

        return applicationId;
    }
}
