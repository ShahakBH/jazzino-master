package com.yazino.web.controller;

import com.yazino.bi.opengraph.OpenGraphCredentialsMessage;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileAuthenticationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.domain.LoginResponse;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.form.LoginForm;
import com.yazino.web.security.LogoutHelper;
import com.yazino.web.service.ExternalWebLoginService;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.RememberMeHandler;
import com.yazino.web.session.*;
import com.yazino.web.util.ClientContextConverter;
import com.yazino.web.util.MobileRequestGameGuesser;
import com.yazino.web.util.RequestParameterUtils;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.yazino.platform.Partner.TANGO;
import static com.yazino.platform.Partner.YAZINO;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@Controller
public class MobileLoginController {
    public static final Logger LOG = LoggerFactory.getLogger(MobileLoginController.class);

    private static final String PROPERTY_PARTNER_ID = "strata.lobby.partnerid";
    private static final String DEFAULT_PARTNER_ID = YAZINO.name();
    private static final Partner TANGO_PARTNER_ID = TANGO;

    private final LobbySessionCache lobbySessionCache;
    private final LobbySessionFactory lobbySessionFactory;
    private final FacebookConfiguration facebookConfiguration;
    private final FacebookUserInformationProvider userInformationProvider;
    private final ExternalWebLoginService externalWebLoginService;
    private final AuthenticationService authenticationService;
    private final QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService;
    private final RememberMeHandler rememberMeHandler;
    private final LogoutHelper logoutHelper;
    private final GameAvailabilityService gameAvailabilityService;
    private final MobileRequestGameGuesser requestGameGuesser;
    private final WebApiResponses responseWriter;
    private final YazinoConfiguration yazinoConfiguration;
    private final TangoPlayerInformationProvider tangoPlayerInformationProvider;

