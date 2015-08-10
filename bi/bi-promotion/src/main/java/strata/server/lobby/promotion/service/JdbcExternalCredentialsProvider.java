package strata.server.lobby.promotion.service;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import strata.server.lobby.promotion.domain.ExternalCredentials;

import java.math.BigDecimal;

public class JdbcExternalCredentialsProvider implements ExternalCredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcExternalCredentialsProvider.class);
    @Autowired
    private PlayerProfileService playerProfileService;

    // for CGLib
    public JdbcExternalCredentialsProvider() {
        playerProfileService = null;
    }

    public JdbcExternalCredentialsProvider(final PlayerProfileService userProfileDao) {
        this.playerProfileService = userProfileDao;
    }

    @Override
    public ExternalCredentials lookupByPlayerId(final BigDecimal playerId) {

        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Loading external credentials for player[%s]", playerId));
        }
        final PlayerProfile userProfile = playerProfileService.findByPlayerId(playerId);
        if (userProfile != null) {
            String externalId;
            if ("YAZINO".equalsIgnoreCase(userProfile.getProviderName())) {
                externalId = userProfile.getPlayerId().toPlainString();
            } else {
                externalId = userProfile.getExternalId();
            }
            return new ExternalCredentials(userProfile.getProviderName(), externalId);
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Failed to find external credentials for player[%s]", playerId));
        }
        return null;
    }
}
