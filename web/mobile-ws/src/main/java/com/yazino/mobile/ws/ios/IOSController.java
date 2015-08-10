package com.yazino.mobile.ws.ios;

import com.yazino.mobile.ws.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.yazino.mobile.ws.ModelAttributeKeys.*;
import static org.apache.commons.lang3.Validate.notNull;


/**
 * Handles all requests to /ios/
 * Note due to the nature of controllers, all instance variables in this class are expected to
 * be initialized *before* this object serves requests. They are then considered immutable.
 */

@Controller
@RequestMapping(value = "/ios/*")
public class IOSController {
    private static final Logger LOG = LoggerFactory.getLogger(IOSController.class);

    private final LightstreamerConfig lightstreamerConfig;
    private final FacebookConfig facebookConfig;
    private final IOSConfig iosConfig;
    private final GamesConfig gamesConfig;
    private final TapjoyConfig tapjoyConfig;
    private final ResourceConfig resourceConfig;

    @Autowired
    public IOSController(final LightstreamerConfig lightstreamerConfig,
                         final FacebookConfig facebookConfig,
                         final IOSConfig iosConfig,
                         final GamesConfig gamesConfig,
                         final TapjoyConfig tapjoyConfig,
                         final ResourceConfig resourceConfig) {
        notNull(lightstreamerConfig, "lightstreamerConfig may not be null");
        notNull(facebookConfig, "facebookConfig may not be null");
        notNull(iosConfig, "iosConfig may not be null");
        notNull(gamesConfig, "gamesConfig may not be null");
        notNull(tapjoyConfig, "tapjoyConfig may not be null");
        notNull(resourceConfig, "resourceConfig may not be null");

        this.lightstreamerConfig = lightstreamerConfig;
        this.facebookConfig = facebookConfig;
        this.iosConfig = iosConfig;
        this.gamesConfig = gamesConfig;
        this.tapjoyConfig = tapjoyConfig;
        this.resourceConfig = resourceConfig;
    }

    @Deprecated
    @RequestMapping(value = "/bootstrap/1.0/{filename}.json")
    public ModelAndView handleLegacyConfigRequest(@PathVariable final String filename,
                                                  final HttpServletRequest request) {
        String agent = request.getHeader("user-agent");
        // best guess at which game we're playing
        String game = "yazinoapp";
        if (agent != null && !agent.contains("Wheel")) {
            game = "blackjack";
        }
        return buildModelAndView("ios/bootstrap/shared/%s/%s", game, "1.0", filename);
    }

    @Deprecated
    @RequestMapping(value = {"/bootstrap/{game}/{version}/{filename}", "{game}/bootstrap/{version}/{filename}"})
    public ModelAndView handleBootstrapRequest(@PathVariable final String game,
                                               @PathVariable final String version,
                                               @PathVariable final String filename) {

        return buildModelAndView("ios/bootstrap/shared/%s/%s", game, version, filename);
    }

    @RequestMapping(value = "/bootstrap/{version}/{game}")
    public ModelAndView handleBootstrapRequest(@PathVariable String version,
                                               @PathVariable String game) throws IOException {
        return buildModelAndView("ios/bootstrap/shared/%s/%s", game, version, "config");
    }

    @RequestMapping(value = {"/{game}/resources/{version}/{filename}", "/{game}/resources/{version}/{gameIdentifier}/{filename}"})
    public ModelAndView handleResourceRequest(@PathVariable final String game,
                                              @PathVariable final String version,
                                              @PathVariable final String filename) {
        return buildModelAndView("ios/resources/shared/%s/%s", game, version, filename);
    }

    private ModelAndView buildModelAndView(String pathTemplate, String game, String version, String filename) {
        LOG.debug("Handing request for bootstrap info for {}, version {}, filename {}", game, version, filename);
        String gameIdentifier = iosConfig.getIdentifiers().get(game.toLowerCase());
        if (gameIdentifier == null) {
            return null;
        }

        String path = String.format(pathTemplate, version, filename);
        LOG.debug("Game {} path is {}", gameIdentifier, path);

        ModelAndView modelAndView = new ModelAndView(path);
        modelAndView.addObject(GAME_TYPE, gameIdentifier);
        modelAndView.addObject(LIGHTSTREAMER, lightstreamerConfig);
        modelAndView.addObject(FACEBOOK, facebookConfig);
        modelAndView.addObject(RESOURCES, resourceConfig);
        modelAndView.addObject(IOS_CONFIG, iosConfig);
        modelAndView.addObject(GAMES_CONFIG, gamesConfig);
        modelAndView.addObject(IOS_GAME_IDENTIFIER, game);
        modelAndView.addObject(TAPJOY_CONFIG, tapjoyConfig);

        return modelAndView;

    }

}