    @Autowired(required = true)
    public MobileLoginController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("lobbySessionFactory") final LobbySessionFactory lobbySessionFactory,
            @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration,
            @Qualifier("facebookUserInformationProvider") final FacebookUserInformationProvider userInformationProvider,
            final ExternalWebLoginService externalWebLoginService,
            final AuthenticationService authenticationService,
            @Qualifier("openGraphCredentialsService") final QueuePublishingService<OpenGraphCredentialsMessage> openGraphCredentialsService,
            final RememberMeHandler rememberMeHandler,
            LogoutHelper logoutHelper,
            final MobileRequestGameGuesser requestGameGuesser,
            final GameAvailabilityService gameAvailabilityService,
            final WebApiResponses responseWriter,
            final YazinoConfiguration yazinoConfiguration,
            final TangoPlayerInformationProvider tangoPlayerInformationProvider
    ) {
        this.lobbySessionCache = lobbySessionCache;
        this.lobbySessionFactory = lobbySessionFactory;
        this.facebookConfiguration = facebookConfiguration;
        this.userInformationProvider = userInformationProvider;
        this.externalWebLoginService = externalWebLoginService;
        this.authenticationService = authenticationService;
        this.openGraphCredentialsService = openGraphCredentialsService;
        this.rememberMeHandler = rememberMeHandler;
        this.logoutHelper = logoutHelper;
        this.requestGameGuesser = requestGameGuesser;
        this.gameAvailabilityService = gameAvailabilityService;
        this.responseWriter = responseWriter;
        this.yazinoConfiguration = yazinoConfiguration;
        this.tangoPlayerInformationProvider = tangoPlayerInformationProvider;
    }

    @Deprecated
    @RequestMapping(value = "/publicCommand/mobile/YAZINO", method = RequestMethod.POST)
    public void mobileYazino(final HttpServletRequest request,
                             final HttpServletResponse response,
                             @ModelAttribute("loginForm") final LoginForm form,
                             @RequestParam(value = "mobileType", defaultValue = "IOS") final String mobileType,
                             @RequestParam(value = "useSessionCookie", defaultValue = "true") final Boolean useSessionCookie,
                             @RequestParam(value = "partnerId", required = false) final String partnerId) throws IOException {

        final Platform platform = convertMobileType(mobileType);
        final String gameType = requestGameGuesser.guessGame(request, platform);

        addRequestURLOverrideParameter(request, "YAZINO", platform, gameType);
        doYazinoLogin(form,
                request,
                response,
                platform,
                gameType,
                useSessionCookie,
                ClientContextConverter.toMap(""),
                validatePartnerId(partnerId));
    }

    private Platform convertMobileType(String mobileType) {
        /*
        ANDROID registrations always specified the mobileType as 'ANDROID'. 'old' IOS mobile registrations never
        specified the mobileType request parameter, so if missing the platform is IOS
        */
        Platform platform;
        try {
            platform = Platform.valueOf(mobileType);
        } catch (IllegalArgumentException e) {
            platform = Platform.IOS;
        }
        return platform;
    }

    @RequestMapping(value = "/public/login/{platform}/{game}/YAZINO", method = RequestMethod.POST)
    public void loginWithYazino(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @ModelAttribute("loginForm") final LoginForm form,
            @PathVariable("platform") final String platformInput,
            @PathVariable("game") final String gameType,
            @RequestParam(value = "useSessionCookie", defaultValue = "true") final Boolean useSessionCookie,
            @RequestParam(value = "clientContext", required = false) final String clientContext,
            @RequestParam(value = "partnerId", required = false) final String partnerId)//TODO is this needed? prob not.
            throws IOException {

        Map<String, Object> clientContextMap = ClientContextConverter.toMap(clientContext);

        final Platform platform = Platform.valueOf(platformInput);
        final Partner partner = validatePartnerId(partnerId);

        doYazinoLogin(form, request, response, platform, gameType, useSessionCookie, clientContextMap, partner);
    }


    private Partner validatePartnerId(final String partnerId) {

        try {
            return Partner.parse(partnerId);//checks for valid partner
        } catch (Exception e) {
            final String defaultPartnerId = yazinoConfiguration.getString(PROPERTY_PARTNER_ID, DEFAULT_PARTNER_ID);
            LOG.error("Invalid partner: {}, defaulting to ", partnerId, defaultPartnerId);
            return Partner.parse(defaultPartnerId);
        }
    }

    @RequestMapping(value = "/public/logout/{platform}/{game}", method = RequestMethod.GET)
    public void logout(final HttpSession session,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws IOException {
        logoutHelper.logout(session, request, response);
        responseWriter.writeOk(response, Collections.emptyMap());
    }

    private void doYazinoLogin(final LoginForm form,
                               final HttpServletRequest request,
                               final HttpServletResponse response,
                               final Platform platform,
                               final String gameType,
                               final Boolean useSessionCookie,
                               final Map<String, Object> clientContext,
                               final Partner partnerId) throws IOException {

        LOG.debug("YAZINO Login request: url {}, form {}, platform {}, game {}, useSessionCookie {}",
                request.getRequestURL(), form, platform.name(), gameType, useSessionCookie);

        LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        final LoginInfo info = new LoginInfo();
        info.setSuccess(true);
        if (lobbySession == null) {
            final PlayerProfileAuthenticationResponse authenticationResponse
                    = authenticationService.authenticateYazinoUser(form.getEmail(), form.getPassword());
            final BigDecimal playerId = authenticationResponse.getPlayerId();
            info.setSuccess(authenticationResponse.isSuccessful());


            if (authenticationResponse.isSuccessful()) {
                lobbySession = lobbySessionFactory.registerAuthenticatedSession(request, response,
                        partnerId, playerId, LoginResult.EXISTING_USER, useSessionCookie, platform, clientContext, gameType);
            } else {
                info.setError("Your username and/or password were incorrect.");
            }
        }
        if (lobbySession != null) {
            LOG.debug("Yazino login successful (has session) for playerId {}", lobbySession.getPlayerId().toPlainString());
            info.setName(lobbySession.getPlayerName());
            info.setPlayerId(lobbySession.getPlayerId());
            info.setSession(new LobbySessionReference(lobbySession).encode());
            info.setTags(lobbySession.getTags());
            addAvailabilityForGameType(gameType, info);
        }
        storeRememberMeInformation(request, response, platform, partnerId);
        responseWriter.writeOk(response, info);
    }

    private void addAvailabilityForGameType(String gameType, LoginInfo info) {
        GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(gameType);
        info.setAvailability(availability.getAvailability());
        if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
            info.setMaintenanceStartsAtMillis(availability.getMaintenanceStartsAtMillis());
        }
    }


    @RequestMapping(value = "/public/login/{platform}/{game}/TANGO", method = RequestMethod.POST)
    public void loginWithTango(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @PathVariable("platform") final String platformInput,
            @PathVariable("game") final String gameType,
            @RequestParam(value = "useSessionCookie", defaultValue = "true") final Boolean useSessionCookie,
            @RequestParam(value = "clientContext", required = false) final String clientContext,
            @RequestParam(value = "encryptedData", required = false) final String encryptedData
    ) throws IOException {
        checkNotNull(encryptedData, "encryptedData");
        checkNotNull(clientContext, "clientContext");
        //accessToken isn't really used due to comms with tango being... difficult.

        LOG.debug("YAZINO Login request: url {}, platform {}, game {}, accessToken {}, useSessionCookie {},encryptedData {}",
                request.getRequestURL(), platformInput, gameType, useSessionCookie, encryptedData);

        try {
            PlayerInformationHolder holder =
                    tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(encryptedData);
            final Platform platform = Platform.valueOf(platformInput);
            Map<String, Object> clientContextMap = ClientContextConverter.toMap(clientContext);

            doTangoLogin(request, response, platform, gameType, useSessionCookie, clientContextMap, TANGO_PARTNER_ID, holder);

        } catch (GeneralSecurityException e) {
            LOG.warn("error decrypting the tango player information:{}", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "could not decrypt");
        } catch (Exception e) {
            LOG.warn("error accessing data from loginJson object");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid json:" + e.getMessage());
        }

    }


    private void doTangoLogin(final HttpServletRequest request,
                              final HttpServletResponse response,
                              final Platform platform,
                              final String gameType,
                              final Boolean useSessionCookie,
                              final Map<String, Object> clientContextMap,
                              final Partner partnerId,
                              PlayerInformationHolder holder) throws IOException {

        final LoginResponse loginResult = externalWebLoginService.login(request,
                response,
                partnerId,
                gameType,
                holder,
                useSessionCookie,
                platform,
                clientContextMap);

        LoginInfo info = createLoginInfo(gameType, "", loginResult, holder.getPlayerProfile());
        storeRememberMeInformation(request, response, platform, partnerId);
        responseWriter.writeOk(response, info);
    }


    @Deprecated
    @RequestMapping("/publicCommand/mobile/FACEBOOK")
    public void mobileFacebook(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = "gameType", required = true) final String gameType,
            @RequestParam(value = "accessToken", required = false) final String accessToken,
            @RequestParam(value = "mobileType", defaultValue = "IOS") final String mobileType,
            @RequestParam(value = "useSessionCookie", defaultValue = "true") final Boolean useSessionCookie)
            throws IOException {

        final Platform platform = convertMobileType(mobileType);
        addRequestURLOverrideParameter(request, "FACEBOOK", platform, gameType);
        Map<String, Object> clientContextMap = ClientContextConverter.toMap("");

        doFacebookLogin(request, response, platform, gameType, accessToken, useSessionCookie, clientContextMap);
    }

    @RequestMapping(value = "/public/login/{platform}/{game}/FACEBOOK", method = RequestMethod.POST)
    public void loginWithFacebook(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @PathVariable("platform") final String platformInput,
            @PathVariable("game") final String gameType,
            @RequestParam(value = "accessToken", required = false) final String accessToken,
            @RequestParam(value = "useSessionCookie", defaultValue = "true") final Boolean useSessionCookie,
            @RequestParam(value = "clientContext", required = false) final String clientContext) throws IOException {

        final Platform platform = Platform.valueOf(platformInput);
        Map<String, Object> clientContextMap = ClientContextConverter.toMap(clientContext);

        doFacebookLogin(request, response, platform, gameType, accessToken, useSessionCookie, clientContextMap);
    }

    private void doFacebookLogin(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 final Platform platform,
                                 final String gameType,
                                 final String accessToken,
                                 final Boolean useSessionCookie,
                                 final Map<String, Object> clientContextMap) throws IOException {

        String url = request.getRequestURI();
        String platformName = platform.name();
        String agent = request.getHeader("User-Agent");

        LOG.debug("FACEBOOK Login request: url {}, token {}, platform {}, game {}, useSessionCookie {}",
                url, accessToken, platformName, gameType, useSessionCookie);

        if (!RequestParameterUtils.hasParameter("accessToken", accessToken, request, response)) {
            LOG.warn("Missing accessToken. URL [{}], game [{}], platform [{}], agent [{}]",
                    url, gameType, platformName, agent);
            return;
        }

        final FacebookAppConfiguration config = facebookConfiguration.getAppConfigFor(gameType, CANVAS, LOOSE);
        final PlayerInformationHolder provider = userInformationProvider.getUserInformationHolder(
                accessToken, null, request.getRemoteAddr(), config.isCanvasActionsAllowed());

        final LoginResponse result = externalWebLoginService.login(request,
                response,
                Partner.parse(yazinoConfiguration.getString(PROPERTY_PARTNER_ID, DEFAULT_PARTNER_ID)),
                config.getGameType(),
                provider,
                useSessionCookie,
                platform,
                clientContextMap);

        LoginInfo info = createLoginInfo(gameType, accessToken, result, provider.getPlayerProfile());
        storeRememberMeInformation(request, response, platform, Partner.YAZINO);
        responseWriter.writeOk(response, info);
    }

    private LoginInfo createLoginInfo(String gameType, String accessToken, LoginResponse result, PlayerProfile playerProfile) {
        LoginInfo info = null;
        switch (result.getResult()) {
            case BLOCKED:
                info = createLoginInfoForBlockedPlayer(playerProfile);
                break;
            case FAILURE:
                info = createLoginInfoForFailedLogin(accessToken, playerProfile);
                break;
            case NEW_USER:
            case EXISTING_USER:
                info = createLoginInfoForSuccessfulLogin(gameType, accessToken, result, playerProfile);
                break;
            default:
                throw new IllegalStateException("Unknown result received from login service: " + result);
        }
        return info;
    }

    private LoginInfo createLoginInfoForSuccessfulLogin(String gameType,
                                                        String accessToken,
                                                        LoginResponse result,
                                                        PlayerProfile playerProfile) {
        final LoginInfo info = new LoginInfo();
        info.setNewPlayer(LoginResult.NEW_USER == result.getResult());
        info.setSuccess(true);
        info.setName(playerProfile.getDisplayName());
        final LobbySession lobbySession = result.getSession().get();
        final BigDecimal playerId = lobbySession.getPlayerId();
        info.setPlayerId(playerId);
        info.setSession(new LobbySessionReference(lobbySession).encode());
        addAvailabilityForGameType(gameType, info);
        if (TANGO_PARTNER_ID.equals(playerProfile.getPartnerId())) {
            //currently, do nothing
        } else {
            sendCredentialsToOpenGraph(gameType, accessToken, playerId);
        }
        info.setTags(lobbySession.getTags());
        return info;
    }

    private LoginInfo createLoginInfoForFailedLogin(String accessToken, PlayerProfile playerProfile) {
        final LoginInfo info = new LoginInfo();
        info.setSuccess(false);
        info.setError("Failed");
        LOG.warn("Failed to register mobile  session for access token [{}], playerId [{}]",
                accessToken, nullSafeGetPlayerId(playerProfile));
        return info;
    }

    private LoginInfo createLoginInfoForBlockedPlayer(PlayerProfile playerProfile) {
        final LoginInfo info = new LoginInfo();
        info.setSuccess(false);
        info.setError("Blocked");
        LOG.warn("Blocked {} user[externalId={}, playerId={}]",
                playerProfile.getProviderName(),
                playerProfile.getExternalId(),
                playerProfile.getPlayerId());
        return info;
    }

    // ... because checkstyle disallows playerProfile == null ? null : playerProfile.getPlayerId()...
    private BigDecimal nullSafeGetPlayerId(final PlayerProfile playerProfile) {
        if (playerProfile == null) {
            return null;
        } else {
            return playerProfile.getPlayerId();
        }
    }

    /*
     * This is done because backoffice uses the request url to determine things like game type and platform.
     * Old style clients hit the url publicCommand/mobile/FACEBOOK which doesn't contain enough information.
     */
    private void addRequestURLOverrideParameter(final HttpServletRequest request,
                                                final String provider,
                                                final Platform platform,
                                                final String gameType) {
        final String platformValue = platform.name();
        final String url = request.getRequestURL().toString();

        final String toBeReplaced = String.format("/publicCommand/mobile/%s", provider);
        final String replacement = String.format("/public/login/%s/%s/%s", platform.name(), gameType, provider);
        final String modifiedPath = String.format(replacement, platformValue, gameType);
        final String requestURL = url.replace(toBeReplaced, modifiedPath);
        request.setAttribute(PlatformReportingHelper.REQUEST_URL, requestURL);
    }

    private void sendCredentialsToOpenGraph(final String gameType,
                                            final String accessToken,
                                            final BigDecimal playerId) {
        try {
            openGraphCredentialsService.send(new OpenGraphCredentialsMessage(
                    playerId.toBigInteger(), gameType, accessToken));
        } catch (Exception e) {
            LOG.warn("Unable to send credentials to OpenGraph service.", e);
        }
    }

    private void storeRememberMeInformation(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Platform platform,
                                            Partner partnerId) {
        LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            LOG.debug("Player not authenticated. Not storing 'remember me' information.");
            return;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Storing 'remember me' info for lobbySession " + lobbySession);
        }
        rememberMeHandler.storeRememberMeCookie(partnerId,
                platform,
                lobbySession.getPlayerId(),
                lobbySession.getPlayerName(),
                request,
                response);
    }

    /**
     * @noinspection UnusedDeclaration
     */
    public static class LoginInfo {
        private boolean success;
        private String name;
        private BigDecimal playerId;
        private String error;
        private String session;
        private Boolean newPlayer;
        private GameAvailabilityService.Availability availability;
        private Long maintenanceStartsAtMillis;
        private Set<String> tags = new HashSet<>();

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public BigDecimal getPlayerId() {
            return playerId;
        }

        public void setPlayerId(final BigDecimal playerId) {
            this.playerId = playerId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(final boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(final String error) {
            this.error = error;
        }

        public String getSession() {
            return session;
        }

        public void setSession(final String session) {
            this.session = session;
        }

        public Boolean isNewPlayer() {
            return newPlayer;
        }

        public void setNewPlayer(Boolean newPlayer) {
            this.newPlayer = newPlayer;
        }

        public GameAvailabilityService.Availability getAvailability() {
            return availability;
        }

        public void setAvailability(GameAvailabilityService.Availability availability) {
            this.availability = availability;
        }

        public void setMaintenanceStartsAtMillis(Long maintenanceStartsAtMillis) {
            this.maintenanceStartsAtMillis = maintenanceStartsAtMillis;
        }

        public Long getMaintenanceStartsAtMillis() {
            return maintenanceStartsAtMillis;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            LoginInfo rhs = (LoginInfo) obj;
            return new EqualsBuilder()
                    .append(this.success, rhs.success)
                    .append(this.name, rhs.name)
                    .append(this.playerId, rhs.playerId)
                    .append(this.error, rhs.error)
                    .append(this.session, rhs.session)
                    .append(this.newPlayer, rhs.newPlayer)
                    .append(this.availability, rhs.availability)
                    .append(this.maintenanceStartsAtMillis, rhs.maintenanceStartsAtMillis)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(success)
                    .append(name)
                    .append(playerId)
                    .append(error)
                    .append(session)
                    .append(newPlayer)
                    .append(availability)
                    .append(maintenanceStartsAtMillis)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("success", success)
                    .append("name", name)
                    .append("playerId", playerId)
                    .append("error", error)
                    .append("session", session)
                    .append("newPlayer", newPlayer)
                    .append("availability", availability)
                    .append("maintenanceStartsAtMillis", maintenanceStartsAtMillis)
                    .toString();
        }

        public void setTags(Set<String> tags) {
            this.tags = tags;
        }

        public Set<String> getTags() {
            return tags;
        }
    }

    private void checkNotNull(String displayName, final String parameterName) {
        // not using checkNotNull as this doesn't use IllegalArgumentException
        checkArgument(displayName != null, "parameter '" + parameterName + "' is missing");
    }
}
