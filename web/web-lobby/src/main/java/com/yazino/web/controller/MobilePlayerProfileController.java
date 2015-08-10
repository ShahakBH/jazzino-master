package com.yazino.web.controller;

import com.yazino.platform.player.*;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.spring.mvc.DateTimeEditor;
import com.yazino.web.data.CountryRepository;
import com.yazino.web.data.GenderRepository;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.form.validation.PasswordChangeFormValidator;
import com.yazino.web.form.validation.ValidationTools;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.web.form.validation.ValidationTools.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notNull;


/**
 * A Controller for mobile devices requiring player profile information.
 * todo create common profileservice that does all the validation for player
 * profiles and refactor this and web to use same service.
 */
@Controller
@RequestMapping("/mobile/playerProfile")
public class MobilePlayerProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(MobilePlayerProfileController.class);

    static final String PARAM_DISPLAY_NAME = "displayName";
    static final String PARAM_EMAIL_ADDRESS = "emailAddress";
    static final String PARAM_OLD_PASSWORD = "oldPassword";
    static final String PARAM_NEW_PASSWORD = "newPassword";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_AVATAR = "avatar";
    static final String PARAM_GENDER = "gender";
    static final String PARAM_COUNTRY = "country";
    static final String PARAM_DOB = "dateOfBirth";

    private final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("yyy-MM-dd");

    private final FieldProcessor displayNameFieldProcessor = displayNameProcessor();
    private final FieldProcessor emailAddressFieldProcessor = emailAddressProcessor();
    private final FieldProcessor summaryProcessor = summaryProcessor();
    private final FieldProcessor avatarProcessor = avatarProcessor();
    private final FieldProcessor passwordProcessor;

    private final LobbySessionCache lobbySessionCache;
    private final PlayerProfileService playerProfileService;
    private final AvatarRepository avatarRepository;
    private final GenderRepository genderRepository;
    private final CountryRepository countryRepository;
    private final WebApiResponses webApiResponses;

    @Autowired
    public MobilePlayerProfileController(final LobbySessionCache lobbySessionCache,
                                         final PlayerProfileService playerProfileService,
                                         final AvatarRepository avatarRepository,
                                         final AuthenticationService authenticationService,
                                         final GenderRepository genderRepository,
                                         final CountryRepository countryRepository,
                                         final WebApiResponses webApiResponses) {
        notNull(genderRepository, "genderRepository may not be null");
        notNull(countryRepository, "countryRepository may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.lobbySessionCache = lobbySessionCache;
        this.playerProfileService = playerProfileService;
        this.avatarRepository = avatarRepository;
        this.genderRepository = genderRepository;
        this.countryRepository = countryRepository;
        this.webApiResponses = webApiResponses;

        this.passwordProcessor = passwordProcessor(authenticationService, playerProfileService);
    }

    @InitBinder
    public void initBinder(final WebDataBinder binder) {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
    }

    @RequestMapping(method = RequestMethod.GET)
    public void fetchProfile(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException {
        final PlayerProfile profile = findPlayerProfile(request);

        final Map<String, Object> player = new HashMap<>();
        player.put("provider", defaultIfNull(profile.getProviderName(), ""));
        player.put("displayName", defaultIfNull(profile.getDisplayName(), ""));
        player.put("emailAddress", defaultIfNull(profile.getEmailAddress(), ""));
        if (profile.getDateOfBirth() != null) {
            player.put("dateOfBirth", dateFormatter.print(profile.getDateOfBirth()));
        } else {
            player.put("dateOfBirth", "");
        }
        player.put("gender", defaultIfNull(idOf(profile.getGender()), ""));
        player.put("country", defaultIfNull(profile.getCountry(), ""));
        player.put("avatarURL", "");

        final Map<String, Object> result = new HashMap<>();
        result.put(profile.getPlayerId().toPlainString(), player);
        webApiResponses.writeOk(response, result);
    }

    private String idOf(final Gender gender) {
        if (gender != null) {
            return gender.getId();
        }
        return null;
    }

    @RequestMapping(value = "profileOptions", method = RequestMethod.GET)
    public void fetchProfileOptions(final HttpServletResponse response) throws IOException {
        final Map<String, Object> result = new HashMap<>();
        result.put("genders", genderRepository.getGenders());
        result.put("countries", countryRepository.getCountries());
        webApiResponses.writeOk(response, result);
    }

    @RequestMapping(value = PARAM_DISPLAY_NAME, method = RequestMethod.POST)
    public void updateDisplayName(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  @RequestParam(PARAM_DISPLAY_NAME) final String displayName) throws IOException {
        validateAndUpdate(request, response, PARAM_DISPLAY_NAME, displayName, displayNameFieldProcessor);
    }

    @RequestMapping(value = PARAM_EMAIL_ADDRESS, method = RequestMethod.POST)
    public void updateEmailAddress(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   @RequestParam(PARAM_EMAIL_ADDRESS) final String emailAddress) throws IOException {
        validateAndUpdate(request, response, PARAM_EMAIL_ADDRESS, emailAddress, emailAddressFieldProcessor);
    }

    @RequestMapping(value = PARAM_GENDER, method = RequestMethod.POST)
    public void updateGender(final HttpServletRequest request,
                             final HttpServletResponse response,
                             @RequestParam(PARAM_GENDER) final String gender) throws IOException {
        final PlayerProfileSummary summary = findPlayerProfileSummary(request);
        summary.setGender(gender);
        validateAndUpdate(request, response, PARAM_GENDER, summary, summaryProcessor);
    }

    @RequestMapping(value = PARAM_COUNTRY, method = RequestMethod.POST)
    public void updateCountry(final HttpServletRequest request,
                              final HttpServletResponse response,
                              @RequestParam(PARAM_COUNTRY) final String country) throws IOException {
        final PlayerProfileSummary summary = findPlayerProfileSummary(request);
        summary.setCountry(country);
        validateAndUpdate(request, response, PARAM_COUNTRY, summary, summaryProcessor);
    }

    @RequestMapping(value = PARAM_DOB, method = RequestMethod.POST)
    public void updateDateOfBirth(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  @RequestParam(value = PARAM_DOB, required = false) final DateTime dateOfBirth) throws IOException {
        final PlayerProfileSummary summary = findPlayerProfileSummary(request);
        summary.setDateOfBirth(dateOfBirth);
        validateAndUpdate(request, response, PARAM_DOB, summary, summaryProcessor);
    }

    @RequestMapping(value = PARAM_PASSWORD, method = RequestMethod.POST)
    public void updatePassword(final HttpServletRequest request,
                               final HttpServletResponse response,
                               @RequestParam(PARAM_OLD_PASSWORD) final String oldPassword,
                               @RequestParam(PARAM_NEW_PASSWORD) final String newPassword) throws IOException {
        final BigDecimal playerId = findPlayerId(request);
        final PasswordChangeRequest changeRequest = new PasswordChangeRequest(
                oldPassword, newPassword, newPassword, playerId);
        validateAndUpdate(request, response, PARAM_PASSWORD, changeRequest, passwordProcessor);
    }

    @RequestMapping(value = PARAM_AVATAR, method = RequestMethod.POST)
    public void updateAvatar(final HttpServletRequest request,
                             final HttpServletResponse response,
                             @RequestParam(PARAM_AVATAR) final MultipartFile avatar) throws IOException {
        final String originalFilename = avatar.getOriginalFilename();
        Avatar storedAvatar = null;
        if (!StringUtils.isBlank(originalFilename)) {
            try {
                final byte[] bytes = avatar.getBytes();
                storedAvatar = avatarRepository.storeAvatar(originalFilename, bytes);
            } catch (IOException e) {
                LOG.warn("Failed to update avatar, could not get bytes from file [{}]", avatar.getOriginalFilename());
            }
        }
        validateAndUpdate(request, response, PARAM_AVATAR, storedAvatar, avatarProcessor);
    }

    private BigDecimal findPlayerId(final HttpServletRequest request) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new IllegalStateException(String.format("No lobby session path:[%s], query:[%s]",
                    request.getPathInfo(), request.getQueryString()));
        }
        return lobbySession.getPlayerId();
    }

    PlayerProfileSummary findPlayerProfileSummary(final HttpServletRequest request) {
        final PlayerProfile profile = findPlayerProfile(request);
        return new PlayerProfileSummary(idOf(profile.getGender()), profile.getCountry(), profile.getDateOfBirth());
    }

    private PlayerProfile findPlayerProfile(final HttpServletRequest request) {
        final BigDecimal playerId = findPlayerId(request);
        final PlayerProfile profile = playerProfileService.findByPlayerId(playerId);
        LOG.debug("fetched profile for [{}], was [{}]", playerId, profile);
        if (profile == null) {
            throw new IllegalStateException(String.format("No profile found for player [%s]", playerId));
        }
        return profile;
    }

    private void validateAndUpdate(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final String fieldName,
                                   final Object fieldValue,
                                   final FieldProcessor processor)
            throws IOException {
        final BigDecimal playerId = findPlayerId(request);
        final Errors errors = new MapBindingResult(new HashMap(), fieldName);
        processor.validate(errors, fieldValue);
        if (!errors.hasFieldErrors(fieldName)) {
            LOG.debug("updating [{}] to [{}] for [{}]", fieldName, fieldValue, playerId);
            final boolean success = processor.update(playerProfileService, playerId, fieldValue);
            if (!success) {
                errors.rejectValue(fieldName, "updateFailed", "A problem occurred, please try again later");
            }
        }

        final Map<String, Object> fieldDetails = new HashMap<>();
        fieldDetails.put("updated", !errors.hasFieldErrors(fieldName));
        if (errors.hasFieldErrors(fieldName)) {
            final List<String> fieldErrors = new ArrayList<>();
            for (FieldError fieldError : errors.getFieldErrors(fieldName)) {
                fieldErrors.add(fieldError.getDefaultMessage());
            }
            fieldDetails.put("errors", fieldErrors);
        }

        final Map<String, Object> model = new HashMap<>();
        model.put(fieldName, fieldDetails);
        webApiResponses.writeOk(response, model);
    }

    private static FieldProcessor displayNameProcessor() {
        return new FieldProcessor() {
            @Override
            public void validate(final Errors errors, final Object value) {
                ValidationTools.validateDisplayName(errors, PARAM_DISPLAY_NAME, (String) value);
            }

            @Override
            public boolean update(final PlayerProfileService profileService,
                                  final BigDecimal playerId,
                                  final Object value) {
                return profileService.updateDisplayName(playerId, (String) value);
            }
        };
    }

    private static FieldProcessor emailAddressProcessor() {
        return new FieldProcessor() {
            @Override
            public void validate(final Errors errors, final Object value) {
                ValidationTools.validateEmailAddress(errors, PARAM_EMAIL_ADDRESS, (String) value);
            }

            @Override
            public boolean update(final PlayerProfileService profileService,
                                  final BigDecimal playerId,
                                  final Object value) {
                return profileService.updateEmailAddress(playerId, (String) value);
            }
        };
    }

    private static FieldProcessor summaryProcessor() {
        return new FieldProcessor() {
            @Override
            public void validate(final Errors errors, final Object value) {
                final PlayerProfileSummary userProfileInfo = (PlayerProfileSummary) value;
                validateGender(errors, "gender", userProfileInfo.getGender());
                validateCountryLength(errors, "country", userProfileInfo.getCountry());
                validateNotNull(errors, "dateOfBirth", userProfileInfo.getDateOfBirth());
            }

            @Override
            public boolean update(final PlayerProfileService profileService,
                                  final BigDecimal playerId,
                                  final Object value) {
                return profileService.updatePlayerInfo(playerId, (PlayerProfileSummary) value);
            }
        };
    }

    private static FieldProcessor avatarProcessor() {
        return new FieldProcessor() {
            @Override
            public void validate(final Errors errors,
                                 final Object value) {
                validateNotNull(errors, PARAM_AVATAR, value);
            }

            @Override
            public boolean update(final PlayerProfileService profileService,
                                  final BigDecimal playerId,
                                  final Object value) {
                return profileService.updateAvatar(playerId, (Avatar) value);
            }
        };
    }

    private static FieldProcessor passwordProcessor(final AuthenticationService authenticationService,
                                                    final PlayerProfileService playerProfileService) {
        return new FieldProcessor() {
            private final PasswordChangeFormValidator passwordValidator = new PasswordChangeFormValidator(
                    authenticationService, playerProfileService);

            @Override
            public void validate(final Errors errors, final Object value) {
                passwordValidator.validate(value, errors);
            }

            @Override
            public boolean update(final PlayerProfileService profileService,
                                  final BigDecimal playerId,
                                  final Object value) {
                profileService.updatePassword(playerId, (PasswordChangeRequest) value);
                return true;
            }
        };
    }


    interface FieldProcessor {
        void validate(Errors errors, Object value);

        boolean update(PlayerProfileService profileService, BigDecimal playerId, Object value);
    }

}
