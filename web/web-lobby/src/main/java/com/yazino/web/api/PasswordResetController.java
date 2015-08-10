package com.yazino.web.api;

import com.yazino.platform.player.ResetPasswordResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.domain.email.ResetPasswordEmailBuilder;
import com.yazino.web.form.ResetPasswordForm;
import com.yazino.web.form.validation.ValidationTools;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * A controller that handles the resetting of a user's password.
 */
@Controller
@AllowPublicAccess
@RequestMapping("/api/1.0/player/transactions/reset-password")
public class PasswordResetController {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetController.class);

    private static final String EMAIL_PARAM = "email";

    private final PlayerProfileService playerProfileService;
    private final QuietPlayerEmailer emailer;
    private final WebApiResponses webApiResponses;
    private final SpringErrorResponseFormatter springErrorResponseFormatter;

    @Autowired
    public PasswordResetController(final PlayerProfileService playerProfileService,
                                   final QuietPlayerEmailer emailer,
                                   final WebApiResponses webApiResponses,
                                   final SpringErrorResponseFormatter springErrorResponseFormatter) {
        notNull(playerProfileService);
        notNull(emailer);
        notNull(webApiResponses, "webApiResponses may not be null");
        notNull(springErrorResponseFormatter, "springErrorResponseFormatter may not be null");

        this.playerProfileService = playerProfileService;
        this.emailer = emailer;
        this.webApiResponses = webApiResponses;
        this.springErrorResponseFormatter = springErrorResponseFormatter;
    }

    /**
     * Resets the password for the specified email address
     *
     * @param response the response
     * @param form     the form with the email address
     * @param result   the errors
     */
    @RequestMapping(method = RequestMethod.POST)
    public void resetPassword(HttpServletResponse response,
                              @ModelAttribute("resetPassword") ResetPasswordForm form,
                              BindingResult result) throws IOException {
        String email = form.getEmail();
        ValidationTools.validateEmailAddress(result, EMAIL_PARAM, email);
        if (result.hasFieldErrors(EMAIL_PARAM)) {
            LOG.debug("Email [{}] failed validation, errors were {}", email, result.getFieldError(EMAIL_PARAM));
            webApiResponses.write(response, HttpServletResponse.SC_BAD_REQUEST, springErrorResponseFormatter.toJson(result));
            return;
        }

        ResetPasswordResponse resetResponse = playerProfileService.resetPassword(email);
        if (!resetResponse.isSuccessful()) {
            LOG.debug("Failed to reset password (service response was null) for email [{}]", email);
            standardRejection(result);
            webApiResponses.write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, springErrorResponseFormatter.toJson(result));
            return;
        }

        ResetPasswordEmailBuilder builder = new ResetPasswordEmailBuilder(email, resetResponse.getPlayerName(), resetResponse.getNewPassword());
        boolean success = emailer.quietlySendEmail(builder);
        if (!success) {
            LOG.debug("Failed to send password to email [{}]", email);
            standardRejection(result);
            webApiResponses.write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, springErrorResponseFormatter.toJson(result));
            return;
        }

        LOG.debug("Successfully reset password for email [{}]", email);
        sendSuccess(response);
    }

    private void sendSuccess(final HttpServletResponse response)
            throws IOException {
        final Map<String, Object> result = new HashMap<>();
        result.put("globalMessages", asList(message("success", "Your new password has been emailed to you.")));
        webApiResponses.writeOk(response, result);
    }

    private Map<String, Object> message(final String code,
                                        final String message) {
        final Map<String, Object> json = new HashMap<>();
        json.put("code", code);
        json.put("message", message);
        return json;
    }

    private void standardRejection(BindingResult result) {
        result.reject("failure", "There was a problem resetting your password, please contact customer services");
    }

}
