package com.yazino.web.controller.guestplay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileRegistrationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.controller.MobileLoginController;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionReference;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Partner.parse;
import static com.yazino.web.controller.guestplay.BusinessError.toBusinessErrors;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class GuestRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(GuestRegistrationController.class);

    private static final String PROPERTY_PARTNER_ID = "strata.lobby.partnerid";
    private static final String PROPERTY_CONTENT_URL = "senet.web.content";
    private static final String PROPERTY_DEFAULT_AVATAR = "senet.web.defaultAvatarPath";
    private static final String DEFAULT_PARTNER_ID = "YAZINO";
    private static final String DEFAULT_AVATAR_STRING = "default";

    private final ReferrerSessionCache referrerSessionCache;
    private final AuthenticationService authenticationService;
    private final GameAvailabilityService gameAvailabilityService;
    private final WebApiResponses responseWriter;
    private final YazinoWebLoginService yazinoWebLoginService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public GuestRegistrationController(final YazinoWebLoginService yazinoWebLoginService,
                                       final ReferrerSessionCache referrerSessionCache,
                                       final AuthenticationService authenticationService,
                                       final GameAvailabilityService gameAvailabilityService,
                                       final WebApiResponses responseWriter,
                                       final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoWebLoginService, "yazinoWebLoginService may not be null");
        notNull(referrerSessionCache, "referrerSessionCache may not be null");
        notNull(authenticationService, "authenticationService may not be null");
        notNull(gameAvailabilityService, "gameAvailabilityService may not be null");
        notNull(responseWriter, "responseWriter may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoWebLoginService = yazinoWebLoginService;
        this.referrerSessionCache = referrerSessionCache;
        this.authenticationService = authenticationService;
        this.gameAvailabilityService = gameAvailabilityService;
        this.responseWriter = responseWriter;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Deprecated
    @RequestMapping(value = "/api/1.0/registration/guest/create", method = RequestMethod.POST)
    @AllowPublicAccess
    public void registerGuest(final HttpServletRequest request,
                              final HttpServletResponse response,
                              @RequestParam(value = "displayName", required = false) String displayName,
                              @RequestParam(value = "emailAddress", required = false) String emailAddress,
                              @RequestParam(value = "password", required = false) String password,
                              @RequestParam(value = "platform", required = false) String platform,
                              @RequestParam(value = "gameType", required = false) String gameType,
                              @RequestParam(value = "avatarUrl", required = false) String avatarUrl

    ) throws IOException {

        LOG.info("register guest (displayName={}, emailAddress={}, password={}, platform={}, gameType={}, avatarUrl={})",
                displayName, emailAddress, password, platform, gameType, avatarUrl);

        guestRegistration(request, response, displayName, emailAddress, password, platform, gameType, avatarUrl, YAZINO);
    }

    @RequestMapping(value = "/api/1.1/registration/guest/create", method = RequestMethod.POST)
    @AllowPublicAccess
    public void registerGuest(final HttpServletRequest request,
                              final HttpServletResponse response,
                              @RequestParam(value = "displayName", required = false) String displayName,
                              @RequestParam(value = "emailAddress", required = false) String emailAddress,
                              @RequestParam(value = "password", required = false) String password,
                              @RequestParam(value = "platform", required = false) String platform,
                              @RequestParam(value = "gameType", required = false) String gameType,
                              @RequestParam(value = "avatarUrl", required = false) String avatarUrl,
                              @RequestParam(value = "partner", required = false) String partnerId
    ) throws IOException {

        LOG.info("register guest (displayName={}, emailAddress={}, password={}, platform={}, gameType={}, avatarUrl={}, partner={})",
                displayName, emailAddress, password, platform, gameType, avatarUrl, partnerId);
        try {
            Partner partner = parse(partnerId);

            guestRegistration(request, response, displayName, emailAddress, password, platform, gameType, avatarUrl, partner);

        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void guestRegistration(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final String displayName,
                                   final String emailAddress,
                                   final String password,
                                   final String platform,
                                   final String gameType,
                                   final String avatarUrl,
                                   final Partner partner) throws IOException {
        try {
            checkNotNull(displayName, "displayName");
            checkNotNull(emailAddress, "emailAddress");
            checkNotNull(password, "password");
            checkNotNull(platform, "platform");
            checkNotNull(gameType, "gameType");

            Platform parsedPlatform = parsePlatform(platform);
            String referrer = referrerSessionCache.getReferrer();
            String remoteAddress = request.getRemoteAddr();

            final PlayerProfile profile = createProfile(displayName, emailAddress, partner);

            final PlayerProfileRegistrationResponse result = authenticationService.registerYazinoUser(
                    emailAddress, password, profile, remoteAddress, referrer, parsedPlatform,
                    prepareAvatarUrl(avatarUrl), gameType);
            responseWriter.write(response, HttpStatus.OK.value(),
                    processResponse(request, response, password, gameType, parsedPlatform, profile, result));

        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private String prepareAvatarUrl(String avatarUrl) {
        if (isBlank(avatarUrl) || DEFAULT_AVATAR_STRING.equalsIgnoreCase(avatarUrl)) {
            avatarUrl = defaultAvatar();
        }
        return avatarUrl;
    }

    private GuestRegistrationResponse processResponse(final HttpServletRequest request,
                                                      final HttpServletResponse response,
                                                      final String password,
                                                      final String gameType,
                                                      final Platform parsedPlatform,
                                                      final PlayerProfile profile,
                                                      final PlayerProfileRegistrationResponse result) {
        GuestRegistrationResponse guestRegistrationResponse;
        if (result.isSuccessful()) {
            final YazinoWebLoginService.NewlyRegisteredUserLoginResult loginResponse = yazinoWebLoginService.loginNewlyRegisteredUser(
                    request, response, profile.getEmailAddress(), password, parsedPlatform, profile.getPartnerId());

            final MobileLoginController.LoginInfo info = loginForFor(loginResponse.getLobbySession());
            addAvailabilityForGameType(gameType, info);
            guestRegistrationResponse = new GuestRegistrationResponse(info);

        } else {
            guestRegistrationResponse = new GuestRegistrationResponse(toBusinessErrors(result.getErrors()));
        }
        return guestRegistrationResponse;
    }

    private String defaultAvatar() {
        return yazinoConfiguration.getString(PROPERTY_CONTENT_URL) + yazinoConfiguration.getString(PROPERTY_DEFAULT_AVATAR);
    }

    private PlayerProfile createProfile(final String displayName, final String emailAddress, final Partner partner) {
        final PlayerProfile profile = new PlayerProfile();
        profile.setGuestStatus(GuestStatus.GUEST);
        profile.setDisplayName(displayName);
        profile.setEmailAddress(emailAddress);
        profile.setProviderName("YAZINO");
        profile.setPartnerId(partner);
        return profile;
    }

    private MobileLoginController.LoginInfo loginForFor(final LobbySession lobbySession) {
        final MobileLoginController.LoginInfo info = new MobileLoginController.LoginInfo();
        info.setSuccess(true);
        info.setNewPlayer(true);
        info.setName(lobbySession.getPlayerName());
        info.setPlayerId(lobbySession.getPlayerId());
        info.setSession(new LobbySessionReference(lobbySession).encode());
        return info;
    }


    // TODO remove after moving this functionality to ping
    private void addAvailabilityForGameType(String gameType, MobileLoginController.LoginInfo info) {
        GameAvailability availability = gameAvailabilityService.getAvailabilityOfGameType(gameType);
        info.setAvailability(availability.getAvailability());
        if (availability.getAvailability() == GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED) {
            info.setMaintenanceStartsAtMillis(availability.getMaintenanceStartsAtMillis());
        }
    }

    private Platform parsePlatform(String platform) {
        Platform parsedPlatform;
        try {
            parsedPlatform = Platform.valueOf(platform);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + platform + "' is not a valid platform");
        }
        return parsedPlatform;
    }

    private void checkNotNull(String displayName, final String parameterName) {
        // not using checkNotNull as this doesn't use IllegalArgumentException
        checkArgument(displayName != null, "parameter '" + parameterName + "' is missing");
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    static class GuestRegistrationResponse {
        @JsonProperty
        private boolean successful;
        @JsonProperty("sessionDetails")
        private MobileLoginController.LoginInfo loginInfo;
        @JsonProperty
        private Set<BusinessError> errors;

        private GuestRegistrationResponse() {
            /* for jackson */
        }

        public GuestRegistrationResponse(MobileLoginController.LoginInfo loginInfo) {
            Validate.notNull(loginInfo, "loginInfo");
            successful = true;
            this.loginInfo = loginInfo;
        }

        GuestRegistrationResponse(Set<BusinessError> errors) {
            Validate.notNull(errors, "errors");
            successful = false;
            this.errors = errors;
        }

        boolean isSuccessful() {
            return successful;
        }

        MobileLoginController.LoginInfo getLoginInfo() {
            return loginInfo;
        }

        Set<BusinessError> getErrors() {
            return errors;
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
            GuestRegistrationResponse rhs = (GuestRegistrationResponse) obj;
            return new EqualsBuilder()
                    .append(this.successful, rhs.successful)
                    .append(this.loginInfo, rhs.loginInfo)
                    .append(this.errors, rhs.errors)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(successful)
                    .append(loginInfo)
                    .append(errors)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("successful", successful)
                    .append("loginInfo", loginInfo)
                    .append("errors", errors)
                    .toString();
        }
    }

}
