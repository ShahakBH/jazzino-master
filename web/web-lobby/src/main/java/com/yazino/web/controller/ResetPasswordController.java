package com.yazino.web.controller;

import com.yazino.platform.player.ResetPasswordResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.ResetPasswordEmailBuilder;
import com.yazino.web.form.LoginForm;
import com.yazino.web.form.ResetPasswordForm;
import com.yazino.web.form.WebLoginForm;
import com.yazino.web.service.QuietPlayerEmailer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class ResetPasswordController {
    private static final Logger LOG = LoggerFactory.getLogger(ResetPasswordController.class);
    public static final String RESET_PASSWORD_VIEW = "resetPassword";
    public static final String RESET_PASSWORD_SUCCESSFUL_VIEW = "partials/loginPanel";
    public static final String RESET_PASSWORD_VIEW_PARTIAL = "partials/resetPasswordPanel";

    private final PlayerProfileService playerProfileService;
    private final QuietPlayerEmailer emailer;

    @Autowired(required = true)
    public ResetPasswordController(final PlayerProfileService playerProfileService,
                                   final QuietPlayerEmailer emailer) {
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(emailer, "emailer may not be null");
        this.playerProfileService = playerProfileService;
        this.emailer = emailer;
    }

    @RequestMapping(value = {"/resetPassword", "/public/resetPassword"}, method = RequestMethod.GET)
    public String viewForm(
            final ModelMap model,
            @RequestParam(value = "game", defaultValue = "slots", required = false) final String game,
            @RequestParam(value = "partial", defaultValue = "false", required = false) final boolean partial,
            @RequestParam(value = "email", defaultValue = "", required = false) final String email) {
        final ResetPasswordForm form = new ResetPasswordForm();
        if (StringUtils.isNotBlank(email)) {
            form.setEmail(email);
        }

        model.addAttribute(RESET_PASSWORD_VIEW, form);
        model.addAttribute("partial", partial);
        model.addAttribute("game", game);
        return resetPasswordView(partial);
    }

    @RequestMapping(value = {"/resetPassword", "/public/resetPassword"}, method = RequestMethod.POST)
    public String processSubmit(@ModelAttribute(RESET_PASSWORD_VIEW) final ResetPasswordForm form,
                                final BindingResult result,
                                final ModelMap model,
                                @RequestParam(value = "game", defaultValue = "slots", required = false) final String game,
                                @RequestParam(value = "partial", defaultValue = "false", required = false) final boolean partial) {
        notNull(form, "Form is required");

        final String email = form.getEmail();
        model.addAttribute("partial", partial);
        model.addAttribute("game", game);

        if (StringUtils.isBlank(email)) {
            return resetPasswordView(partial);

        }

        final ResetPasswordResponse resetResponse = playerProfileService.resetPassword(email);
        if (resetResponse.isSuccessful()) {
            try {
                final ResetPasswordEmailBuilder builder = new ResetPasswordEmailBuilder(
                        email, resetResponse.getPlayerName(), resetResponse.getNewPassword());
                final boolean success = emailer.quietlySendEmail(builder);
                if (!success) {
                    appendResetError(result);
                    return resetPasswordView(partial);
                }
            } catch (Exception e) {
                LOG.error("Error resetting password for user with email {}", email, e);
                appendResetError(result);
                return resetPasswordView(partial);
            }
        } else {
            LOG.debug("Failure resetting password for user with email {}. Reset was unsuccessful.", email);
            result.addError(new ObjectError(RESET_PASSWORD_VIEW,
                    "There was a problem resetting your password, please contact customer services"));
            return resetPasswordView(partial);
        }

        final LoginForm loginForm = new WebLoginForm();
        loginForm.setEmail(email);
        model.addAttribute("loginForm", loginForm);
        model.addAttribute("loginMessage", "Your new Yazino password has been sent to your email address.");
        return RESET_PASSWORD_SUCCESSFUL_VIEW;
    }

    private String resetPasswordView(final boolean partial) {
        if (partial) {
            return RESET_PASSWORD_VIEW_PARTIAL;
        } else {
            return RESET_PASSWORD_VIEW;
        }
    }

    private void appendResetError(final BindingResult result) {
        result.addError(new ObjectError(RESET_PASSWORD_VIEW,
                "There was a problem resetting your password, please contact customer services"));
    }
}
