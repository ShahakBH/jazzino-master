package com.yazino.web.api;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
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
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Controls endpoints for the /player resource.
 */
@Controller
@RequestMapping("/api/1.0/player/find")
public class PlayersResourceController {

    private static final Logger LOG = LoggerFactory.getLogger(PlayersResourceController.class);

    private final FindPlayersFormValidator validator = new FindPlayersFormValidator();
    private final PlayerProfileService playerProfileService;
    private final WebApiResponses webApiResponses;
    private final SpringErrorResponseFormatter springErrorResponseFormatter;

    @Autowired
    public PlayersResourceController(PlayerProfileService playerProfileService,
                                     final WebApiResponses webApiResponses,
                                     final SpringErrorResponseFormatter springErrorResponseFormatter) {
        notNull(playerProfileService);
        notNull(webApiResponses, "webApiResponses may not be null");
        notNull(springErrorResponseFormatter, "springErrorResponseFormatter may not be null");

        this.playerProfileService = playerProfileService;
        this.webApiResponses = webApiResponses;
        this.springErrorResponseFormatter = springErrorResponseFormatter;
    }

    /**
     * Perform a search for players.
     * This is a POST because the providerIds parameter is of indeterminate length.
     * Hence the rpc style path of find.
     *
     * @param response never null
     * @param form     the details of what to find
     * @param result   the binding result for validation purposes
     */
    @RequestMapping(method = RequestMethod.POST)
    public void findPlayers(final HttpServletResponse response,
                            @ModelAttribute("findPlayersForm") final FindPlayersForm form,
                            final BindingResult result) throws IOException {
        validator.validate(form, result);
        if (result.hasErrors()) {
            webApiResponses.write(response, HttpServletResponse.SC_BAD_REQUEST, springErrorResponseFormatter.toJson(result));
            return;
        }

        final String provider = form.getProvider();
        final String[] providerIds = StringUtils.stripAll(form.getProviderIds().split(","));

        final Map<String, BigDecimal> found = new HashMap<>();
        if (validator.isYazinoProvider(provider)) {
            found.putAll(playerProfileService.findByEmailAddresses(providerIds));
        }
        if (validator.isFacebookProvider(provider)) {
            found.putAll(playerProfileService.findByProviderNameAndExternalIds(provider, providerIds));
        }

        LOG.debug("Found {} results for provider {} out of {} ids", found.size(), provider, providerIds.length);

        final Map<String, Map<String, BigDecimal>> model = new HashMap<>();
        model.put("players", found);
        webApiResponses.writeOk(response, model);
    }


}
