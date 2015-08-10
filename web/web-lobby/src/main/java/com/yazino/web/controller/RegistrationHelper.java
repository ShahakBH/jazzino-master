package com.yazino.web.controller;

import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileRegistrationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.form.RegistrationForm;
import com.yazino.web.form.validation.RegistrationFormValidator;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service("registrationHelper")
public class RegistrationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationHelper.class);

    private final Validator registrationFormValidator = new RegistrationFormValidator();

    private final YazinoWebLoginService yazinoWebLoginService;
    private final CookieHelper cookieHelper;
    private final AuthenticationService authenticationService;
    private final ReferrerSessionCache referrerSessionCache;
    private final RegistrationPerIpLimit registrationPerIpLimit;

    @Autowired
    public RegistrationHelper(final AuthenticationService authenticationService,
                              final YazinoWebLoginService yazinoWebLoginService,
                              final CookieHelper cookieHelper,
                              final ReferrerSessionCache referrerSessionCache,
                              final RegistrationPerIpLimit registrationPerIpLimit) {
        notNull(authenticationService, "authenticationService may not be null");
        notNull(yazinoWebLoginService, "yazinoWebLoginService may not be null");
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(referrerSessionCache, "referrerSessionCache may not be null");
        notNull(yazinoWebLoginService, "yazinoWebLoginService may not be null");

        this.yazinoWebLoginService = yazinoWebLoginService;
        this.cookieHelper = cookieHelper;
        this.authenticationService = authenticationService;
        this.referrerSessionCache = referrerSessionCache;
        this.registrationPerIpLimit = registrationPerIpLimit;
    }

    public RegistrationResult register(final RegistrationForm form,
                                       final BindingResult result,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final Platform platform,
                                       final String gameType,
                                       final Partner partnerId) {
        notNull(form, "Form is required");

        registrationFormValidator.validate(form, result);
        if (result.hasErrors() || result.hasFieldErrors()) {
            return RegistrationResult.FAILURE;
        }

        final String referrer = referrerSessionCache.getReferrer();
        final PlayerProfile playerProfile = playerProfileFrom(form, referralIdFrom(request), partnerId);

        if (registrationPerIpLimit.hasReachedLimit(request)) {
            final String message = "Registration limit for this IP address has been reached. Please try again later";
            addErrorsToBinding(Collections.singleton(new ParameterisedMessage(message)), result);
            return RegistrationResult.FAILURE;
        }

        final PlayerProfileRegistrationResponse registrationResponse = authenticationService.registerYazinoUser(
                form.getEmail(), form.getPassword(), playerProfile, request.getRemoteAddr(), referrer, platform, form.getAvatarURL(), gameType);
        if (!registrationResponse.isSuccessful()) {
            LOG.warn("registration failed for user {} because {}", playerProfile.getRealName(), registrationResponse.getErrors());

            /*
            * if this happens the errors will display at the top of the page because
            * we don't know which fields to bind them to
             */
            addErrorsToBinding(registrationResponse.getErrors(), result);
            return RegistrationResult.FAILURE;
        }

        registrationPerIpLimit.recordRegistration(request);

        final YazinoWebLoginService.NewlyRegisteredUserLoginResult loginResponse = yazinoWebLoginService.loginNewlyRegisteredUser(
                request, response, form.getEmail(), form.getPassword(), platform, partnerId);
        if (loginResponse.getLoginResult() != LoginResult.NEW_USER) {
            return null;
        }

        return RegistrationResult.SUCCESS;
    }

    private String referralIdFrom(final HttpServletRequest request) {
        final BigDecimal referralPlayerId = cookieHelper.getReferralPlayerId(request.getCookies());
        final String referralId;
        if (referralPlayerId != null) {
            referralId = referralPlayerId.toString();
        } else {
            referralId = null;
        }

        LOG.debug("Referral id is {}", referralPlayerId);
        return referralId;
    }

    private PlayerProfile playerProfileFrom(final RegistrationForm form, final String referralId, final Partner partnerId) {
        final PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setEmailAddress(form.getEmail());
        playerProfile.setDisplayName(form.getDisplayName());
        playerProfile.setReferralIdentifier(referralId);
        playerProfile.setProviderName("YAZINO");
        playerProfile.setOptIn(form.getOptIn());
        playerProfile.setGuestStatus(GuestStatus.NON_GUEST);
        playerProfile.setPartnerId(partnerId);
        return playerProfile;
    }

    private void addErrorsToBinding(final Set<ParameterisedMessage> messages,
                                    final BindingResult result) {
        for (final ParameterisedMessage message : messages) {
            result.addError(new ObjectError("global", message.getMessage()));
        }
    }
}
