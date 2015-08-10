package com.yazino.platform.player.updater;


import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.PasswordType;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileUpdateResponse;
import com.yazino.platform.player.YazinoLogin;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.util.PlayerProfileMerger;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import com.yazino.platform.reference.ReferenceService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yazino.game.api.ParameterisedMessage;

import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service("yazinoPlayerProfileUpdater")
public class YazinoPlayerProfileUpdater implements PlayerProfileUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(YazinoPlayerProfileUpdater.class);

    private static final String EMAIL_ADDRESS_NOT_UNIQUE = "E-mail already registered";

    private final PlayerService playerService;
    private final YazinoLoginDao yazinoLoginDao;
    private final PlayerProfileDao playerProfileDao;
    private final CommunityService communityService;
    private final ReferenceService referenceService;
    private final YazinoPlayerValidator yazinoPlayerValidator;
    private final HasherFactory hashers;

    @Autowired
    public YazinoPlayerProfileUpdater(final PlayerService playerService,
                                      final YazinoLoginDao yazinoLoginDao,
                                      final PlayerProfileDao playerProfileDao,
                                      final CommunityService communityService,
                                      final ReferenceService referenceService,
                                      final YazinoPlayerValidator yazinoPlayerValidator,
                                      final HasherFactory hashers) {
        notNull(hashers, "hashers may not be null");

        this.playerService = playerService;
        this.yazinoLoginDao = yazinoLoginDao;
        this.playerProfileDao = playerProfileDao;
        this.communityService = communityService;
        this.referenceService = referenceService;
        this.yazinoPlayerValidator = yazinoPlayerValidator;
        this.hashers = hashers;
    }

    @Override
    public boolean accepts(final String provider) {
        return provider != null && "YAZINO".equals(provider);
    }

    @Override
    public PlayerProfileUpdateResponse update(final PlayerProfile updatedProfile,
                                              final String password,
                                              final String avatarUrl) {
        notNull(updatedProfile, "updatedProfile may not be null");

        final String emailAddress = updatedProfile.getEmailAddress();

        final BasicProfileInformation profile = playerService.getBasicProfileInformation(updatedProfile.getPlayerId());
        if (profile == null) {
            return new PlayerProfileUpdateResponse(updatedProfile, new ParameterisedMessage(
                    "Could not find a Player with ID " + updatedProfile.getPlayerId()));
        }

        LOG.debug("Processing update for playerId: {}, email: {}", profile.getPlayerId(), emailAddress);

        final PlayerProfile existingProfile = playerProfileDao.findByPlayerId(profile.getPlayerId());

        final Set<ParameterisedMessage> errors = validateUpdateRequest(
                updatedProfile, existingProfile, emailAddress, password, avatarUrl);

        final PlayerProfileMerger profileMerger = new PlayerProfileMerger(existingProfile);
        final PlayerProfile mergedProfile = profileMerger.merge(updatedProfile);

        if (errors.size() > 0) {
            LOG.debug("Validation failed for player {} : {} with errors {}", profile.getPlayerId(), updatedProfile, errors);
            return new PlayerProfileUpdateResponse(mergedProfile, errors);
        }

        final PlayerProfileUpdateResponse response = new PlayerProfileUpdateResponse(mergedProfile);

        final String existingEmailAddress = existingProfile.getEmailAddress();
        updateYazinoLogin(password, profile, existingEmailAddress);

        if (emailAddress != null && !ObjectUtils.equals(existingEmailAddress, emailAddress)) {
            LOG.debug("{} changed the e-mail from {} to {}",
                    existingProfile.getDisplayName(), existingEmailAddress, emailAddress);
            response.setEmailChanged(true);
            mergedProfile.setEmailAddress(existingEmailAddress);
        }
        playerProfileDao.save(mergedProfile);

        final PaymentPreferences paymentPreferences = new PaymentPreferences(
                referenceService.getPreferredCurrency(mergedProfile.getCountry()));
        communityService.asyncUpdatePlayer(profile.getPlayerId(), mergedProfile.getDisplayName(),
                avatarUrl, paymentPreferences);

        return response;
    }

    private void updateYazinoLogin(final String password,
                                   final BasicProfileInformation profile,
                                   final String existingEmailAddress) {
        final YazinoLogin yazinoLogin = yazinoLoginDao.findByEmailAddress(existingEmailAddress);

        String passwordHash = yazinoLogin.getPasswordHash();
        PasswordType passwordType = yazinoLogin.getPasswordType();
        byte[] salt = yazinoLogin.getSalt();

        if (!StringUtils.isBlank(password)) {
            final Hasher hasher = hashers.getPreferred();
            LOG.debug("Rehashing password from {} to {}", yazinoLogin.getPasswordType(), hasher.getType());
            if (yazinoLogin.getPasswordType() != hasher.getType()) {
                salt = hasher.generateSalt();
            }
            passwordHash = hasher.hash(password, salt);
            passwordType = hasher.getType();
        }

        yazinoLoginDao.save(new YazinoLogin(profile.getPlayerId(), existingEmailAddress,
                passwordHash, passwordType, salt, yazinoLogin.getLoginAttempts()));
    }

    private Set<ParameterisedMessage> validateUpdateRequest(final PlayerProfile updatedProfile,
                                                            final PlayerProfile existingProfile,
                                                            final String emailAddress,
                                                            final String password,
                                                            final String avatarUrl) {
        final Set<ParameterisedMessage> errors = yazinoPlayerValidator.validate(
                emailAddress, password, updatedProfile, avatarUrl, false);

        if (emailAddress != null
                && !ObjectUtils.equals(existingProfile.getEmailAddress(), emailAddress.toLowerCase())) {
            final boolean exists = yazinoLoginDao.existsWithEmailAddress(emailAddress);
            if (exists) {
                errors.add(new ParameterisedMessage(EMAIL_ADDRESS_NOT_UNIQUE));
            }
        }

        return errors;
    }

}
