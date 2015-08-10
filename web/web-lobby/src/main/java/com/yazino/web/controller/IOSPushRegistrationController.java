package com.yazino.web.controller;

import com.yazino.mobile.yaps.config.TypedMapBean;
import com.yazino.mobile.yaps.message.PlayerDevice;
import com.yazino.platform.messaging.publisher.SafeQueuePublishingEventService;
import com.yazino.spring.security.AllowPublicAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This controller provides a way for ios devices to register to receive push notifications.
 */
@Controller
@AllowPublicAccess("/ios-push-messaging/**")
@RequestMapping("/ios-push-messaging")
public class IOSPushRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(IOSPushRegistrationController.class);
    private static final String AGL_BLACKJACK_BUNDLE = "com.yazino.Blackjack";
    private static final String AGL_WHEELDEAL_BUNDLE = "com.yazino.YazinoApp";
    private static final String YAZINO_HIGHSTAKES_BUNDLE = "yazino.HighStakes";
    private static final String SLOTS = "SLOTS";
    private static final String BLACKJACK = "BLACKJACK";

    private final Map<String, String> bundleMappings;
    private final SafeQueuePublishingEventService<PlayerDevice> playerDeviceQueuePublishingService;

    @Autowired(required = true)
    public IOSPushRegistrationController(@Qualifier("playerDeviceQueuePublishingService") final SafeQueuePublishingEventService<PlayerDevice>
                                                 playerDeviceQueuePublishingService,
                                         @Qualifier("bundleMappings") final TypedMapBean<String, String> bundleMappingsBean) {
        notNull(playerDeviceQueuePublishingService);
        notNull(bundleMappingsBean);
        this.playerDeviceQueuePublishingService = playerDeviceQueuePublishingService;
        this.bundleMappings = bundleMappingsBean.getSource();
        LOG.info("Configured with playerDeviceQueuePublishingService {} and bundle mappings {}", playerDeviceQueuePublishingService, bundleMappingsBean);
    }

    /**
     * Perform the registration.
     *
     * @param gameType    strata's game identifier, not null or empty
     * @param playerId    the playerId, not null or empty, player must exist or this call is ignored
     * @param deviceToken the token of the device, not null or empty
     * @param response    the http response
     */
    @Deprecated
    @RequestMapping(value = "/pushRegistration", method = RequestMethod.POST)
    public void registerDeviceForPush(@RequestParam("gameType") final String gameType,
                                      @RequestParam("playerId") final BigDecimal playerId,
                                      @RequestParam("deviceToken") final String deviceToken,
                                      final HttpServletResponse response) {
        String bundle = mappedBundle(gameType);
        registerDevice(gameType, bundle, playerId, deviceToken);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private String mappedBundle(String gameType) {
        // this is for legacy support of advanced games lab blackjack/slots and v1.0 of yazino's highstakes
        if (gameType.equalsIgnoreCase(SLOTS)) {
            return AGL_WHEELDEAL_BUNDLE;
        }
        if (gameType.equalsIgnoreCase(BLACKJACK)) {
            return AGL_BLACKJACK_BUNDLE;
        }
        return YAZINO_HIGHSTAKES_BUNDLE;
    }

    @RequestMapping(value = "/{bundle}/devices", method = RequestMethod.POST)
    public void registerDevice(@PathVariable("bundle") String bundle,
                               @RequestParam("playerId") BigDecimal playerId,
                               @RequestParam("deviceToken") String deviceToken,
                               HttpServletResponse response) {

        String gameType = mappedGame(bundle);
        registerDevice(gameType, bundle, playerId, deviceToken);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private String mappedGame(String bundle) {
        return bundleMappings.get(bundle);
    }

    private void registerDevice(String gameType,
                                String bundle,
                                BigDecimal playerId,
                                String deviceToken) {
        if (bundle.equalsIgnoreCase(AGL_BLACKJACK_BUNDLE)) {
            LOG.debug("Ignoring AGL Blackjack device registration for Player [{}] and Token [{}]", playerId, deviceToken);
            return;
        }
        String modifiedToken = deviceToken.replaceAll(" ", "").replace("<", "").replace(">", "");
        final PlayerDevice playerDevice = new PlayerDevice(gameType, playerId, modifiedToken, bundle);

        LOG.debug("Publishing player device [{}] using publishingService [{}]", playerDevice, playerDeviceQueuePublishingService);

        playerDeviceQueuePublishingService.send(playerDevice);
    }

}
