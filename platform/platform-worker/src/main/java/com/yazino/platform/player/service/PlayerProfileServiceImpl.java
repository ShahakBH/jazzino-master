package com.yazino.platform.player.service;

import com.google.common.base.Optional;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.updater.PlayerProfileUpdater;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.util.PasswordGenerator;
import com.yazino.platform.player.util.TestModeGuard;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import com.yazino.platform.session.SessionService;
import com.yazino.platform.worker.message.PlayerVerifiedMessage;
import com.yazino.platform.worker.message.VerificationType;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.player.GuestStatus.CONVERTED;
import static com.yazino.platform.player.GuestStatus.GUEST;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("playerProfileService")
public class PlayerProfileServiceImpl implements PlayerProfileService, PlayerProfileTestService {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerProfileService.class);
    private static final String DO_NOT_CHANGE_AVATAR_URL = null;
    private static final String GUEST_CONVERSION_TRANSACTION_TYPE = "Convert Account";
    private static final String GUEST_CONVERSION_TRANSACTION_REFERENCE = "account conversion";
    private static final String EMAIL_FALLBACK = "fb-no-email@yazino.com";

    private final List<PlayerProfileUpdater> playerProfileUpdaters = new ArrayList<PlayerProfileUpdater>();

    private final PlayerProfileDao playerProfileDao;
    private final YazinoLoginDao yazinoLoginDao;
    private final CommunityService communityService;
    private final PlayerService playerService;
    private final SessionService sessionService;
    private final HasherFactory hashers;
    private final PasswordGenerator passwordGenerator;
    private final QueuePublishingService<PlayerProfileEvent> playerProfileEventService;
    private final QueuePublishingService<PlayerVerifiedMessage> playerVerifiedService;
    private final YazinoPlayerValidator yazinoPlayerValidator;
    private final WalletService walletService;
    private final PlayerCreditConfiguration playerCreditConfig;

    @Autowired
    private TestModeGuard testModeGuard;

    @Autowired
    public PlayerProfileServiceImpl(final PlayerProfileDao playerProfileDao,
                                    final CommunityService communityService,
                                    final PlayerService playerService,
                                    final SessionService sessionService,
                                    final YazinoLoginDao yazinoLoginDao,
                                    final HasherFactory hashers,
                                    @Qualifier("playerProfileEventQueuePublishingService")
                                    final QueuePublishingService<PlayerProfileEvent> playerProfileEventService,
                                    @Qualifier("playerVerifiedPublishingService")
                                    final QueuePublishingService<PlayerVerifiedMessage> playerVerifiedService,
                                    final PasswordGenerator passwordGenerator,
                                    final YazinoPlayerValidator yazinoPlayerValidator,
                                    final WalletService walletService,
                                    final PlayerCreditConfiguration playerCreditConfig) {
        notNull(playerProfileDao, "playerProfileDao may not be null");
        notNull(hashers, "hashers may not be null");
        notNull(passwordGenerator, "passwordGenerator may not be null");
        notNull(playerVerifiedService, "playerVerifiedService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(yazinoLoginDao, "yazinoLoginDao may not be null");

        this.playerProfileDao = playerProfileDao;
        this.communityService = communityService;
        this.playerService = playerService;
        this.sessionService = sessionService;
        this.yazinoLoginDao = yazinoLoginDao;
        this.hashers = hashers;
        this.playerProfileEventService = playerProfileEventService;
        this.playerVerifiedService = playerVerifiedService;
        this.passwordGenerator = passwordGenerator;
        this.yazinoPlayerValidator = yazinoPlayerValidator;
        this.walletService = walletService;
        this.playerCreditConfig = playerCreditConfig;
    }

    @Resource(name = "playerProfileUpdaters")
    public void setPlayerProfileUpdaters(final List<PlayerProfileUpdater> playerProfileUpdaters) {
        if (playerProfileUpdaters != null) {
            this.playerProfileUpdaters.addAll(playerProfileUpdaters);
        }
    }

    @Override
    public PlayerProfile findByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        return playerProfileDao.findByPlayerId(playerId);
    }

    @Override
    public PlayerProfile findByProviderNameAndExternalId(final String providerName,
                                                         final String externalId) {
        notNull(providerName, "providerName may not be null");
        notNull(externalId, "externalId may not be null");

        return playerProfileDao.findByProviderNameAndExternalId(providerName, externalId);
    }

    @Override
    // TODO Should be transactional
    public PlayerProfileServiceResponse convertGuestToYazinoAccount(BigDecimal playerId,
                                                                    String emailAddress,
                                                                    String password,
                                                                    String displayName) {
        notNull(playerId, "playerId may not be null");
        notBlank(emailAddress, "emailAddress cannot be null or empty");
        notBlank(password, "emailAddress cannot be null or empty");
        notBlank(displayName, "displayName cannot be null or empty");

        try {
            Set<ParameterisedMessage> errors = yazinoPlayerValidator.validateForYazinoConversion(displayName, emailAddress, password);
            if (!errors.isEmpty()) {
                return new PlayerProfileServiceResponse(errors, false);
            }
            PlayerProfile guestProfile = findGuestPlayerProfileByPlayerId(playerId);
            if (guestProfile.getGuestStatus() != GUEST) {
                errors.add(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)", guestProfile.getGuestStatus().getId()));
                return new PlayerProfileServiceResponse(errors, false);
            }
            final PlayerProfile updatedPlayerProfile = PlayerProfile.copy(guestProfile)
                    .withEmailAddress(emailAddress)
                    .withDisplayName(displayName)
                    .withGuestStatus(CONVERTED)
                    .asProfile();

            updateActive(updatedPlayerProfile, DO_NOT_CHANGE_AVATAR_URL);
            updateYazinoLogin(playerId, password, emailAddress);
            creditYazinoConversionAmount(playerId, emailAddress, displayName, password);
            return new PlayerProfileServiceResponse(true);
        } catch (Exception e) {
            LOG.error("error converting guest play account to yazino account for "
                            + "playerId: {}, emailAddress: {}, displayName: {}, password: {}",
                    playerId, emailAddress, displayName, password, e
            );
            throw e;
        }
    }

    private void creditYazinoConversionAmount(BigDecimal playerId, String emailAddress, String displayName, String password) {
        BigDecimal accountId = playerService.getAccountId(playerId);
        try {
            walletService.postTransaction(accountId, playerCreditConfig.getGuestConversionAmount(),
                    GUEST_CONVERSION_TRANSACTION_TYPE, GUEST_CONVERSION_TRANSACTION_REFERENCE, TransactionContext.EMPTY);
            communityService.publishBalance(playerId);
        } catch (WalletServiceException e) {
            LOG.error("failed to credit conversion amount when converting guest play account to yazino account for "
                            + "playerId: {}, emailAddress: {}, displayName: {}, password: {}",
                    playerId, emailAddress, displayName, password, e
            );
        }
    }

    // TODO Should be transactional
    @Override
    public PlayerProfileServiceResponse convertGuestToFacebookAccount(BigDecimal playerId,
                                                                      String facebookId,
                                                                      String displayName,
                                                                      String emailAddress) {
        notNull(facebookId, "facebookId may not be null");
        return convertGuestToExternalAccount(playerId, facebookId, displayName, emailAddress, "FACEBOOK");
    }

    // TODO Should be transactional
    @Override
    public PlayerProfileServiceResponse convertGuestToExternalAccount(BigDecimal playerId,
                                                                      String externalId,
                                                                      String displayName,
                                                                      String emailAddress,
                                                                      String provider) {
        notNull(playerId, "playerId may not be null");
        notNull(externalId, "externalId may not be null");
        notNull(displayName, "displayName may not be null");
        notNull(provider, "provider may not be null");

        if (StringUtils.isBlank(emailAddress)) {
            emailAddress = EMAIL_FALLBACK;
        }

        try {
            Set<ParameterisedMessage> errors = new HashSet<>();
            if (hasAccountForExternalId(provider, externalId)) {
                errors.add(new ParameterisedMessage(StringUtils.capitalize(provider.toLowerCase()) + " user already has an account"));
                return new PlayerProfileServiceResponse(errors, false);
            }
            PlayerProfile guestProfile = findGuestPlayerProfileByPlayerId(playerId);
            if (guestProfile.getGuestStatus() != GUEST) {
                errors.add(new ParameterisedMessage("cannot convert account with non-guest account (status=%s)",
                        guestProfile.getGuestStatus().getId()));
                return new PlayerProfileServiceResponse(errors, false);
            }

            PlayerProfile updatedPlayerProfile = PlayerProfile.copy(guestProfile)
                    .withEmailAddress(emailAddress)
                    .withExternalId(externalId)
                    .withProviderName(provider)
                    .withRpxProvider(provider)
                    .withDisplayName(displayName)
                    .withGuestStatus(CONVERTED)
                    .asProfile();

            updateActive(updatedPlayerProfile, DO_NOT_CHANGE_AVATAR_URL);
            yazinoLoginDao.deleteByPlayerId(playerId);
            creditConversionBonus(playerId, externalId, emailAddress, displayName);
            return new PlayerProfileServiceResponse(true);
        } catch (Exception e) {
            LOG.error("error converting guest play account to {} account for playerId: {}, externalId: {}",
                    provider, playerId, externalId, e);
            throw e;
        }
    }

    private void creditConversionBonus(BigDecimal playerId, String externalId, String emailAddress, String displayName) {
        BigDecimal accountId = playerService.getAccountId(playerId);
        try {
            walletService.postTransaction(accountId, playerCreditConfig.getGuestConversionAmount(),
                    GUEST_CONVERSION_TRANSACTION_TYPE, GUEST_CONVERSION_TRANSACTION_REFERENCE, TransactionContext.EMPTY);
            communityService.publishBalance(playerId);
        } catch (WalletServiceException e) {
            LOG.error("failed to credit conversion amount when converting guest play account to external account for "
                            + "playerId: {}, externalId: {}, emailAddress: {}, displayName: {}",
                    playerId, externalId, emailAddress, displayName, e
            );
        }
    }

    private PlayerProfile findGuestPlayerProfileByPlayerId(BigDecimal playerId) {
        PlayerProfile guestProfile = findByPlayerId(playerId);
        if (guestProfile == null) {
            throw new RuntimeException("Player not found (playerId=" + playerId + ")");
        }
        return guestProfile;
    }

    private boolean hasAccountForExternalId(String provider, String externalId) {
        return findByProviderNameAndExternalId(provider, externalId) != null;
    }

    @Override
    public boolean updatePlayerInfo(final BigDecimal playerId, final PlayerProfileSummary userProfileInfo) {
        try {
            final PlayerProfile updatedUserProfile = PlayerProfile.copy(findByPlayerId(playerId))
                    .withDateOfBirth(userProfileInfo.getDateOfBirth())
                    .withCountry(userProfileInfo.getCountry())
                    .withGender(Gender.getById(userProfileInfo.getGender()))
                    .asProfile();
            updateActive(updatedUserProfile, null);
            return true;

        } catch (Exception ex) {
            LOG.error("Error caught updating user profile data", ex);
            return false;
        }
    }

    @Override
    public boolean updateDisplayName(final BigDecimal playerId, final String displayName) {
        try {
            final PlayerProfile updatedUserProfile = PlayerProfile.copy(findByPlayerId(playerId))
                    .withDisplayName(displayName)
                    .asProfile();
            updateActive(updatedUserProfile, null);
            return true;

        } catch (Exception ex) {
            LOG.error("Error caught updating user profile data", ex);
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean updateEmailAddress(final BigDecimal playerId, final String emailAddress) {
        try {
            final PlayerProfile updatedUserProfile = PlayerProfile.copy(findByPlayerId(playerId))
                    .withEmailAddress(emailAddress)
                    .asProfile();
            updateActive(updatedUserProfile, null);
            return true;

        } catch (Exception ex) {
            LOG.error("Error caught updating user profile data", ex);
            return Boolean.FALSE;
        }
    }

    @Override
    public boolean updateAvatar(final BigDecimal playerId,
                                final Avatar avatar) {
        try {
            updateActive(findByPlayerId(playerId), avatar.getUrl());
            return true;

        } catch (Exception ex) {
            LOG.error("Error caught updating user profile data", ex);
            return Boolean.FALSE;
        }
    }

    @Override
    public void updatePassword(final BigDecimal playerId, final PasswordChangeRequest passwordChangeForm) {
        final YazinoLogin yazinoLogin = yazinoLoginDao.findByPlayerId(playerId);
        byte[] salt = yazinoLogin.getSalt();
        final Hasher preferredHasher = hashers.getPreferred();
        if (yazinoLogin.getPasswordType() != preferredHasher.getType()) {
            salt = preferredHasher.generateSalt();
        }
        final String passwordHash = preferredHasher.hash(
                passwordChangeForm.getNewPassword(), salt);
        final YazinoLogin finalYazinoLogin = YazinoLogin.copy(yazinoLogin)
                .withPasswordHash(passwordHash)
                .withSalt(salt)
                .withPasswordType(preferredHasher.getType())
                .asLogin();
        yazinoLoginDao.save(finalYazinoLogin);
    }

    private void updateYazinoLogin(BigDecimal playerId, String password, String emailAddress) {
        final YazinoLogin yazinoLogin = yazinoLoginDao.findByPlayerId(playerId);
        byte[] salt = yazinoLogin.getSalt();
        final Hasher preferredHasher = hashers.getPreferred();
        if (yazinoLogin.getPasswordType() != preferredHasher.getType()) {
            salt = preferredHasher.generateSalt();
        }
        final String passwordHash = preferredHasher.hash(password, salt);
        final YazinoLogin finalYazinoLogin = YazinoLogin.copy(yazinoLogin)
                .withPasswordHash(passwordHash)
                .withSalt(salt)
                .withPasswordType(preferredHasher.getType())
                .withEmail(emailAddress)
                .asLogin();
        yazinoLoginDao.save(finalYazinoLogin);
    }

    @Override
    public void updateSyncFor(final BigDecimal playerId, final boolean syncProfile) {
        final PlayerProfile updatedUserProfile = PlayerProfile.copy(findByPlayerId(playerId))
                .withSyncProfile(syncProfile)
                .asProfile();
        playerProfileDao.save(updatedUserProfile);
    }

    @Override
    public void updateStatus(final BigDecimal playerId,
                             final PlayerProfileStatus status, final String changedBy, final String reason) {
        notNull(playerId, "playerId may not be null");
        notNull(status, "status may not be null");
        notNull(changedBy, "changedBy may not be null");
        notNull(reason, "reason may not be null");

        playerProfileDao.updateStatus(playerId, status, changedBy, reason);

        final PlayerProfile playerProfile = playerProfileDao.findByPlayerId(playerId);
        LOG.debug("publishing event for {}", playerProfile);
        playerProfileEventService.send(convert(playerProfile, playerService.getPictureUrl(playerProfile.getPlayerId())));
    }

    @Override
    public void updateRole(final BigDecimal playerId,
                           final PlayerProfileRole role) {
        notNull(playerId, "playerId may not be null");
        notNull(role, "role may not be null");

        playerProfileDao.updateRole(playerId, role);

        final PlayerProfile playerProfile = playerProfileDao.findByPlayerId(playerId);
        LOG.debug("publishing event for {}", playerProfile);
        playerProfileEventService.send(convert(playerProfile, playerService.getPictureUrl(playerProfile.getPlayerId())));
    }

    @Override
    public List<PlayerProfileAudit> findAuditRecordsFor(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        return playerProfileDao.findAuditRecordsFor(playerId);
    }

    @Override
    public ResetPasswordResponse resetPassword(final String email) {
        notNull(email, "email may not be null");

        final YazinoLogin yazinoLogin = yazinoLoginDao.findByEmailAddress(email);
        if (yazinoLogin == null) {
            return new ResetPasswordResponse();
        }

        final String newPassword = passwordGenerator.generatePassword();

        final Hasher hasher = hashers.getPreferred();
        byte[] salt = yazinoLogin.getSalt();
        if (yazinoLogin.getPasswordType() != hasher.getType()) {
            salt = hasher.generateSalt();
        }
        final String hashedPassword = hasher.hash(newPassword, salt);
        yazinoLoginDao.save(withNewPassword(yazinoLogin, hashedPassword, hasher.getType(), salt));

        final PlayerProfile playerProfile = playerProfileDao.findByPlayerId(yazinoLogin.getPlayerId());
        if (playerProfile == null) {
            LOG.error("Integrity error: login exists without profile, player ID: {}", yazinoLogin.getPlayerId());
            return new ResetPasswordResponse();
        }

        return new ResetPasswordResponse(playerProfile.getPlayerId(), playerProfile.getDisplayName(), newPassword);
    }

    private YazinoLogin withNewPassword(final YazinoLogin yazinoLogin,
                                        final String hashedPassword,
                                        final PasswordType passwordType,
                                        final byte[] salt) {
        return new YazinoLogin(yazinoLogin.getPlayerId(), yazinoLogin.getEmail(),
                hashedPassword, passwordType, salt, yazinoLogin.getLoginAttempts());
    }

    @Override
    public int count() {
        return playerProfileDao.count();
    }

    @Override
    public String findLoginEmailByPlayerId(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        final YazinoLogin yazinoLogin = yazinoLoginDao.findByPlayerId(playerId);
        if (yazinoLogin != null) {
            return yazinoLogin.getEmail();
        }
        return null;
    }

    @Override
    public boolean exists(final String emailAddress) {
        return emailAddress != null && yazinoLoginDao.existsWithEmailAddress(emailAddress);
    }

    @Override
    public PlayerProfileUpdateResponse update(final PlayerProfile playerProfile,
                                              final String password,
                                              final String avatarUrl) {
        notNull(playerProfile, "playerProfile may not be null");

        for (PlayerProfileUpdater providerProfileUpdater : playerProfileUpdaters) {
            if (providerProfileUpdater.accepts(playerProfile.getProviderName())) {
                return providerProfileUpdater.update(playerProfile, password, avatarUrl);
            }
        }

        throw new IllegalStateException("Cannot find an updater that accepts provider "
                + playerProfile.getProviderName());
    }

    @Override
    public boolean verify(final String email,
                          final String verificationIdentifier,
                          final VerificationType verificationType) {
        if (email == null || verificationIdentifier == null) {
            return false;
        }

        final PlayerProfile profile = playerProfileDao.findByEmailAddress(email);
        if (profile == null
                || !ObjectUtils.equals(verificationIdentifier, profile.getVerificationIdentifier())) {
            return false;
        }

        profile.setVerificationIdentifier(null);
        playerProfileDao.save(profile);

        playerProfileEventService.send(convert(profile, playerService.getPictureUrl(profile.getPlayerId())));
        playerVerifiedService.send(new PlayerVerifiedMessage(profile.getPlayerId(), verificationType));

        return true;
    }

    @Override
    public Map<BigDecimal, String> findDisplayNamesById(final Set<BigDecimal> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return playerProfileDao.findDisplayNamesByIds(playerIds);
    }

    private void updateActive(final PlayerProfile updatedPlayerProfile,
                              final String newAvatarUrl) {
        String avatarUrl = newAvatarUrl;
        if (avatarUrl == null) {
            avatarUrl = playerService.getPictureUrl(updatedPlayerProfile.getPlayerId());
        }

        final PaymentPreferences paymentPreferences = playerService
                .getPaymentPreferences(updatedPlayerProfile.getPlayerId());

        communityService.asyncUpdatePlayer(updatedPlayerProfile.getPlayerId(),
                updatedPlayerProfile.getDisplayName(), avatarUrl, paymentPreferences);

        playerProfileDao.save(updatedPlayerProfile);

        sessionService.updatePlayerInformation(updatedPlayerProfile.getPlayerId(),
                updatedPlayerProfile.getDisplayName(), avatarUrl);

        LOG.debug("Publishing event for: {}", updatedPlayerProfile);
        playerProfileEventService.send(convert(updatedPlayerProfile, avatarUrl));
    }

    private PlayerProfileEvent convert(final PlayerProfile profile, final String avatarUrl) {
        final String gender;
        if (profile.getGender() == null) {
            gender = null;
        } else {
            gender = profile.getGender().getId();
        }

        return new PlayerProfileEvent(profile.getPlayerId(),
                new DateTime(),
                profile.getDisplayName(),
                profile.getRealName(),
                profile.getFirstName(),
                avatarUrl,
                profile.getEmailAddress(),
                profile.getCountry(),
                profile.getExternalId(),
                profile.getVerificationIdentifier(),
                profile.getProviderName(),
                profile.getStatus(),
                profile.getPartnerId(),
                profile.getDateOfBirth(),
                gender,
                profile.getReferralIdentifier(),
                null,
                false,
                profile.getLastName(),
                guestStatusIdFor(profile));
    }

    private String guestStatusIdFor(PlayerProfile profile) {
        GuestStatus guestStatus = profile.getGuestStatus();
        if (guestStatus == null) {
            guestStatus = GuestStatus.NON_GUEST;
        }
        return guestStatus.getId();
    }

    public Set<String> findRegisteredEmailAddresses(final String... candidateEmailAddresses) {
        return new HashSet<String>(findByEmailAddresses(candidateEmailAddresses).keySet());
    }

    @Override
    public Set<String> findRegisteredExternalIds(final String providerName, final String... candidateExternalIds) {
        return new HashSet<String>(findByProviderNameAndExternalIds(providerName, candidateExternalIds).keySet());
    }

    @Override
    public Map<String, BigDecimal> findByEmailAddresses(String... candidateEmailAddresses) {
        if (ArrayUtils.isEmpty(candidateEmailAddresses)) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> matches = new HashMap<String, BigDecimal>();
        matches.putAll(playerProfileDao.findRegisteredEmailAddresses(candidateEmailAddresses));
        matches.putAll(yazinoLoginDao.findRegisteredEmailAddresses(candidateEmailAddresses));
        return matches;
    }

    @Override
    public Map<String, BigDecimal> findByProviderNameAndExternalIds(String providerName, String... candidateExternalIds) {
        if (ArrayUtils.isEmpty(candidateExternalIds)) {
            return Collections.emptyMap();
        }
        return playerProfileDao.findRegisteredExternalIds(providerName, candidateExternalIds);
    }

    @Override
    public void breakLinkBetweenPlayerAndExternalIdentity(final String providerName, final String externalId) {
        testModeGuard.assertTestModeEnabled();
        playerProfileDao.invalidateExternalId(providerName, externalId);
    }

    @Override
    public void breakLinkBetweenDeviceAndGuestAccount(final String email) {
        testModeGuard.assertTestModeEnabled();
        playerProfileDao.invalidateGuestPlayer(email);
    }

    @Override
    public PagedData<PlayerSearchResult> searchByEmailAddress(final String emailAddress, final int page, final int pageSize) {
        notNull(emailAddress, "emailAddress may not be null");
        return playerProfileDao.searchByEmailAddress(sanitiseQuery(emailAddress), page, pageSize);
    }

    @Override
    public PagedData<PlayerSearchResult> searchByRealOrDisplayName(final String name, final int page, final int pageSize) {
        notNull(name, "name may not be null");
        return playerProfileDao.searchByRealOrDisplayName(sanitiseQuery(name), page, pageSize);
    }

    private String sanitiseQuery(final String query) {
        final String sanitisedQuery = query.trim();
        if (sanitisedQuery.startsWith("%")) {
            throw new IllegalArgumentException("Queries may not start with a wildcard: " + sanitisedQuery);
        }
        return sanitisedQuery;
    }

    @Override
    public Optional<PlayerSummary> findSummaryById(final BigDecimal playerId) {
        notNull(playerId, "playerId may not be null");

        return playerProfileDao.findSummaryById(playerId);
    }
}
