package com.yazino.web.controller;

import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

@Controller
public class MobilePlayerReferrerController {

    private static final Logger LOG = LoggerFactory.getLogger(MobilePlayerReferrerController.class);

    private final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;

    @Autowired
    public MobilePlayerReferrerController(@Qualifier("playerReferrerEventQueuePublishingService")
                                          final QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService) {

        this.playerReferrerEventService = playerReferrerEventService;
    }

    @RequestMapping("/public/playerRegistrationSource/{platform}/{gameType}")
    public void referrer(@PathVariable final String platform,
                         @PathVariable final String gameType,
                         @RequestParam(value = "playerId", required = true) final String playerId,
                         @RequestParam(value = "ref", required = true) final String ref,
                         final HttpServletResponse response) throws WalletServiceException, IOException {

        if (StringUtils.isBlank(gameType) || StringUtils.isBlank(platform) || StringUtils.isBlank(playerId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!isValidPlatform(platform)) {
            final String msg = String.format("Unsupported platform sent to MobilePlayerReferrerController, %s", platform);
            LOG.warn(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final BigDecimal player = new BigDecimal(playerId);

        final PlayerReferrerEvent event = new PlayerReferrerEvent(player, ref, platform, gameType);
        playerReferrerEventService.send(event);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isValidPlatform(final String platform) {
        for (Platform plat : Platform.values()) {
            if (plat.name().equals(platform)) {
                return true;
            }
        }
        return false;
    }
}
