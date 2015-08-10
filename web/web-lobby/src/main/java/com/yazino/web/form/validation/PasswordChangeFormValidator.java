package com.yazino.web.form.validation;

import com.yazino.platform.player.PasswordChangeRequest;
import com.yazino.platform.player.PlayerProfileAuthenticationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.platform.player.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

import static org.apache.commons.lang3.Validate.notNull;

@Service("passwordChangeFormValidator")
public class PasswordChangeFormValidator implements Validator {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordChangeFormValidator.class);

    private final AuthenticationService authenticationService;
    private final PlayerProfileService playerProfileService;

    @Autowired
    public PasswordChangeFormValidator(final AuthenticationService authenticationService,
                                       final PlayerProfileService playerProfileService) {
        notNull(authenticationService, "authenticationService may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");

        this.authenticationService = authenticationService;
        this.playerProfileService = playerProfileService;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return PasswordChangeRequest.class.equals(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        final PasswordChangeRequest passwordChangeForm = (PasswordChangeRequest) target;

        ValidationTools.validatePassword(errors, "newPassword", passwordChangeForm.getNewPassword());

        if (!errors.hasFieldErrors("newPassword")) {
            ValidationTools.validateMatchingValues(errors,
                    "newPassword",
                    passwordChangeForm.getNewPassword(),
                    passwordChangeForm.getConfirmNewPassword());
        }

        ValidationTools.validateNotEmptyOrWhitespace(errors, "currentPassword", passwordChangeForm.getCurrentPassword());

        if (!errors.hasFieldErrors("currentPassword")) {
            final String loginEmail = playerProfileService.findLoginEmailByPlayerId(passwordChangeForm.getPlayerId());
            final PlayerProfileAuthenticationResponse authentication = authenticationService.authenticateYazinoUser(
                    loginEmail, passwordChangeForm.getCurrentPassword());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication result for {} with password {} was {}",
                        loginEmail, passwordChangeForm.getCurrentPassword(), authentication);
            }

            if (!authentication.isSuccessful()) {
                errors.rejectValue("currentPassword",
                        ValidationTools.ERROR_CODE_NON_MATCHING, "incorrect password");
            }
        }

        // This is a horrid hack to get around the MobilePlayerProfileController expecting validation to
        // be performed against a single value, and worse, not writing tests that tested this *sigh*.
        if (errors.hasFieldErrors()) {
            for (FieldError fieldError : errors.getFieldErrors()) {
                errors.rejectValue("password", fieldError.getDefaultMessage());
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Validation errors for {} are {}", target, errors);
        }
    }
}
