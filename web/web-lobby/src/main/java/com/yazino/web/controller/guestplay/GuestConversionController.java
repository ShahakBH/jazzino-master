package com.yazino.web.controller.guestplay;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileServiceResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.controller.TangoPlayerInformationProvider;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.yazino.web.controller.guestplay.BusinessError.toBusinessErrors;
import static com.yazino.web.controller.guestplay.GuestConversionController.ConversionResponse.createSuccessfulConversionResponse;
import static javax.servlet.http.HttpServletResponse.*;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@Controller
public class GuestConversionController {

    private static final Logger LOG = LoggerFactory.getLogger(GuestConversionController.class);

    private final LobbySessionCache lobbySessionCache;
    private final PlayerProfileService playerProfileService;
    private final FacebookUserInformationProvider facebookUserInformationProvider;
    private final FacebookConfiguration facebookConfiguration;
    private final WebApiResponses responseWriter;
    private final TangoPlayerInformationProvider tangoPlayerInformationProvider;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public GuestConversionController(LobbySessionCache lobbySessionCache,
                                     PlayerProfileService playerProfileService,
                                     FacebookUserInformationProvider facebookUserInformationProvider,
                                     FacebookConfiguration facebookConfiguration,
                                     WebApiResponses responseWriter,
                                     final TangoPlayerInformationProvider tangoPlayerInformationProvider,
                                     final YazinoConfiguration yazinoConfiguration) {
        this.lobbySessionCache = lobbySessionCache;
        this.playerProfileService = playerProfileService;
        this.facebookUserInformationProvider = facebookUserInformationProvider;
        this.facebookConfiguration = facebookConfiguration;
        this.responseWriter = responseWriter;
        this.tangoPlayerInformationProvider = tangoPlayerInformationProvider;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @RequestMapping(value = "/api/1.0/registration/guest/convert/YAZINO", method = RequestMethod.POST)
    public void convertGuestToYazinoAccount(HttpServletRequest request,
                                            HttpServletResponse response,
                                            @RequestParam("displayName") String displayName,
                                            @RequestParam("emailAddress") String emailAddress,
                                            @RequestParam("password") String password) throws IOException {

        LOG.info("convert guest account to yazino account (displayName={}, emailAddress={}, passwordPresent={})",
                displayName, emailAddress, password != null);

        try {
            checkNotNull(displayName, "displayName");
            checkNotNull(emailAddress, "emailAddress");
            checkNotNull(password, "password");

            final LobbySession session = lobbySessionCache.getActiveSession(request);
            if (session == null) {
                LOG.warn("attempt to convert account with a null session.");
                responseWriter.writeError(response, SC_FORBIDDEN, "no session");
            } else {
                PlayerProfileServiceResponse profileServiceResponse =
                        playerProfileService
                                .convertGuestToYazinoAccount(
                                        session.getPlayerId(), emailAddress, password, displayName);
                if (profileServiceResponse.isSuccessful()) {
                    responseWriter.writeOk(response, createSuccessfulConversionResponse(displayName));
                } else {
                    responseWriter.writeOk(response, new ConversionResponse(toBusinessErrors(profileServiceResponse.getErrors())));
                }
            }
        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, SC_BAD_REQUEST, e.getMessage());
        }
    }

