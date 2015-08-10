package com.yazino.web.controller;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.form.MobileRegistrationForm;
import com.yazino.web.session.PlatformReportingHelper;
import com.yazino.web.util.MobileRequestGameGuesser;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class MobileRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(MobileRegistrationController.class);
    private static final String GAME_REFERRER = "gameReferrer";
    private static final String GAME = "game";
    private static final String LEGACY_REGISTRATION_PATH = "/public/mobileRegistration";
    private static final String REGISTRATION_VIEW = "registration";

    private final MobileRequestGameGuesser requestGameGuesser = new MobileRequestGameGuesser();
    private final GameTypeRepository gameTypeRepository;
    private final RegistrationHelper registrationHelper;
    private final WebApiResponses webApiResponses;

    private String defaultImage = "/images/gloss/friend-bar-none-photo.png";

    @Autowired
    public MobileRegistrationController(final RegistrationHelper registrationHelper,
                                        final GameTypeRepository gameTypeRepository,
                                        final WebApiResponses webApiResponses) {
        notNull(registrationHelper, "registrationHelper may not be null");
        notNull(gameTypeRepository, "gameTypeRepository may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.gameTypeRepository = gameTypeRepository;
        this.registrationHelper = registrationHelper;
        this.webApiResponses = webApiResponses;
    }

    @Autowired
    public void setSiteConfiguration(final SiteConfiguration siteConfiguration) {
        if (siteConfiguration.getAssetUrl() != null) {
            final String assetUrl = siteConfiguration.getAssetUrl();
            defaultImage = assetUrl + "/images/gloss/friend-bar-none-photo.png";
        }
        LOG.debug("Setting default avatar for mobile registrations to [{}]", defaultImage);
    }

    @RequestMapping(value = LEGACY_REGISTRATION_PATH, method = RequestMethod.GET)
    public void viewMobileRegistration(final ModelMap model,
                                       final BindingResult result,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response)
            throws IOException {
        LOG.info("LEGACY - legacy path called: GET on {} by {}", LEGACY_REGISTRATION_PATH, request.getHeader("User-Agent"));

        String gameType = request.getParameter(GAME);
        if (LOG.isDebugEnabled()) {
            LOG.debug("viewMobileRegistration:mobileGame was [{}]", gameTypeOf(gameType));
        }
        if (StringUtils.isBlank(gameType)) {
            gameType = MobileRequestGameGuesser.SLOTS;
        }
        model.addAttribute(GAME_REFERRER, gameType);

        final MobileRegistrationForm form = new MobileRegistrationForm();
        model.addAttribute(REGISTRATION_VIEW, form);

        webApiResponses.writeOk(response, registrationJson(result));
    }

    @Deprecated
    @RequestMapping(value = LEGACY_REGISTRATION_PATH, method = RequestMethod.POST)
    public void processSubmit(@ModelAttribute(REGISTRATION_VIEW) final MobileRegistrationForm form,
                              final BindingResult result,
                              final HttpServletRequest request,
                              final HttpServletResponse response,
                              @RequestParam(value = "mobileType", required = false) final String mobileType)
            throws IOException {
        LOG.info("LEGACY - legacy path called: POST on {} by {}", LEGACY_REGISTRATION_PATH, request.getHeader("User-Agent"));

        Platform platform = convertLegacyPlatform(mobileType);

        final String gameType = requestGameGuesser.guessGame(request, platform);

        final String platformValue = platform.name();
        final String url = request.getRequestURL().toString();

        final String modifiedPath = String.format("/public/registration/%s/%s", platformValue, gameType);
        final String requestURL = url.replace(LEGACY_REGISTRATION_PATH, modifiedPath);

        LOG.debug("Found game type as {} for platform {} so setting url to {}", gameType, platformValue, requestURL);
        request.setAttribute(PlatformReportingHelper.REQUEST_URL, requestURL);

        processRegistration(form, request, result, response, platform, gameType);
    }

    @RequestMapping(value = "/public/registration/{platform}/{game}", method = RequestMethod.POST)
    public void register(@ModelAttribute(REGISTRATION_VIEW) final MobileRegistrationForm form,
                         final BindingResult result,
                         final HttpServletRequest request,
                         final HttpServletResponse response,
                         @PathVariable("platform") final String platformInput,
                         @PathVariable("game") final String gameType) throws IOException {

        LOG.debug("Form was {}, game type {}, platform {}", form, gameType, platformInput);
        Platform platform;

        try {
            platform = Platform.valueOf(platformInput);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        processRegistration(form, request, result, response, platform, gameType);
    }

    private void processRegistration(final MobileRegistrationForm form,
                                     final HttpServletRequest request,
                                     final BindingResult result,
                                     final HttpServletResponse response,
                                     final Platform platform,
                                     final String gameType) throws IOException {

        if (platform == null || !gameTypeRepository.getGameTypes().containsKey(gameType)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (StringUtils.isBlank(form.getAvatarURL())) {
            form.setAvatarURL(defaultImage);
        }
        LOG.debug("Processing registration for game type {} on platform {}", gameType, platform.name());

        Platform convertedPlatform = convertLegacyPlatform(platform.name());

        final RegistrationResult registrationResult = registrationHelper.register(form,
                result,
                request,
                response,
                convertedPlatform,
                gameType,
                Partner.YAZINO);
        if (registrationResult == RegistrationResult.SUCCESS) {
            webApiResponses.writeOk(response, singletonMap("registered", true));
        } else {
            webApiResponses.writeOk(response, registrationJson(result));
        }
    }

    private String gameTypeOf(final String gameType) {
        if (StringUtils.isBlank(gameType)) {
            return "NULL (SLOTS)";
        }
        return gameType;
    }

    // called by legacy apps, can be deleted once all live mobile apps use the new registration url
    private Platform convertLegacyPlatform(final String mobileType) {
        /*
        ANDROID registrations always specify the mobileType as 'ANDROID'. 'old' IOS mobile registrations never
        specified the mobileType request parameter, so if missing the platform is IOS
        */
        try {
            return Platform.valueOf(mobileType);
        } catch (Exception e) {
            return Platform.IOS;
        }
    }

    private Map<String, Object> registrationJson(final BindingResult bindingResult) {
        final Map<String, Object> result = new HashMap<>();
        result.put("registered", bindingResult.getErrorCount() == 0);

        if (bindingResult.getGlobalErrorCount() > 0) {
            final List<String> globalErrors = new ArrayList<>();
            for (ObjectError globalError : bindingResult.getGlobalErrors()) {
                globalErrors.add(globalError.getDefaultMessage());
            }
            result.put("globalErrors", globalErrors);
        }

        if (bindingResult.getFieldErrorCount() > 0) {
            final List<Map<String, Object>> fieldErrors = new ArrayList<>();
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                final Map<String, Object> error = new HashMap<>();
                error.put("field", fieldError.getField());
                error.put("message", fieldError.getDefaultMessage());
                fieldErrors.add(error);
            }
            result.put("fieldErrors", fieldErrors);
        }
        return result;
    }
}
