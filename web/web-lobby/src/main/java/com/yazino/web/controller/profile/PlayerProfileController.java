package com.yazino.web.controller.profile;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.Avatar;
import com.yazino.platform.player.PasswordChangeRequest;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileSummary;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.spring.mvc.DateTimeEditor;
import com.yazino.util.DateUtils;
import com.yazino.web.data.CountryRepository;
import com.yazino.web.data.GenderRepository;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.domain.DisplayName;
import com.yazino.web.domain.EmailAddress;
import com.yazino.web.domain.PlayerProfileSummaryBuilder;
import com.yazino.web.form.validation.DisplayNameValidator;
import com.yazino.web.form.validation.EmailAddressValidator;
import com.yazino.web.form.validation.PasswordChangeFormValidator;
import com.yazino.web.form.validation.PlayerProfileSummaryValidator;
import com.yazino.web.service.PlayerProfileCacheRemovalListener;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.LobbyExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;


@Controller
@RequestMapping("/player/profile")
public class PlayerProfileController extends AbstractProfileController implements HandlerExceptionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerProfileController.class);

    public static final String PLAYER_OBJECT_KEY = "player";
    public static final String PLAYER_INFO_OBJECT_KEY = "playerInfo";
    public static final String DISPLAY_NAME_OBJECT_KEY = "displayName";
    public static final String EMAIL_ADDRESS_OBJECT_KEY = "emailAddress";
    public static final String AVATAR_OBJECT_KEY = "avatar";
    public static final String PASSWORD_CHANGE_OBJECT_KEY = "passwordChange";
    public static final String FB_SYNC = "fbSync";
    public static final String TAB_CODE_NAME = "player";

    private final DateUtils dateUtils = new DateUtils();
    private final PlayerProfileSummaryValidator playerProfileSummaryValidator = new PlayerProfileSummaryValidator();
    private final DisplayNameValidator displayNameValidator = new DisplayNameValidator();
    private final EmailAddressValidator emailAddressValidator = new EmailAddressValidator();

    private final LobbySessionCache lobbySessionCache;
    private final PlayerProfileService playerProfileService;
    private final AvatarRepository avatarRepository;
    private final LobbyExceptionHandler lobbyExceptionHandler;
    private final PasswordChangeFormValidator passwordChangeFormValidator;
    private final PlayerProfileCacheRemovalListener playerProfileCacheRemovalListener;
    private final GenderRepository genderRepository;
    private final CountryRepository countryRepository;
    private final PlayerService playerService;

    @Autowired
    public PlayerProfileController(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
                                   final PlayerProfileService playerProfileService,
                                   @Qualifier("avatarRepository") final AvatarRepository avatarRepository,
                                   final LobbyExceptionHandler lobbyExceptionHandler,
                                   final PasswordChangeFormValidator passwordChangeFormValidator,
                                   final PlayerProfileCacheRemovalListener playerProfileCacheRemovalListener,
                                   final YazinoConfiguration yazinoConfiguration,
                                   final GenderRepository genderRepository,
                                   final CountryRepository countryRepository,
                                   final PlayerService playerService) {
        super(yazinoConfiguration);

        notNull(playerProfileCacheRemovalListener, "playerProfileCacheRemovalListener may not be null");
        notNull(genderRepository, "genderRepository may not be null");
        notNull(countryRepository, "countryRepository may not be null");
        notNull(playerService, "playerService may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.playerProfileService = playerProfileService;
        this.avatarRepository = avatarRepository;
        this.lobbyExceptionHandler = lobbyExceptionHandler;
        this.passwordChangeFormValidator = passwordChangeFormValidator;
        this.playerProfileCacheRemovalListener = playerProfileCacheRemovalListener;
        this.genderRepository = genderRepository;
        this.countryRepository = countryRepository;
        this.playerService = playerService;
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView getMainProfilePage(
            final ModelMap modelMap,
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = PARTIAL_URI_FLAG,
                    defaultValue = PARTIAL_URI_FLAG_DEFAULT,
                    required = PARTIAL_URI_FLAG_IS_REQUIRED) final boolean partial) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }
        return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
    }

    @RequestMapping(value = "/displayName", method = RequestMethod.POST)
    public ModelAndView updateDisplayName(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = PARTIAL_URI_FLAG,
                    defaultValue = "false",
                    required = false) final boolean partial,
            @ModelAttribute(DISPLAY_NAME_OBJECT_KEY) final DisplayName displayName,
            final BindingResult result,
            final ModelMap modelMap) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }

        displayNameValidator.validate(displayName, result);
        if (result.hasErrors()) {
            modelMap.addAttribute("displayNameHasErrors", "true");
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
        }

        playerProfileService.updateDisplayName(lobbySession.getPlayerId(), displayName.getDisplayName());
        playerProfileCacheRemovalListener.playerUpdated(lobbySession.getPlayerId());
        return redirectAfterSuccess(modelMap, partial);
    }

    @RequestMapping(value = "/communicationEmail", method = RequestMethod.POST)
    public ModelAndView updateCommunicationEmailAddress(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = PARTIAL_URI_FLAG,
                    defaultValue = "false",
                    required = false) final boolean partial,
            @ModelAttribute(EMAIL_ADDRESS_OBJECT_KEY) final EmailAddress emailAddress,
            final BindingResult result,
            final ModelMap modelMap) {
        emailAddressValidator.validate(emailAddress, result);

        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }

        if (result.hasErrors()) {
            modelMap.addAttribute("communicationEmailHasErrors", "true");
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
        }

        playerProfileService.updateEmailAddress(lobbySession.getPlayerId(), emailAddress.getEmailAddress());
        playerProfileCacheRemovalListener.playerUpdated(lobbySession.getPlayerId());
        return redirectAfterSuccess(modelMap, partial);
    }

    @RequestMapping(value = "/userProfileInfo", method = RequestMethod.POST)
    public ModelAndView updateUserProfileInfo(final HttpServletRequest request,
                                              final HttpServletResponse response,
                                              @RequestParam(value = PARTIAL_URI_FLAG, defaultValue = "false", required = false) final boolean partial,
                                              @ModelAttribute(PLAYER_INFO_OBJECT_KEY) final PlayerProfileSummary userProfileInfo,
                                              final BindingResult result,
                                              final ModelMap modelMap) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }

        playerProfileSummaryValidator.validate(userProfileInfo, result);
        if (result.hasErrors()) {
            modelMap.addAttribute("playerInfoHasErrors", "true");
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
        }

        playerProfileService.updatePlayerInfo(lobbySession.getPlayerId(), userProfileInfo);
        playerProfileCacheRemovalListener.playerUpdated(lobbySession.getPlayerId());
        return redirectAfterSuccess(modelMap, partial);
    }

    @RequestMapping(value = "/avatar", method = RequestMethod.POST)
    public ModelAndView updateUserAvatar(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         @RequestParam(value = PARTIAL_URI_FLAG, defaultValue = "false", required = false) final boolean partial,
                                         @RequestParam(AVATAR_OBJECT_KEY) final MultipartFile avatar,
                                         final ModelMap modelMap) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }

        try {
            final String originalFilename = avatar.getOriginalFilename();
            if (StringUtils.isBlank(originalFilename)) {
                modelMap.addAttribute("avatarHasErrors", "true");
                return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
            }

            final Avatar storedAvatar = avatarRepository.storeAvatar(originalFilename, avatar.getBytes());

            playerProfileService.updateAvatar(lobbySession.getPlayerId(), storedAvatar);
            playerProfileCacheRemovalListener.playerUpdated(lobbySession.getPlayerId());

            return redirectAfterSuccess(modelMap, partial);

        } catch (IllegalArgumentException e) {
            modelMap.addAttribute("avatarHasErrors", "true");
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);

        } catch (IOException ex) {
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
        }
    }

    @RequestMapping(value = "/password", method = RequestMethod.POST)
    public ModelAndView updatedPassword(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        @RequestParam(value = PARTIAL_URI_FLAG, defaultValue = "false", required = false) final boolean partial,
                                        @ModelAttribute(PASSWORD_CHANGE_OBJECT_KEY) final PasswordChangeRequest passwordChangeForm,
                                        final BindingResult result,
                                        final ModelMap modelMap) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }
        passwordChangeForm.setPlayerId(lobbySession.getPlayerId());

        passwordChangeFormValidator.validate(passwordChangeForm, result);

        if (result.hasErrors()) {
            passwordChangeForm.clear();
            modelMap.addAttribute("emailAddressHasErrors", "true");
            return returnDefaultInfo(lobbySession.getPlayerId(), modelMap, partial, response);
        }

        playerProfileService.updatePassword(lobbySession.getPlayerId(), passwordChangeForm);
        return redirectAfterSuccess(modelMap, partial);
    }

    @RequestMapping(value = "/fbSync", method = RequestMethod.POST)
    public ModelAndView updateFbSync(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = PARTIAL_URI_FLAG,
                    defaultValue = "false",
                    required = false) final boolean partial,
            @RequestParam(value = FB_SYNC, defaultValue = "false", required = false) final boolean fbSync,
            final ModelMap modelMap) {
        final LobbySession lobbySession = getLobbySession(request);
        if (lobbySession == null) {
            return noSessionExists(response);
        }

        playerProfileService.updateSyncFor(lobbySession.getPlayerId(), fbSync);
        return redirectAfterSuccess(modelMap, partial);
    }

    private ModelAndView redirectAfterSuccess(final ModelMap model, final boolean partial) {
        model.clear();
        final StringBuilder urlSb = new StringBuilder("/player/profile");
        if (partial) {
            urlSb.append("?partial=true");
        }
        return new ModelAndView(new RedirectView(urlSb.toString(), false, false, false), model);
    }

    private ModelAndView noSessionExists(final HttpServletResponse response) {
        LOG.debug("No session exists");
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            LOG.warn("Failed to send forbidden status: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return null;
    }

    private LobbySession getLobbySession(final HttpServletRequest request) {
        return lobbySessionCache.getActiveSession(request);
    }

    @ModelAttribute("genders")
    public Map<String, String> populateGenders() {
        return genderRepository.getGenders();
    }

    @ModelAttribute("countries")
    public Map<String, String> populateCountries() {
        return countryRepository.getCountries();
    }

    @ModelAttribute("years")
    public Map<String, String> populateMonthsYears() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        final GregorianCalendar now = new GregorianCalendar();
        final int offsetYear = 100;
        final int[] years = dateUtils.getYearsUntil(now.get(Calendar.YEAR) - offsetYear);
        for (int year : years) {
            final String value = String.valueOf(year);
            map.put(value, value);
        }
        return map;
    }

    @ModelAttribute("months")
    public Map<String, String> populateMonths() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        final String[] months = dateUtils.getShortFormMonthsOfYear();
        for (int i = 0; i < months.length; i++) {
            map.put(String.valueOf(i + 1), months[i]);
        }
        return map;
    }

    private ModelAndView returnDefaultInfo(final BigDecimal playerId,
                                           final ModelMap modelMap,
                                           final boolean isPartial, HttpServletResponse response) {

        final PlayerProfile playerProfile = playerProfileService.findByPlayerId(playerId);
        if (playerProfile == null) {
            return noSessionExists(response);
        }

        modelMap.addAttribute(PLAYER_OBJECT_KEY, playerProfile);

        if (!modelMap.containsAttribute(AVATAR_OBJECT_KEY)) {
            modelMap.addAttribute(AVATAR_OBJECT_KEY, playerService.getPictureUrl(playerId));
        }

        if (!modelMap.containsAttribute(PLAYER_INFO_OBJECT_KEY)) {
            modelMap.addAttribute(PLAYER_INFO_OBJECT_KEY, new PlayerProfileSummaryBuilder(playerProfile).build());
        }

        if (!modelMap.containsAttribute(DISPLAY_NAME_OBJECT_KEY)) {
            modelMap.addAttribute(DISPLAY_NAME_OBJECT_KEY, new DisplayName(playerProfile));
        }

        if (!modelMap.containsAttribute(EMAIL_ADDRESS_OBJECT_KEY)) {
            modelMap.addAttribute(EMAIL_ADDRESS_OBJECT_KEY, new EmailAddress(playerProfile));
        }

        if (!modelMap.containsAttribute(PASSWORD_CHANGE_OBJECT_KEY)) {
            modelMap.addAttribute(PASSWORD_CHANGE_OBJECT_KEY, new PasswordChangeRequest(playerProfile));
        }


        final ModelAndView modelAndView = getInitialisedModelAndView(isPartial);

        setupDefaultModel(modelMap);
        modelAndView.getModelMap().addAllAttributes(modelMap);

        return modelAndView;
    }

    @Override
    public ModelAndView resolveException(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Object handler,
                                         final Exception exception) {
        final Boolean isPartial = Boolean.parseBoolean(String.valueOf(request.getParameter((PARTIAL_URI_FLAG))));

        if (exception instanceof MaxUploadSizeExceededException) {
            final LobbySession lobbySession = getLobbySession(request);
            if (lobbySession == null) {
                return noSessionExists(response);
            }


            final ModelMap modelMap = new ModelMap();

            modelMap.addAttribute("avatarHasErrors", "true");
            modelMap.addAttribute("avatarSizeExceeded", "true");
            modelMap.addAttribute("genders", populateGenders());
            modelMap.addAttribute("countries", populateCountries());

            final ModelAndView modelAndView = returnDefaultInfo(lobbySession.getPlayerId(), modelMap, isPartial, response);

            lobbyExceptionHandler.setCommonPropertiesInModelAndView(request, response, modelAndView);
            return modelAndView;
        } else {
            return null;
        }
    }

    @Override
    String getCurrentTab() {
        return TAB_CODE_NAME;
    }
}
