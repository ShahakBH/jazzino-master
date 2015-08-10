package com.yazino.platform.player.service;

import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yazino.game.api.ParameterisedMessage;

import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class YazinoAuthenticationService {
    private static final Logger LOG = LoggerFactory.getLogger(YazinoAuthenticationService.class);

    private static final String EMAIL_ADDRESS_NOT_UNIQUE = "E-mail already registered";

    private final YazinoLoginDao yazinoLoginDao;
    private final HasherFactory hashers;
    private final PlayerProfileDao playerProfileDao;
    private final YazinoPlayerValidator yazinoPlayerValidator;
    private final PlayerProfileFactory playerProfileFactory;

    @Autowired
    public YazinoAuthenticationService(final YazinoLoginDao yazinoLoginDao,
                                       final HasherFactory hashers,
                                       final PlayerProfileDao playerProfileDao,
                                       final YazinoPlayerValidator yazinoPlayerValidator,
                                       final PlayerProfileFactory playerProfileFactory) {
        notNull(yazinoLoginDao, "yazinoLoginDao may not be null");
        notNull(hashers, "hashers may not be null");
        notNull(playerProfileDao, "playerProfileDao may not be null");
        notNull(yazinoPlayerValidator, "yazinoUserValidator may not be null");
        notNull(playerProfileFactory, "playerProfileFactory may not be null");

        this.yazinoLoginDao = yazinoLoginDao;
        this.hashers = hashers;
        this.playerProfileDao = playerProfileDao;
        this.yazinoPlayerValidator = yazinoPlayerValidator;
        this.playerProfileFactory = playerProfileFactory;
    }

    public PlayerProfileAuthenticationResponse authenticate(final String emailAddress,
                                                            final String password) {
        final PlayerProfile playerProfile = authenticatePlayer(emailAddress, password);
        if (playerProfile == null) {
            return new PlayerProfileAuthenticationResponse();
        }

        return new PlayerProfileAuthenticationResponse(playerProfile.getPlayerId(), !playerProfile.getStatus().isLoginAllowed());
    }

    private PlayerProfile authenticatePlayer(final String emailAddress,
                                             final String password) {
        if (StringUtils.isBlank(emailAddress) || StringUtils.isBlank(password)) {
            return null;
        }
        final YazinoLogin yazinoLogin = yazinoLoginDao.findByEmailAddress(emailAddress);
        if (yazinoLogin == null) {
            return null;
        }
        final String hashPassword = hashers.forType(yazinoLogin.getPasswordType())
                .hash(password, yazinoLogin.getSalt());
        if (!StringUtils.equals(hashPassword, yazinoLogin.getPasswordHash())) {
            return null;
        }

        rehashPasswordIfRequired(yazinoLogin, password);

        return playerProfileDao.findByPlayerId(yazinoLogin.getPlayerId());
    }

    private void rehashPasswordIfRequired(final YazinoLogin yazinoLogin,
                                          final String password) {
        final Hasher preferredHasher = hashers.getPreferred();
        if (yazinoLogin.getPasswordType() != preferredHasher.getType()) {
            LOG.debug("Rehashing password from {} to {}", yazinoLogin.getPasswordType(), preferredHasher.getType());
            final byte[] salt = preferredHasher.generateSalt();
            final String newPasswordHash = preferredHasher.hash(password, salt);

            try {
                yazinoLoginDao.save(new YazinoLogin(yazinoLogin.getPlayerId(), yazinoLogin.getEmail(),
                        newPasswordHash, preferredHasher.getType(), salt, yazinoLogin.getLoginAttempts()));

            } catch (Exception e) {
                LOG.error("Failed to rehash password for {}", yazinoLogin.getEmail(), e);
            }
        }
    }


    public PlayerProfileRegistrationResponse register(final String email,
                                                      final String password,
                                                      final PlayerProfile profile,
                                                      final String remoteAddress,
                                                      final String referrer,
                                                      final Platform platform,
                                                      final String avatarUrl,
                                                      final String gameType) {
        notNull(profile, "profile may not be null");
        notNull(platform, "platform may not be null");

        LOG.debug("Processing registration request for {}: {}", email, profile);

        final Set<ParameterisedMessage> errors = validateRegisterRequest(profile, email, password, avatarUrl);

        if (errors.size() > 0) {
            LOG.debug("Validation failed for: {} with errors {} ", profile, errors);
            return new PlayerProfileRegistrationResponse(errors);
        }

        profile.setVerificationIdentifier(generateUniqueId());
        final BasicProfileInformation profileInformation = playerProfileFactory.createAndPersistPlayerAndSendPlayerRegistrationEvent(
                profile, remoteAddress, referrer, platform, avatarUrl, gameType);

        notNull(profileInformation, "profileInformation may not be null");

        final Hasher hasher = hashers.getPreferred();
        final byte[] salt = hasher.generateSalt();
        yazinoLoginDao.save(new YazinoLogin(profile.getPlayerId(), email, hasher.hash(password, salt),
                hasher.getType(), salt, 0));

        return new PlayerProfileRegistrationResponse(profileInformation.getPlayerId());
    }

    String generateUniqueId() {
        return UUID.randomUUID().toString();
    }

    public PlayerProfileLoginResponse login(final String email,
                                            final String password) {
        final PlayerProfile playerProfile = authenticatePlayer(email, password);
        if (playerProfile == null) {
            if (email != null) {
                incrementLoginAttemptsFor(email);
            }
            return new PlayerProfileLoginResponse(LoginResult.FAILURE);
        }

        if (!playerProfile.getStatus().isLoginAllowed()) {
            LOG.warn("YAZINO user[playerId={}] may not log in with status", playerProfile.getPlayerId(), playerProfile.getStatus());
            incrementLoginAttemptsFor(email);
            return new PlayerProfileLoginResponse(LoginResult.BLOCKED);
        }

        resetLoginAttemptsFor(email);
        return new PlayerProfileLoginResponse(
                playerProfile.getPlayerId(), LoginResult.EXISTING_USER);
    }

    private void incrementLoginAttemptsFor(final String email) {
        try {
            yazinoLoginDao.incrementLoginAttempts(email);
        } catch (Exception e) {
            LOG.error("Failed to increment login attempts for {}", email, e);
        }
    }

    private void resetLoginAttemptsFor(final String email) {
        try {
            yazinoLoginDao.resetLoginAttempts(email);
        } catch (Exception e) {
            LOG.error("Failed to reset login attempts for {}", email, e);
        }
    }

    private Set<ParameterisedMessage> validateRegisterRequest(final PlayerProfile profile,
                                                              final String email,
                                                              final String password,
                                                              final String avatarUrl) {
        final Set<ParameterisedMessage> errors = yazinoPlayerValidator.validate(email, password, profile, avatarUrl, true);

        final String emailAddress;
        if (email == null) {
            emailAddress = "";
        } else {
            emailAddress = email.toLowerCase();
        }

        final boolean exists = yazinoLoginDao.existsWithEmailAddress(emailAddress);
        if (exists) {
            errors.add(new ParameterisedMessage(EMAIL_ADDRESS_NOT_UNIQUE));
        }

        return errors;
    }

}
