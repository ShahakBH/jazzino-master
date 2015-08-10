package com.yazino.web.controller;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.worker.message.VerificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.yazino.platform.worker.message.VerificationType.forId;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class VerificationController {

    private final PlayerProfileService playerProfileService;

    @Autowired
    public VerificationController(final PlayerProfileService playerProfileService) {
        notNull(playerProfileService, "playerProfileService may not be null");

        this.playerProfileService = playerProfileService;
    }

    @RequestMapping("/verify/{emailAddress}/{verificationType}/{verificationIdentifier}")
    public ModelAndView verifyPlayer(
            @PathVariable("emailAddress") final String emailAddress,
            @PathVariable("verificationType") final String verificationTypeId,
            @PathVariable("verificationIdentifier") final String verificationIdentifier,
            final HttpServletResponse response)
            throws IOException {
        final VerificationType verificationType = forId(verificationTypeId);
        if (emailAddress == null || verificationType == null || verificationIdentifier == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        final boolean verifySucceeded = playerProfileService.verify(
                emailAddress, verificationIdentifier, verificationType);

        final ModelMap model = new ModelMap();
        model.addAttribute("verificationResult", verifySucceeded);

        return new ModelAndView("verification", model);
    }
}
