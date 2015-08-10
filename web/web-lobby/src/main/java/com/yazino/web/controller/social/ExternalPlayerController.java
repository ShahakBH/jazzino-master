package com.yazino.web.controller.social;

import com.restfb.util.StringUtils;
import com.yazino.platform.player.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Set;

@Controller
public class ExternalPlayerController {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalPlayerController.class);

    private final PlayerProfileService playerProfileService;

    @Autowired
    public ExternalPlayerController(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @RequestMapping({"/api/1.0/external/check_registered/{providerName}"})
    @ResponseBody
    public Set<String> checkRegisteredFacebookUsers(@PathVariable("providerName") String providerName,
                                                    @RequestParam("externalIds") final String commaSeparatedExternalIds) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Checking if external %s ids are registered: %s", providerName, commaSeparatedExternalIds));
        }
        if (StringUtils.isBlank(commaSeparatedExternalIds)) {
            return Collections.emptySet();
        }
        final String[] candidateIds = splitAndTrim(commaSeparatedExternalIds);
        return playerProfileService.findRegisteredExternalIds(providerName, candidateIds);
    }

    private String[] splitAndTrim(final String sendToEmailAddresses) {
        final String[] addresses = sendToEmailAddresses.split(",");
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = addresses[i].trim();
        }
        return addresses;
    }

}