    @RequestMapping(value = "/api/1.0/registration/guest/convert/FACEBOOK", method = RequestMethod.POST)
    public void convertGuestToFacebookAccount(HttpServletRequest request,
                                              HttpServletResponse response,
                                              @RequestParam("gameType") String gameType,
                                              @RequestParam("accessToken") String accessToken) throws IOException {

        LOG.info("convert guest account to facebook account (accessToken={})", accessToken);

        try {
            checkNotNull(gameType, "gameType");
            checkNotNull(accessToken, "accessToken");

            final LobbySession session = lobbySessionCache.getActiveSession(request);
            if (session == null) {
                LOG.warn("attempt to convert account with a null session.");
                responseWriter.writeError(response, SC_FORBIDDEN, "no session");
            } else {
                FacebookAppConfiguration facebookAppConfiguration = facebookConfiguration.getAppConfigFor(gameType, CANVAS, LOOSE);
                PlayerInformationHolder userInformationHolder = facebookUserInformationProvider
                        .getUserInformationHolder(accessToken,
                                null,
                                request.getRemoteAddr(),
                                facebookAppConfiguration.isCanvasActionsAllowed());
                if (userInformationHolder == null || userInformationHolder.getPlayerProfile() == null) {
                    responseWriter.writeError(response, SC_BAD_REQUEST, "unable to load user with access-token");
                    return;
                }
                PlayerProfile profile = userInformationHolder.getPlayerProfile();
                PlayerProfileServiceResponse profileServiceResponse =
                        playerProfileService.convertGuestToFacebookAccount(
                                session.getPlayerId(),
                                profile.getExternalId(),
                                profile.getDisplayName(),
                                profile.getEmailAddress());
                // TODO we may need to update player details in the active lobby session
                if (profileServiceResponse.isSuccessful()) {
                    responseWriter.writeOk(response, createSuccessfulConversionResponse(profile.getDisplayName()));
                } else {
                    responseWriter.writeOk(response, new ConversionResponse(toBusinessErrors(profileServiceResponse.getErrors())));
                }
            }
        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, SC_BAD_REQUEST, e.getMessage());
        }
    }

    @RequestMapping(value = "/api/1.0/registration/guest/convert/TANGO", method = RequestMethod.POST)
    public void convertGuestToTangoAccount(HttpServletRequest request,
                                           HttpServletResponse response,
                                           @RequestParam("gameType") String gameType,
                                           @RequestParam("encryptedData") String encryptedData
    ) throws IOException {
        LOG.info("convert guest account to tango account");

        try {
            checkNotNull(gameType, "gameType");
            checkNotNull(encryptedData, "encryptedData");

            final LobbySession session = lobbySessionCache.getActiveSession(request);
            if (session == null) {
                LOG.warn("attempt to convert account with a null session.");
                responseWriter.writeError(response, SC_FORBIDDEN, "no session");
                return;
            }
            final PlayerInformationHolder userInformationHolder = tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(
                    encryptedData);

            if (userInformationHolder == null || userInformationHolder.getPlayerProfile() == null) {
                responseWriter.writeError(response, SC_BAD_REQUEST, "unable to load user with access-token");
                return;
            }
            PlayerProfile profile = userInformationHolder.getPlayerProfile();
            PlayerProfileServiceResponse profileServiceResponse =
                    playerProfileService.convertGuestToExternalAccount(
                            session.getPlayerId(),
                            profile.getExternalId(),
                            profile.getDisplayName(),
                            profile.getEmailAddress(),
                            "TANGO");//provider
            // TODO we may need to update player details in the active lobby session
            if (profileServiceResponse.isSuccessful()) {
                responseWriter.writeOk(response, createSuccessfulConversionResponse(profile.getDisplayName()));
            } else {
                responseWriter.writeOk(response, new ConversionResponse(toBusinessErrors(profileServiceResponse.getErrors())));
            }

        } catch (IllegalArgumentException e) {
            responseWriter.writeError(response, SC_BAD_REQUEST, e.getMessage());
        } catch (GeneralSecurityException e) {
            responseWriter.writeError(response, SC_UNAUTHORIZED, e.getMessage());
        }
    }

    private void checkNotNull(String displayName, final String parameterName) {
        // not using the available checkNotNull method as it doesn't throw the required class of exception (IllegalArgumentException)
        checkArgument(displayName != null, "parameter '" + parameterName + "' is missing");
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    static class ConversionResponse {

        @JsonProperty
        private boolean successful;
        @JsonProperty
        private Set<BusinessError> errors;
        @JsonProperty
        private String displayName;

        private ConversionResponse() {
            /* for jackson */
        }

        ConversionResponse(Set<BusinessError> errors) {
            Validate.notNull(errors, "errors");
            successful = false;
            this.errors = errors;
        }

        boolean isSuccessful() {
            return successful;
        }

        Set<BusinessError> getErrors() {
            return errors;
        }

        String getDisplayName() {
            return displayName;
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
            ConversionResponse rhs = (ConversionResponse) obj;
            return new EqualsBuilder()
                    .append(this.successful, rhs.successful)
                    .append(this.errors, rhs.errors)
                    .append(this.displayName, rhs.displayName)
                    .isEquals();
        }


        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(successful)
                    .append(errors)
                    .append(displayName)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("successful", successful)
                    .append("errors", errors)
                    .append("displayName", displayName)
                    .toString();
        }

        public static ConversionResponse createSuccessfulConversionResponse(String displayName) {
            ConversionResponse response = new ConversionResponse();
            response.successful = true;
            response.displayName = displayName;
            return response;
        }
    }
}
