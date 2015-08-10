package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.*;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.updater.PlayerProfileUpdater;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class ExternalAuthenticationService {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalAuthenticationService.class);

    private final PlayerProfileUpdater playerProfileUpdater;
    private final PlayerProfileFactory playerProfileFactory;
    private final PlayerProfileDao playerProfileDao;
    private final PlayerService playerService;

    @Autowired
    public ExternalAuthenticationService(
            @Qualifier("externalPlayerProfileUpdater") final PlayerProfileUpdater playerProfileUpdater,
            final PlayerProfileFactory playerProfileFactory,
            final PlayerProfileDao playerProfileDao,
            final PlayerService playerService) {
        notNull(playerProfileUpdater, "playerProfileUpdater may not be null");
        notNull(playerProfileFactory, "playerProfileFactory may not be null");
        notNull(playerProfileDao, "playerProfileDao may not be null");
        notNull(playerService, "playerService may not be null");

        this.playerProfileUpdater = playerProfileUpdater;
        this.playerProfileFactory = playerProfileFactory;
        this.playerProfileDao = playerProfileDao;
        this.playerService = playerService;
    }

    public PlayerProfileLoginResponse login(final String remoteAddress,
                                            final Partner partnerId,
                                            final PlayerInformationHolder provider,
                                            final String referrer,
                                            final Platform platform,
                                            final String gameType) {
        notNull(remoteAddress, "remoteAddress is null");
        notNull(partnerId, "partnerId is null");
        notNull(provider, "provider is null");
        notNull(platform, "platform is null");

        final PlayerProfile playerProfile = provider.getPlayerProfile();
        LOG.debug("Registering session for {}", playerProfile);
        if (playerProfile == null) {
            return new PlayerProfileLoginResponse(LoginResult.FAILURE);
        }

        final PlayerProfile existingProfile = getPlayerProfile(
                playerProfile.getProviderName(), playerProfile.getExternalId());

        final LoginResult result;
        if (existingProfile == null) {
            final BigDecimal playerId = createAndAuthenticateNewPlayer(
                    remoteAddress, playerProfile, referrer, platform, provider.getAvatarUrl(), gameType);
            if (playerId == null) {
                return new PlayerProfileLoginResponse(LoginResult.FAILURE);
            }

            result = LoginResult.NEW_USER;

        } else if (PlayerProfileStatus.ACTIVE != existingProfile.getStatus()) {
            return new PlayerProfileLoginResponse(LoginResult.BLOCKED);

        } else {
            LOG.debug("Updating existing profile");
            playerProfile.setPlayerId(existingProfile.getPlayerId());

            synchroniseProfile(playerProfile, existingProfile, provider.getAvatarUrl());
            result = LoginResult.EXISTING_USER;
        }

        if (playerProfile.getPlayerId() == null) {
            throw new IllegalStateException("Player profile has no Player ID: " + playerProfile);
        }

        registerFriends(provider, playerProfile);

        return new PlayerProfileLoginResponse(playerProfile.getPlayerId(), result);
    }

    private BigDecimal createAndAuthenticateNewPlayer(final String remoteAddress,
                                                      final PlayerProfile playerProfile,
                                                      final String referrer,
                                                      final Platform platform,
                                                      final String avatarUrl,
                                                      final String gameType) {
        LOG.debug("Player profile doesn't exist. Creating a new one");

        final BasicProfileInformation profileInfo = playerProfileFactory.createAndPersistPlayerAndSendPlayerRegistrationEvent(
                playerProfile, remoteAddress, referrer, platform, avatarUrl, gameType);
        if (profileInfo == null || profileInfo.getPlayerId() == null) {
            LOG.warn("Registration failed for {}", playerProfile.getRealName());
            return null;
        }

        return profileInfo.getPlayerId();
    }

    private void synchroniseProfile(final PlayerProfile playerProfile,
                                    final PlayerProfile existingProfile,
                                    final String avatarUrl) {
        if (existingProfile.isSyncProfile()) {
            existingProfile.setEmailAddress(playerProfile.getEmailAddress());
            existingProfile.setDisplayName(playerProfile.getDisplayName());
            existingProfile.setGender(playerProfile.getGender());
            existingProfile.setDateOfBirth(playerProfile.getDateOfBirth());
            existingProfile.setCountry(playerProfile.getCountry());
        }

        try {
            playerProfileUpdater.update(existingProfile, null, avatarUrl);

        } catch (final Exception e) {
            LOG.error("Couldn't update existing profile {}", existingProfile, e);
        }
    }

    private void registerFriends(final PlayerInformationHolder provider,
                                 final PlayerProfile playerProfile) {
        final Set<String> friendExternalIds = provider.getFriends();
        final String providerName = playerProfile.getProviderName();
        final BigDecimal playerId = playerProfile.getPlayerId();
        syncBuddies(playerId, providerName, friendExternalIds);
    }

    public void syncBuddies(BigDecimal playerId, String providerName, Set<String> friendExternalIds) {
        notNull(playerId, "playerId is null");
        notBlank(providerName, "providerName is null");
        try {
            LOG.debug("Friends to register {} for player {}", friendExternalIds, playerId);
            final Set<BigDecimal> friendPlayerIds = playerProfileDao.findPlayerIdsByProviderNameAndExternalIds(
                    friendExternalIds, providerName);
            if (friendPlayerIds != null && !friendPlayerIds.isEmpty()) {
                playerService.asyncRegisterFriends(playerId, friendPlayerIds);
            }
        } catch (final Exception e) {
            LOG.error("Couldn't register friends for player {}; friends {}", playerId, friendExternalIds, e);
        }
    }

    public PlayerProfileAuthenticationResponse authenticate(final String provider,
                                                            final String externalId) {
        final PlayerProfile userProfile = getPlayerProfile(provider, externalId);
        if (userProfile != null) {
            return new PlayerProfileAuthenticationResponse(userProfile.getPlayerId(), !userProfile.getStatus().isLoginAllowed());
        }
        return new PlayerProfileAuthenticationResponse();
    }

    private PlayerProfile getPlayerProfile(final String provider,
                                           final String externalId) {
        if (StringUtils.isBlank(provider) || StringUtils.isBlank(externalId)) {
            LOG.warn("Authentication failed: provider [{}], id [{}]", provider, externalId);
            return null;
        }

        return playerProfileDao.findByProviderNameAndExternalId(provider, externalId);
    }

}
