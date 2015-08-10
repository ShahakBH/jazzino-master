package com.yazino.platform.player.updater;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileUpdateResponse;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.util.PlayerProfileMerger;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yazino.game.api.ParameterisedMessage;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides a default implementation of the {@link com.yazino.platform.player.updater.PlayerProfileUpdater} interface.
 */
@Service("externalPlayerProfileUpdater")
public class ExternalPlayerProfileUpdater implements PlayerProfileUpdater {
    private static final String USER_COULD_NOT_BE_FOUND = "User could not be found";

    private final PlayerProfileDao playerProfileDao;
    private final CommunityService communityService;
    private final ReferenceService referenceService;
    private final PlayerService playerService;

    @Autowired
    public ExternalPlayerProfileUpdater(final PlayerProfileDao playerProfileDao,
                                        final CommunityService communityService,
                                        final ReferenceService referenceService,
                                        final PlayerService playerService) {
        notNull(playerProfileDao, "playerProfileDao must not be null");
        notNull(communityService, "communityService must not be null");
        notNull(referenceService, "referenceService must not be null");
        notNull(playerService, "playerService must not be null");

        this.playerProfileDao = playerProfileDao;
        this.communityService = communityService;
        this.referenceService = referenceService;
        this.playerService = playerService;
    }

    @Override
    public boolean accepts(final String provider) {
        return provider != null && !"YAZINO".equals(provider);
    }

    @Override
    public PlayerProfileUpdateResponse update(final PlayerProfile playerProfile,
                                              final String ignored,
                                              final String avatarUrl) {
        notNull(playerProfile, "playerProfile must not be null");

        final PlayerProfile existingProfile = playerProfileDao.findByPlayerId(playerProfile.getPlayerId());
        if (existingProfile == null) {
            return new PlayerProfileUpdateResponse(playerProfile, new ParameterisedMessage(USER_COULD_NOT_BE_FOUND));
        }

        final BasicProfileInformation profile = playerService.getBasicProfileInformation(
                playerProfile.getPlayerId());
        if (profile == null) {
            throw new IllegalArgumentException("No player exists for player ID " + playerProfile.getPlayerId());
        }
        final BigDecimal playerId = profile.getPlayerId();

        final PlayerProfileMerger merger = new PlayerProfileMerger(existingProfile);
        final PlayerProfile merged = merger.merge(playerProfile);

        final PlayerProfileUpdateResponse response = new PlayerProfileUpdateResponse(merged);

        playerProfileDao.save(merged);

        final Currency preferredCurrency = referenceService.getPreferredCurrency(merged.getCountry());
        communityService.asyncUpdatePlayer(playerId, merged.getDisplayName(), avatarUrl, new PaymentPreferences(preferredCurrency));

        if (merger.hasEmailAddressChanged()) {
            response.setEmailChanged(true);
        }

        return response;
    }
}
