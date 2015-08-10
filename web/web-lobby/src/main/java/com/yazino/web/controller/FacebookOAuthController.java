package com.yazino.web.controller;

import com.restfb.exception.FacebookException;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.spring.mvc.ExternalRedirectView;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.GameTypeMapper;
import com.yazino.web.domain.GameTypeResolver;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.domain.facebook.SignedRequest;
import com.yazino.web.service.*;
import com.yazino.web.util.CookieHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.web.controller.FacebookOAuthController.Source.*;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

/**
 * Facebook Canvas authentication controller
 *
 * @see "https://developers.facebook.com/docs/guides/canvas/#auth"
 * @see "https://developers.facebook.com/docs/authentication/server-side/"
 */
@Controller
public class FacebookOAuthController {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookOAuthController.class);

    enum Source {
        CONNECT,
        SECURE_CANVAS,
        INSECURE_CANVAS
    }

    private static final String CANVAS_URL = "/public/facebookLogin/";
    private static final String FACEBOOK_REAUTH_URL = "https://www.facebook.com/dialog/oauth?client_id=%s&scope=email,publish_actions&redirect_uri=%s";
    private static final String AUTH_URL =
            "https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=%s&scope=%s";
    private static final String REDIRECTION_URL = "%s://apps.facebook.com/%s/";
    private static final String GAME_LOBBY_DESTINATION = "%s/%s";
    private static final String GAME_TABLE_DESTINATION = "%s/table/find/%s";
    private final GameTypeMapper gameTypeMapper = new GameTypeMapper();

    private final FacebookConfiguration facebookConfiguration;
    private final FacebookService facebookService;
    private final String facebookPermissions;
    private final FacebookAppToUserRequestHandler fbAppRequestService;
    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;
    private final GameTypeResolver gameTypeResolver;
    private final FacebookUserInformationProvider userInformationProvider;
    private final RememberMeHandler rememberMeHandler;
    private final GameTypeRepository gameTypeRepository;
    private final GameConfigurationRepository gameConfigurationRepository;
    private final ExternalWebLoginService externalWebLoginService;

    @Autowired(required = true)
    public FacebookOAuthController(
            @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration,
            @Value("${facebook.permissions}") final String facebookPermissions,
            @Qualifier("cookieHelper") final CookieHelper cookieHelper,
            @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
            @Qualifier("gameTypeResolver") final GameTypeResolver gameTypeResolver,
            @Qualifier("facebookUserInformationProvider") final FacebookUserInformationProvider userInformationProvider,
            @Qualifier("facebookService") final FacebookService facebookService,
            @Qualifier("rememberMeHandler") final RememberMeHandler rememberMeHandler,
            final GameTypeRepository gameTypeRepository,
            final ExternalWebLoginService externalWebLoginService,
            final FacebookAppToUserRequestHandler fbAppRequestService,
            final GameConfigurationRepository gameConfigurationRepository) {
        notNull(facebookConfiguration, "facebookConfiguration may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(siteConfiguration, "siteConfiguration may not be null");
        notNull(gameTypeResolver, "gameTypeResolver may not be null");
        notNull(userInformationProvider, "facebookUserInformationProvider may not be null");
        notNull(facebookService, "facebookService may not be null");
        notNull(rememberMeHandler, "rememberMeHandler may not be null");
        notNull(gameTypeRepository, "gameTypeRepository may not be null");
        notNull(externalWebLoginService, "externalLoginService may not be null");
        notNull(fbAppRequestService, "fbAppRequestService may not be null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository may not be null");

        this.facebookConfiguration = facebookConfiguration;
        this.facebookPermissions = facebookPermissions;
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
        this.gameTypeResolver = gameTypeResolver;
        this.userInformationProvider = userInformationProvider;
        this.facebookService = facebookService;
        this.rememberMeHandler = rememberMeHandler;
        this.gameTypeRepository = gameTypeRepository;
        this.externalWebLoginService = externalWebLoginService;
        this.fbAppRequestService = fbAppRequestService;
        this.gameConfigurationRepository = gameConfigurationRepository;
    }

    @RequestMapping("/not-allowed/{gameType}")
    public String notAllowed(@PathVariable("gameType") final String requestedGameType,
                             final ModelMap model) {
        model.put("gameType", gameTypeFor(requestedGameType));
        return "do-not-allow";
    }

    @RequestMapping(value = "/facebookOAuthLogin/{gameType}",
            method = RequestMethod.GET,
            params = {"code"})
    public ModelAndView connectFromFacebook(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            @PathVariable("gameType") final String gameType,
                                            @RequestParam(value = "code", required = true) final String code,
                                            @RequestParam(value = "ref", required = false) final String ref) {
        final FacebookAppConfiguration facebookAppConfiguration =
                facebookConfiguration.getAppConfigFor(gameType, ApplicationType.CONNECT, LOOSE);
        final String requestURL = buildRedirectionUri(request, ref);

        try {
            cookieHelper.setLastGameType(response, gameType);

            final String accessToken = facebookService.getAccessTokenForGivenCode(code,
                    facebookAppConfiguration.getApplicationId(),
                    facebookAppConfiguration.getSecretKey(),
                    requestURL);

            if (accessToken == null) {
                throw new Exception(String.format("Null facebook access token returned for code '%s'", code));
            }

            return registerSession(request, response, gameType, null, accessToken, CONNECT, WEB);

        } catch (FacebookOAuthException e) {
            LOG.info("OAuth exception caught, attempting to reauthorise: {}; {}", e.getFacebookType(), e.getFacebookMessage());
            return new ModelAndView(new ExternalRedirectView(reauthenticationUrlFor(requestURL, facebookAppConfiguration)));

        } catch (final Exception ex) {
            LOG.error("Facebook Connect authentication failed", ex);
            sendInternalServerError(response);
            return null;
        }
    }

    private String buildRedirectionUri(final HttpServletRequest request, final String ref) {
        final StringBuffer requestURL = request.getRequestURL();
        if (StringUtils.isNotEmpty(ref)) {
            requestURL.append("?ref=").append(ref);
        }
        return requestURL.toString();
    }

    private String reauthenticationUrlFor(final String redirectUri,
                                          final FacebookAppConfiguration facebookAppConfiguration) {
        return String.format(FACEBOOK_REAUTH_URL, facebookAppConfiguration.getApplicationId(), redirectUri);
    }

    private void sendInternalServerError(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/facebookOAuthLogin/{gameType}", method =
            RequestMethod.GET,
            params = {"error_reason", "error", "error_description"})
    public ModelAndView connectFromFacebookError(final HttpServletResponse response,
                                                 @PathVariable("gameType") final String gameType,
                                                 @RequestParam("error_reason") final String errorReason,
                                                 @RequestParam("error") final String error,
                                                 @RequestParam("error_description") final String errorDescription) {
        try {
            LOG.warn("Facebook authentication error returned for gameTpe '{}': {}; reason={}; desc={}",
                    gameType,
                    error,
                    errorReason,
                    errorDescription);

            final RedirectView redirectView;
            if ("user_denied".equals(errorReason)) {
                redirectView = new RedirectView("/not-allowed/" + gameType);
            } else {
                redirectView = new RedirectView("/" + gameType);
            }
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);

        } catch (final Exception ex) {
            LOG.error("Facebook Connect authentication failed", ex);
            sendInternalServerError(response);
            return null;
        }
    }

    @RequestMapping(value = "/facebookOAuthLogin/{gameType}", method =
            RequestMethod.GET,
            params = {"error_code", "error_message"})
    public ModelAndView connectFromFacebookWithCodedError(final HttpServletResponse response,
                                                          @PathVariable("gameType") final String gameType,
                                                          @RequestParam("error_code") final String errorCode,
                                                          @RequestParam("error_message") final String errorMessage) {
        try {
            LOG.warn("Facebook authentication error returned for gameType '{}': error {}; message={}",
                    gameType, errorCode, errorMessage);

            final RedirectView redirectView = new RedirectView("/" + gameType);
            redirectView.setExposeModelAttributes(false);
            return new ModelAndView(redirectView);

        } catch (final Exception ex) {
            LOG.error("Facebook Connect authentication failed", ex);
            sendInternalServerError(response);
            return null;
        }
    }

    @RequestMapping(value = {"/public/connectLogin", "/connectLogin"}, method = RequestMethod.POST)
    public ModelAndView connectLogin(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     @RequestParam(value = "access_token", required = false) final String accessToken) {
        try {
            notNull(accessToken, "accessToken may not be null");

            final String gameType = gameTypeResolver.resolveGameType(request, response);

            LOG.debug("Entering Facebook Connect with gameType = {}; accessToken = {}", gameType, accessToken);

            return registerSession(request, response, gameType, null, accessToken, CONNECT, WEB);

        } catch (final Exception ex) {
            LOG.error("Facebook Connect authentication failed", ex);
            sendInternalServerError(response);
            return null;
        }
    }

    @RequestMapping({"/public/facebookLogin/**", "/facebookLogin/**"})
    public ModelAndView canvasLogin(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = "tableId", required = false) final String tableId,
            @RequestParam(value = "signed_request", required = false) final String signedRequest,
            @RequestParam(value = "code", required = false) final String code,
            @RequestParam(value = "error", required = false) final String error)
            throws IOException, FacebookException {
        try {
            LOG.debug("Entering Facebook OAuth 2.0 with tableId={}, code={}, signed_request={}",
                    tableId, code, signedRequest);

            return performCanvasLogin(request, response, tableId, signedRequest, error);

        } catch (final Exception ex) {
            LOG.error("Facebook authentication failed", ex);
            sendInternalServerError(response);
            return null;
        }
    }

    private ModelAndView performCanvasLogin(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            final String tableId,
                                            final String signedRequest,
                                            final String error) throws IOException {
        String gameType = gameTypeFromRequestURI(request);
        if (gameType == null) {
            gameType = gameTypeResolver.resolveGameType(request, response);
        }

        cookieHelper.setReferralTableId(response, tableId);

        if (isNotBlank(error)) {
            LOG.warn("Facebook authentication error returned: {}; reason={}; desc={}", error,
                    request.getParameter("error_reason"), request.getParameter("error_description"));

            return new ModelAndView("facebookAuthFailure");

        } else if (isNotBlank(signedRequest)) {
            final FacebookAppConfiguration config = facebookConfiguration.getAppConfigFor(
                    gameType, ApplicationType.CANVAS, LOOSE);
            final SignedRequest decodedSignedRequest = new SignedRequest(signedRequest, config.getSecretKey());
            if (decodedSignedRequest.getOAuthToken() != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authenticated via signed request; token = {}", decodedSignedRequest.getOAuthToken());
                }

                Platform platform = Platform.FACEBOOK_CANVAS;
                if (config.isRedirecting()) {
                    platform = WEB;
                }
                return registerSession(request, response, gameType, tableId,
                        decodedSignedRequest.getOAuthToken(), canvasSourceFor(request), platform);
            }
        }

        return authenticateOnCanvas(request, gameType, tableId);
    }

    private Source canvasSourceFor(final HttpServletRequest request) {
        if (request.isSecure()) {
            return SECURE_CANVAS;
        }
        return INSECURE_CANVAS;
    }

    private String gameTypeFromRequestURI(final HttpServletRequest request) {
        String gameType = null;
        if (request.getRequestURI().startsWith(CANVAS_URL)) {
            gameType = request.getRequestURI()
                    .substring(CANVAS_URL.length(), request.getRequestURI().length())
                    .replaceAll("/", "");
        }
        return gameType;
    }

    private ModelAndView authenticateOnCanvas(final HttpServletRequest request,
                                              final String gameType,
                                              final String tableId) throws IOException {
        final FacebookAppConfiguration config = facebookConfiguration.getAppConfigFor(
                gameType, ApplicationType.CANVAS, LOOSE);

        final StringBuilder sb = new StringBuilder(REDIRECTION_URL);
        sb.append("?");
        if (tableId != null) {
            sb.append("tableId=");
            sb.append(tableId);
            sb.append("&");
        }
        sb.append("request_ids=").append(request.getParameter("request_ids"));

        final String redirectUrl = encode(String.format(sb.toString(), protocolOf(request), config.getAppName()),
                "UTF-8");

        final String authUrl = String.format(AUTH_URL,
                config.getApplicationId(),
                redirectUrl,
                permissionsFor(config));

        LOG.debug("User not authenticated. Redirecting to {}", authUrl);

        return new ModelAndView("facebookLoginRedirect", "faceBookLoginUrl", authUrl);
    }

    private String permissionsFor(final FacebookAppConfiguration config) {
        if (config.isCanvasActionsAllowed()) {
            return facebookPermissions;
        }
        return "";
    }

    private String protocolOf(final HttpServletRequest request) {
        final String protocol;
        if (request.isSecure()) {
            protocol = "https";
        } else {
            protocol = "http";
        }
        return protocol;
    }

    private ModelAndView registerSession(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final String gameType,
                                         final String tableId,
                                         final String accessToken,
                                         final Source source,
                                         final Platform platform)
            throws IOException, FacebookException {

        LOG.debug("Facebook Login: RequestGameType = {}, Params = {}", gameType, request.getQueryString());

        final FacebookAppConfiguration facebookAppConfiguration =
                facebookConfiguration.getAppConfigFor(gameType, applicationTypeFor(source), LOOSE);
        LOG.debug("Lobby session not found. Creating new one.");

        cookieHelper.setOriginalGameType(response, facebookAppConfiguration.getGameType());

        final String requestIdsString = request.getParameter("request_ids");

        fbAppRequestService.processAppRequests(requestIdsString, accessToken);

        final String requestIdsDecoded;
        if (requestIdsString == null) {
            requestIdsDecoded = null;
        } else {
            requestIdsDecoded = URLDecoder.decode(requestIdsString, "UTF-8");
        }

        final PlayerInformationHolder provider = userInformationProvider.getUserInformationHolder(
                accessToken, requestIdsDecoded, request.getRemoteAddr(), facebookAppConfiguration.isCanvasActionsAllowed());

        final LoginResponse loginResponse = externalWebLoginService.login(request,
                response,
                Partner.YAZINO,
                facebookAppConfiguration.getGameType(),
                provider,
                true,
                platform, new HashMap<String, Object>());

        if (LoginResult.FAILURE == loginResponse.getResult()) {
            sendInternalServerError(response);
            return null;
        }

        if (LoginResult.BLOCKED == loginResponse.getResult()) {
            LOG.debug("Blocked {} user[externalId={}]", provider.getPlayerProfile().getProviderName(),
                    provider.getPlayerProfile().getExternalId());
            final Map<String, String> model = new HashMap<String, String>();
            model.put("assetUrl", siteConfiguration.getAssetUrl());
            return new ModelAndView("blocked", model);
        }

        if (platform != Platform.FACEBOOK_CANVAS && provider.getPlayerProfile() != null) {
            rememberMeHandler.storeRememberMeCookie(
                    Partner.YAZINO,
                    platform, provider.getPlayerProfile().getPlayerId(),
                    provider.getPlayerProfile().getExternalId(),
                    request,
                    response
            );
        }

        request.getSession().setAttribute("facebookAccessToken." + gameType,accessToken);
        final String sourceId = request.getParameter("sourceId");
        final String redirectUrl = redirectionUrlFor(
                firstNotNullOf(gameType, facebookAppConfiguration.getGameType()),
                tableId,
                cookieHelper.getRedirectTo(request),
                sourceId,
                platform);
        cookieHelper.setRedirectTo(response, null);

        LOG.debug("sourceId: {}; redirectUrl: {}", sourceId, redirectUrl);

        if ((source == SECURE_CANVAS || source == INSECURE_CANVAS) && platform == WEB) {
            return splashPageFor(facebookAppConfiguration);
        } else if (source == INSECURE_CANVAS && platform == Platform.FACEBOOK_CANVAS) {
            return redirectToSecureFacebookCanvasFor(facebookAppConfiguration);
        }

        return new ModelAndView(new RedirectView(redirectUrl, false, true, false));
    }

    private String firstNotNullOf(final String... items) {
        for (String item : items) {
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    private FacebookConfiguration.ApplicationType applicationTypeFor(final Source source) {
        switch (source) {
            case SECURE_CANVAS:
            case INSECURE_CANVAS:
                return FacebookConfiguration.ApplicationType.CANVAS;
            case CONNECT:
                return FacebookConfiguration.ApplicationType.CONNECT;
            default:
                throw new IllegalArgumentException("Unexpected source: " + source);
        }
    }

    private ModelAndView splashPageFor(final FacebookAppConfiguration facebookAppConfiguration)
            throws IOException {
        return new ModelAndView("fbredirect/fanPage")
                .addObject("gameType", facebookAppConfiguration.getGameType())
                .addObject("targetUrl", facebookAppConfiguration.getRedirectUrl());
    }

    private ModelAndView redirectToSecureFacebookCanvasFor(final FacebookAppConfiguration facebookAppConfiguration) {
        return new ModelAndView("fbredirect/secureCanvasRedirect")
                .addObject("appName", facebookAppConfiguration.getAppName());
    }

    private String redirectionUrlFor(final String gameType,
                                     final String tableId,
                                     final String redirectTo,
                                     final String sourceId,
                                     final Platform platform) {
        String redirectUrl;
        if (isNotBlank(tableId)) {
            redirectUrl = "/table/" + tableId;
        } else if (isNotBlank(redirectTo)) {
            redirectUrl = redirectTo;
        } else if (platform == FACEBOOK_CANVAS && !gameUsesFlashLobby(gameType)) {
            redirectUrl = String.format(GAME_TABLE_DESTINATION, siteConfiguration.getHostUrl(), gameType);
        } else {
            redirectUrl = String.format(GAME_LOBBY_DESTINATION, siteConfiguration.getHostUrl(),
                    gameTypeMapper.getViewName(gameType));
        }

        if (sourceId != null) {
            redirectUrl = addRequestParameter(redirectUrl, "sourceId", sourceId);
        }
        return redirectUrl;
    }

    private boolean gameUsesFlashLobby(final String gameType) {
        GameConfiguration gameConfiguration = gameConfigurationRepository.find(gameType);
        return gameConfiguration != null
                && gameConfiguration.getProperty("usesFlashLobby") != null
                && "true".equals(gameConfiguration.getProperty("usesFlashLobby").trim());
    }

    private String gameTypeFor(final String pseudonym) {
        if (pseudonym != null) {
            for (GameTypeInformation gameTypeInformation : gameTypeRepository.getGameTypes().values()) {
                LOG.info("Available pseudonyms are: {}", gameTypeInformation.getGameType().getPseudonyms());
                if (gameTypeInformation.getGameType().getPseudonyms().contains(pseudonym)) {
                    return gameTypeInformation.getId();
                }
            }
        }
        return siteConfiguration.getDefaultGameType();
    }

    private String addRequestParameter(final String url, final String name, final String value) {
        String format;
        if (url.indexOf('?') == -1) {
            format = url + "?%s=%s";
        } else {
            format = url + "&%s=%s";
        }
        return String.format(format, name, value);
    }

}
